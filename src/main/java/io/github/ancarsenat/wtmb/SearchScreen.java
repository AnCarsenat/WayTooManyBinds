package io.github.ancarsenat.wtmb;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Comparator;

import com.mojang.blaze3d.platform.InputConstants;

import net.minecraft.client.KeyMapping;
//? if >=26 {
import net.minecraft.client.gui.GuiGraphicsExtractor;
//?} else
/*import net.minecraft.client.gui.GuiGraphics;*/
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.renderer.Rect2i;
import net.minecraft.locale.Language;
import net.minecraft.network.chat.Component;

public class SearchScreen extends Screen {
	EditBox searchBox;
	Button idSettingButton;
	Button underflowSettingButton;

	List<BindingEntry> binds;
	List<BindingEntry> matched = new ArrayList<>();
	int selectedIndex = 0;

	public SearchScreen() {
		super(Component.translatable("searchScreen.title"));
		binds = getEntries();
	}

	@Override
	protected void init() {
		{
			int buttonSize = 16;
			int y = 0;

			idSettingButton = Button.builder(Component.literal("ID"), this::onIdSetting)
					.bounds(0, y, buttonSize, buttonSize).build();
			addRenderableWidget(idSettingButton);
			y += buttonSize;

			underflowSettingButton = Button.builder(Component.literal("↑"), this::onUnderflowSetting)
					.bounds(0, y, buttonSize, buttonSize).build();
			addRenderableWidget(underflowSettingButton);
			y += buttonSize;
		}

		// init() runs again on every resize; keep what the user already typed.
		String previousQuery = searchBox != null ? searchBox.getValue() : "";

		searchBox = new EditBox(font, width / 2, 24,
				Component.empty());
		searchBox.setValue(previousQuery);
		searchBox.setResponder(this::onQueryChanged);

		searchBox.setPosition(width / 2 - (searchBox.getWidth() / 2), height / 2 - (searchBox.getHeight() / 2));

		addRenderableWidget(searchBox);
		setInitialFocus(searchBox);

		onQueryChanged(previousQuery);
	}

	/**
	 * Pausing the integrated server makes it save the world, which shows the autosave
	 * indicator every time the search screen is opened. This screen is transient, so
	 * there is nothing to pause for.
	 */
	@Override
	public boolean isPauseScreen() {
		return false;
	}

	int getEntryHeight() {
		return WTMB.Config.showBindIDs ? 9 : 5;
	}

	/** Height of one list row, in pixels. */
	int rowHeight() {
		return getEntryHeight() * 2;
	}

	/** Index of the first entry drawn directly below the search box. */
	int scrollOffset() {
		int halfRows = (height - searchBox.getY() - searchBox.getHeight()) / rowHeight() / 2;
		return Math.max(0, selectedIndex - halfRows);
	}

	/**
	 * Screen y of the entry at {@code index}. Entries before the scroll offset (the
	 * "underflow") stack upwards from the top of the search box instead of overlapping it.
	 */
	int rowY(int index, int offset) {
		int rel = index - offset;
		if (rel >= 0)
			return searchBox.getY() + searchBox.getHeight() + rel * rowHeight();
		return searchBox.getY() + rel * rowHeight();
	}

	/** Whether the entry at {@code index} is drawn (and therefore clickable). */
	boolean isRowVisible(int index, int offset) {
		if (index < offset && !WTMB.Config.drawUndeflowSuggestions)
			return false;
		int y = rowY(index, offset);
		return y + rowHeight() > 0 && y < height;
	}

	Rect2i rowRect(int index, int offset) {
		return new Rect2i(searchBox.getX(), rowY(index, offset), searchBox.getWidth(), rowHeight() - 1);
	}

	/**
	 * Draws a string with a shadow. The concrete call differs per version, so the
	 * version-specific render entrypoint below supplies the implementation and the
	 * rest of the screen stays shared.
	 */
	@FunctionalInterface
	interface TextDrawer {
		void draw(String text, int x, int y, int color);
	}

	//? if >=26 {
	@Override
	public void extractRenderState(GuiGraphicsExtractor context, int mouseX, int mouseY, float delta) {
		super.extractRenderState(context, mouseX, mouseY, delta);
		renderEntries((s, x, y, color) -> context.text(font, s, x, y, color), mouseX, mouseY);
	}
	//?} else {
	/*@Override
	public void render(GuiGraphics context, int mouseX, int mouseY, float delta) {
		super.render(context, mouseX, mouseY, delta);
		renderEntries((s, x, y, color) -> context.drawString(font, s, x, y, color), mouseX, mouseY);
	}
	*///?}

	void renderEntries(TextDrawer text, int mouseX, int mouseY) {
		int shownIndex = matched.isEmpty() ? 0 : selectedIndex + 1;
		text.draw(shownIndex + "/" + matched.size(),
				searchBox.getX() + searchBox.getWidth() + 8, searchBox.getY() + (searchBox.getHeight() / 2) - 4,
				0xffffffff);

		int rowSize = getEntryHeight();
		int offset = scrollOffset();

		for (int i = 0; i < matched.size(); i++) {
			if (!isRowVisible(i, offset))
				continue;

			BindingEntry be = matched.get(i);
			Rect2i r = rowRect(i, offset);

			boolean hovered = r.contains(mouseX, mouseY);
			boolean selected = selectedIndex == i;

			int nameColor = selected ? 0xffffff00 : (hovered ? 0xffdddd88 : 0xffdddddd);
			int keyColor = selected ? 0xff666600 : (hovered ? 0xff666644 : 0xff666666);

			int y = r.getY();
			text.draw(be.name, r.getX(), y, nameColor);

			int catWidth = font.width(be.categoryName);
			text.draw(be.categoryName, r.getX() + r.getWidth() - catWidth, y, nameColor);

			if (WTMB.Config.showBindIDs) {
				text.draw(be.id, r.getX(), y + rowSize, keyColor);

				catWidth = font.width(be.categoryId);
				text.draw(be.categoryId, r.getX() + r.getWidth() - catWidth, y + rowSize, keyColor);
			}
		}
	}

	/** Moves the selection by {@code delta}, wrapping around, and is a no-op on an empty list. */
	void moveSelection(int delta) {
		if (matched.isEmpty()) {
			selectedIndex = 0;
			return;
		}
		selectedIndex = Math.floorMod(selectedIndex + delta, matched.size());
	}

	// Keys are compared against InputConstants instead of GLFW: 26.3 swapped GLFW for
	// SDL, the GLFW class is gone and the key numbers changed. InputConstants has the
	// right values on every supported version.
	@Override
	public boolean keyPressed(KeyEvent input) {
		int keyCode = input.key();
		if (keyCode == InputConstants.KEY_RETURN || keyCode == InputConstants.KEY_NUMPADENTER) {
			onAccept();
			return true;
		}

		if (keyCode == InputConstants.KEY_UP) {
			moveSelection(-1);
			return true;
		}
		if (keyCode == InputConstants.KEY_DOWN || (searchBox.isFocused() && keyCode == InputConstants.KEY_TAB)) {
			moveSelection(1);
			return true;
		}

		return super.keyPressed(input);
	}

	@Override
	public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
		if (verticalAmount == 0)
			return super.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount);

		moveSelection(verticalAmount > 0 ? -1 : 1);
		return true;
	}

	@Override
	public boolean mouseClicked(MouseButtonEvent click, boolean doubled) {
		// Mouse button numbers changed with SDL in 26.3 (left is 1, not 0), so never
		// compare against a bare number or a GLFW constant.
		if (click.button() == InputConstants.MOUSE_BUTTON_LEFT) {
			int offset = scrollOffset();
			int mouseX = (int) click.x();
			int mouseY = (int) click.y();

			for (int i = 0; i < matched.size(); i++) {
				if (isRowVisible(i, offset) && rowRect(i, offset).contains(mouseX, mouseY)) {
					selectedIndex = i;
					onAccept();
					return true;
				}
			}
		}

		boolean handled = super.mouseClicked(click, doubled);
		// Clicking a toggle button focuses it, which stops keystrokes reaching the search
		// box. The focus is set after the button's handler runs, so restore it here.
		if (getFocused() != searchBox)
			setFocused(searchBox);
		return handled;
	}

	void onQueryChanged(String query) {
		BindingEntry oldSelected = null;
		if (selectedIndex >= 0 && selectedIndex < matched.size())
			oldSelected = matched.get(selectedIndex);

		matched = match(query);

		if (oldSelected != null) {
			int newIdx = matched.indexOf(oldSelected);
			if (newIdx != -1) {
				selectedIndex = newIdx;
				return;
			}
		}

		selectedIndex = 0;
	}

	KeyMapping getSelectedBind() {
		if (selectedIndex < 0 || selectedIndex >= matched.size())
			return null;
		return matched.get(selectedIndex).bind;
	}

	void onAccept() {
		KeyMapping bind = getSelectedBind();
		if (bind != null)
			WTMB.queuePress(bind);
		onClose();
	}

	@Override
	public void onClose() {
		//? if >=26 {
		minecraft.gui.setScreen(null);
		//?} else
		/*minecraft.setScreen(null);*/
	}

	void onIdSetting(Button b) {
		WTMB.Config.showBindIDs = !WTMB.Config.showBindIDs;
		WTMB.Config.saveConfig();
	}

	void onUnderflowSetting(Button b) {
		WTMB.Config.drawUndeflowSuggestions = !WTMB.Config.drawUndeflowSuggestions;
		WTMB.Config.saveConfig();
	}

	List<BindingEntry> match(String query) {
		String[] terms = query.toLowerCase(Locale.ROOT).trim().split("\\s+");
		ArrayList<BindingEntry> list = new ArrayList<>();

		for (BindingEntry be : binds) {
			if (be.matches(terms))
				list.add(be);
		}

		return list;
	}

	List<BindingEntry> getEntries() {
		ArrayList<BindingEntry> list = new ArrayList<>();

		for (KeyMapping e : KeyMapping.ALL.values()) {
			String name = Language.getInstance().getOrDefault(e.getName(), e.getName());
			String categoryId = "key.category." + e.getCategory().id().toLanguageKey();
			String categoryName = Language.getInstance().getOrDefault(categoryId, categoryId);

			list.add(new BindingEntry(e, e.getName(), name, categoryId, categoryName));
		}

		list.sort(Comparator.comparing(BindingEntry::getName, String.CASE_INSENSITIVE_ORDER));

		return list;
	}

	static class BindingEntry {
		public final KeyMapping bind;

		public final String id;
		public final String name;

		public final String categoryId;
		public final String categoryName;

		/** Every searchable field, lower-cased once instead of on every keystroke. */
		private final String haystack;

		BindingEntry(KeyMapping bind, String id, String name, String categoryId, String categoryName) {
			this.bind = bind;

			this.id = id;
			this.name = name;

			this.categoryId = categoryId;
			this.categoryName = categoryName;

			this.haystack = String.join("\n", name, id, categoryName, categoryId).toLowerCase(Locale.ROOT);
		}

		public String getName() {
			return name;
		}

		/**
		 * Every term has to appear somewhere in the name, id, category name or category id,
		 * so "inventory movement" finds a bind named "Inventory" in the Movement category.
		 */
		boolean matches(String[] terms) {
			for (String term : terms) {
				if (!haystack.contains(term))
					return false;
			}
			return true;
		}
	}
}

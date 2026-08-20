package io.github.ancarsenat.wtmb;

import java.util.ArrayList;
import java.util.List;
import java.util.Comparator;

import org.lwjgl.glfw.GLFW;

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

	List<BindingEntry> binds = new ArrayList<>();
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

		searchBox = new EditBox(font, width / 2, 24,
				Component.empty());
		searchBox.setResponder(this::onQueryChanged);

		searchBox.setPosition(width / 2 - (searchBox.getWidth() / 2), height / 2 - (searchBox.getHeight() / 2));

		addRenderableOnly(searchBox);
		addWidget(searchBox);
		setInitialFocus(searchBox);

		onQueryChanged("");
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
		text.draw((selectedIndex + 1) + "/" + matched.size(),
				searchBox.getX() + searchBox.getWidth() + 8, searchBox.getY() + (searchBox.getHeight() / 2) - 4,
				0xffffffff);

		int i = 0;
		int rowSize = getEntryHeight();

		int halfRows = (height - searchBox.getY() - searchBox.getHeight()) / (rowSize * 2) / 2;
		int offset = selectedIndex - halfRows;
		if (selectedIndex < halfRows)
			offset = 0;

		for (BindingEntry be : matched) {
			if (!WTMB.Config.drawUndeflowSuggestions && i - offset < 0) {
				i++;
				continue;
			}

			Rect2i r = new Rect2i(searchBox.getX(),
					searchBox.getY() + searchBox.getHeight() + ((i - offset) * rowSize * 2),
					searchBox.getWidth(), (rowSize * 2) - 1);

			Boolean hovered = r.contains(mouseX, mouseY);
			Boolean selected = selectedIndex == i;

			int nameColor = selected ? 0xffffff00 : (hovered ? 0xffdddd88 : 0xffdddddd);
			int keyColor = selected ? 0xff666600 : (hovered ? 0xff666644 : 0xff666666);

			text.draw(be.name,
					searchBox.getX(),
					searchBox.getY() + searchBox.getHeight() + ((i - offset) * rowSize * 2),
					nameColor);

			int catWidth = font.width(be.categoryName);
			text.draw(be.categoryName,
					searchBox.getX() + searchBox.getWidth() - catWidth,
					searchBox.getY() + searchBox.getHeight() + ((i - offset) * rowSize * 2),
					nameColor);

			if (WTMB.Config.showBindIDs) {
				text.draw(be.id,
						searchBox.getX(),
						searchBox.getY() + searchBox.getHeight() + ((i - offset) * rowSize * 2) + rowSize,
						keyColor);

				catWidth = font.width(be.categoryId);
				text.draw(be.categoryId,
						searchBox.getX() + searchBox.getWidth() - catWidth,
						searchBox.getY() + searchBox.getHeight() + ((i - offset) * rowSize * 2) + rowSize,
						keyColor);
			}

			i++;
		}
	}

	@Override
	public boolean keyPressed(KeyEvent input) {
		int keyCode = input.key();
		boolean handled = false;
		if (keyCode == GLFW.GLFW_KEY_ENTER || keyCode == GLFW.GLFW_KEY_KP_ENTER) {
			onAccept();
			return true;
		}

		else if (keyCode == GLFW.GLFW_KEY_UP) {
			selectedIndex--;
			handled = true;
		} else if (keyCode == GLFW.GLFW_KEY_DOWN || (searchBox.isFocused() && keyCode == GLFW.GLFW_KEY_TAB)) {
			selectedIndex++;
			handled = true;
		}

		if (selectedIndex == -1)
			selectedIndex = matched.size() - 1;
		if (selectedIndex == matched.size())
			selectedIndex = 0;

		if (handled)
			return true;

		return super.keyPressed(input);
	}

	@Override
	public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
		if (verticalAmount == 0)
			return super.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount);

		else if (verticalAmount > 0)
			selectedIndex--;
		else
			selectedIndex++;

		if (selectedIndex == -1)
			selectedIndex = matched.size() - 1;
		if (selectedIndex == matched.size())
			selectedIndex = 0;

		return true;
	}

	@Override
	public boolean mouseClicked(MouseButtonEvent click, boolean doubled) {
		int button = click.button();
		double mouseX = click.x();
		double mouseY = click.y();
		if (button == GLFW.GLFW_MOUSE_BUTTON_1) {
			int i = 0;
			int rowSize = getEntryHeight();

			int halfRows = (height - searchBox.getY() - searchBox.getHeight()) / (rowSize * 2) / 2;
			int offset = selectedIndex - halfRows;
			if (selectedIndex < halfRows)
				offset = 0;

			for (BindingEntry be : matched) {
				if (!WTMB.Config.drawUndeflowSuggestions && i - offset < 0) {
					i++;
					continue;
				}

				Rect2i r = new Rect2i(searchBox.getX(),
						searchBox.getY() + searchBox.getHeight() + ((i - offset) * rowSize * 2),
						searchBox.getWidth(), (rowSize * 2) - 1);

				if (r.contains((int) mouseX, (int) mouseY)) {
					selectedIndex = i;
					onAccept();
					return true;
				}
				i++;
			}
		}

		return super.mouseClicked(click, doubled);
	}

	void onQueryChanged(String query) {
		BindingEntry oldSelected = null;
		if (matched.size() != 0)
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
		if (matched.size() == 0)
			return null;
		return matched.get(selectedIndex).bind;
	}

	void onAccept() {
		KeyMapping bind = getSelectedBind();
		if (bind != null)
			WTMB.queuePress(bind);
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
		ArrayList<BindingEntry> list = new ArrayList<>();

		for (BindingEntry be : binds) {
			if (be.matches(query))
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

			BindingEntry be = new BindingEntry(e,
					e.getName(), name,
					categoryId, categoryName);

			list.add(be);
		}

		list.sort(Comparator.comparing(BindingEntry::getName));

		return list;
	}

	class BindingEntry {
		public BindingEntry(KeyMapping bind, String id, String name, String categoryId, String categoryName) {
			this.bind = bind;

			this.id = id;
			this.name = name;

			this.categoryId = categoryId;
			this.categoryName = categoryName;
		}

		public KeyMapping bind;

		public String id;
		public String name;

		public String categoryId;
		public String categoryName;

		public String getName() {
			return name;
		}

		public boolean matches(String query) {
			String[] split = query.toLowerCase().split("\\ ");

			return stringMatches(name, split) ||
					stringMatches(id, split) ||
					stringMatches(categoryName, split) ||
					stringMatches(categoryId, split);
		}

		boolean stringMatches(String text, String[] split) {
			for (String q : split) {
				if (!text.toLowerCase().contains(q)) {
					return false;
				}
			}
			return true;
		}
	}
}

package xyz.ryhon.tmb;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
//? if >=26 {
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
//?} else
/*import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;*/
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.KeyMapping.Category;
import net.minecraft.resources.Identifier;

import com.mojang.blaze3d.platform.InputConstants;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;

import org.lwjgl.glfw.GLFW;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

public class TMB implements ModInitializer {
	public static final Logger LOGGER = LoggerFactory.getLogger("tmb");
	static ArrayList<KeyMapping> toPress = new ArrayList<>();
	static ArrayList<KeyMapping> toRelease = new ArrayList<>();

	@Override
	public void onInitialize() {
		Config.loadConfig();

		Category category = Category.register(Identifier.fromNamespaceAndPath("tmb", "tmb"));
		KeyMapping searchScreenBind;
		searchScreenBind = new KeyMapping(
				"key.tmb.search",
				InputConstants.Type.KEYSYM,
				GLFW.GLFW_KEY_ENTER,
				category);
		//? if >=26 {
		KeyMappingHelper.registerKeyMapping(searchScreenBind);
		//?} else
		/*KeyBindingHelper.registerKeyBinding(searchScreenBind);*/
		ClientTickEvents.START_CLIENT_TICK.register(client -> {
			if (searchScreenBind.consumeClick()) {
				SearchScreen s = new SearchScreen();
				// setScreenAndShow() renders an out-of-band frame, which shows up as a flash.
				//? if >=26 {
				client.gui.setScreen(s);
				//?} else
				/*client.setScreen(s);*/
			}
		});

		KeyMapping reloadConfigBind;
		reloadConfigBind = new KeyMapping(
				"key.tmb.reloadConfig",
				InputConstants.Type.KEYSYM,
				GLFW.GLFW_KEY_UNKNOWN,
				category);
		//? if >=26 {
		KeyMappingHelper.registerKeyMapping(reloadConfigBind);
		//?} else
		/*KeyBindingHelper.registerKeyBinding(reloadConfigBind);*/
		ClientTickEvents.START_CLIENT_TICK.register(client -> {
			if (reloadConfigBind.consumeClick()) {
				Config.loadConfig();
			}
		});

		ClientTickEvents.END_CLIENT_TICK.register(client -> {
			for (KeyMapping bind : toRelease) {
				bind.setDown(false);
			}
			toRelease.clear();

			for (KeyMapping bind : toPress) {
				bind.clickCount++;
				bind.setDown(true);
				toRelease.add(bind);
			}
			toPress.clear();
		});
	}

	public static void queuePress(KeyMapping bind)
	{
		toPress.add(bind);
	}

	public static class Config
	{
		public static boolean showBindIDs = false;
		public static boolean drawUndeflowSuggestions = false;
	
		public static final Path configDir = FabricLoader.getInstance().getConfigDir().resolve("toomanybinds");
		public static final Path configFile = configDir.resolve("config.json");

		public static void loadConfig()
		{
			try
			{
				if(!Files.exists(configFile)) return;

				JsonObject jo = (JsonObject)JsonParser.parseString(Files.readString(configFile));

				if(jo.has("showBindIDs"))
					showBindIDs = jo.get("showBindIDs").getAsBoolean();

				if(jo.has("drawUndeflowSuggestions"))
					drawUndeflowSuggestions = jo.get("drawUndeflowSuggestions").getAsBoolean();
			}catch(Exception e){
				LOGGER.error("Failed to load config", e);
			}
		}

		public static void saveConfig()
		{
			JsonObject jo = new JsonObject();
			jo.addProperty("showBindIDs", showBindIDs);
			jo.addProperty("drawUndeflowSuggestions", drawUndeflowSuggestions);

			try
			{
				Files.createDirectories(configDir);
				Files.writeString(configFile, new Gson().toJson(jo));
			}catch(Exception e)
			{
				LOGGER.error("Failed to save config", e);
			}
		}
	}
}

package me.onethecrazy;

import me.onethecrazy.commands.Commands;
import com.aksulightning.platform.PlatformServices;
import com.aksulightning.platform.fabric.FabricPlatformClient;
import com.aksulightning.platform.fabric.FabricPlatformConfig;
import com.aksulightning.platform.fabric.FabricPlatformEvents;
import com.aksulightning.platform.fabric.FabricPlatformLogger;
import me.onethecrazy.util.FileUtil;
import me.onethecrazy.util.network.BackendInteractor;
import me.onethecrazy.util.objects.save.AllTheSkinsSave;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientEntityEvents;
import net.minecraft.client.network.OtherClientPlayerEntity;
import org.jetbrains.annotations.Nullable;


import java.io.IOException;

public class AllTheSkinsClient implements ClientModInitializer {
	@Nullable private static AllTheSkinsSave options;
	public static String bannerText;
	public static boolean isFirstStartup;

	public static AllTheSkinsSave options(){
		try{
			if(options == null)
				options = FileUtil.loadSave();
		} catch (IOException e) {
            AllTheSkins.LOGGER.error("Error while getting save: {0}", e);
        }

		return options;
    }

	@Override
	public void onInitializeClient() {
		PlatformServices.initialize(
				new FabricPlatformLogger(AllTheSkins.LOGGER),
				new FabricPlatformConfig(),
				new FabricPlatformClient(),
				new FabricPlatformEvents()
		);
		firstStartupSetup();
		// Load Banner text
		BackendInteractor.getBannerTextAsync().thenAccept(text -> bannerText = text);
		// Initialize Commands
		registerCommands();
		// Register player join world callback
		registerPlayerJoinCallback();
		// Clear world-scoped skin state when leaving a world/server.
		registerDisconnectCallback();
		// Queue a self skin load
		queueLoadSelf();
	}

	public void registerCommands(){
		Commands.initializeCommands();

		ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) -> {
			dispatcher.register(Commands.SKINS_COMMAND);
		});
	}

	public void registerPlayerJoinCallback(){
		ClientEntityEvents.ENTITY_LOAD.register((entity, world) -> {
			// Reload that players skin when they (re-)join the world
			if (entity instanceof OtherClientPlayerEntity other) {
				SkinManager.loadSkin(other.getUuidAsString());
			}
		});
	}

	public void registerDisconnectCallback(){
		PlatformServices.events().registerClientDisconnected(SkinManager::clearWorldSkinState);
	}

	public void firstStartupSetup(){
		isFirstStartup = !FileUtil.doesFileExist(FileUtil.getSavePath());

		if(isFirstStartup)
			// Create Paths
			FileUtil.createPaths();
	}

	public void queueLoadSelf(){
		PlatformServices.events().registerClientStarted(SkinManager::loadSelfSkin);
	}
}

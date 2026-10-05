package com.ianblk.ziangui;

import com.mojang.logging.LogUtils;
import net.neoforged.fml.common.Mod;
import org.slf4j.Logger;
import com.ianblk.ziangui.network.GuiNetwork;
import com.ianblk.ziangui.server.MenuManager;
import com.ianblk.ziangui.client.ZianGuiClient;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.common.NeoForge;

@Mod(ZianGui.MOD_ID)
public final class ZianGui {
    public static final String MOD_ID = "ziangui";
    public static final String COMMAND_ROOT = "ZianGui";
    public static final Logger LOGGER = LogUtils.getLogger();

    public ZianGui(IEventBus modBus) {
        if (FMLEnvironment.dist == Dist.CLIENT) ZianGuiClient.init(modBus);
        modBus.addListener(GuiNetwork::register);
        NeoForge.EVENT_BUS.addListener(MenuManager::registerCommands);
        NeoForge.EVENT_BUS.addListener(MenuManager::onLogout);
        NeoForge.EVENT_BUS.addListener(MenuManager::onStop);
        LOGGER.info("[ZianGUI] cargado");
    }
}

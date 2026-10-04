package com.ianblk.ziangui;

import com.mojang.logging.LogUtils;
import net.neoforged.fml.common.Mod;
import org.slf4j.Logger;

@Mod(ZianGui.MOD_ID)
public final class ZianGui {
    public static final String MOD_ID = "ziangui";
    public static final String COMMAND_ROOT = "ZianGui";
    public static final Logger LOGGER = LogUtils.getLogger();

    public ZianGui() {
        LOGGER.info("[ZianGUI] cargado");
    }
}

package com.ianblk.ziangui.server;

import com.ianblk.ziangui.ZianGui;
import net.minecraft.server.level.ServerPlayer;
import java.lang.reflect.Method;

/** Fixed commands only, as the player. Never grants OP or retries an attempted dispatch. */
public final class CommandBridge {
    private CommandBridge() {}
    public static boolean execute(ServerPlayer player, String command) {
        Method dispatch;
        Object sender;
        try {
            sender = PermissionService.bukkitPlayer(player);
            if (sender == null) return vanilla(player, command);
            Class<?> bukkit = Class.forName("org.bukkit.Bukkit");
            Class<?> commandSender = Class.forName("org.bukkit.command.CommandSender");
            dispatch = bukkit.getMethod("dispatchCommand", commandSender, String.class);
        } catch (ReflectiveOperationException | RuntimeException | LinkageError error) {
            ZianGui.LOGGER.warn("[ZianGUI] Command bridge unavailable; no command executed.", error);
            return false;
        }
        try {
            return (Boolean) dispatch.invoke(null, sender, command);
        } catch (ReflectiveOperationException | RuntimeException | LinkageError error) {
            // The invocation may already have produced effects. Do not try another route.
            ZianGui.LOGGER.warn("[ZianGUI] Command dispatch uncertain; automatic retry blocked.", error);
            return false;
        }
    }
    private static boolean vanilla(ServerPlayer player, String command) {
        var server = player.getServer();
        if (server == null) return false;
        try {
            var dispatcher = server.getCommands().getDispatcher();
            return dispatcher.execute(command, player.createCommandSourceStack()) > 0;
        } catch (Exception error) {
            ZianGui.LOGGER.warn("[ZianGUI] Vanilla command failed; no automatic retry.", error);
            return false;
        }
    }
}

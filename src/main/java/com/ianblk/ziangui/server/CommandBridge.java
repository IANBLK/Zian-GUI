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
            // Bukkit may return true after a permission denial. Check its command gate
            // first without dispatching, elevating privileges or trying another route.
            Object server = bukkit.getMethod("getServer").invoke(null);
            Object map = server.getClass().getMethod("getCommandMap").invoke(server);
            Object target = Class.forName("org.bukkit.command.CommandMap")
                .getMethod("getCommand", String.class).invoke(map, command.split(" ", 2)[0]);
            if (target == null) {
                // Youer does not expose every mod command in Bukkit's command map.
                // Select the native route BEFORE any dispatch; preserve exact root case.
                return CommandRoute.execute(false, () -> false, () -> false, () -> vanilla(player, command));
            }
            Method permission = Class.forName("org.bukkit.command.Command")
                .getMethod("testPermissionSilent", commandSender);
            return CommandRoute.execute(true,
                () -> permitted(permission, target, sender),
                () -> dispatch(dispatch, sender, command),
                () -> vanilla(player, command));
        } catch (ReflectiveOperationException | RuntimeException | LinkageError error) {
            ZianGui.LOGGER.warn("[ZianGUI] Command bridge unavailable; no command executed.", error);
            return false;
        }
    }
    private static boolean permitted(Method permission, Object target, Object sender) {
        try { return (Boolean) permission.invoke(target, sender); }
        catch (ReflectiveOperationException | RuntimeException | LinkageError error) {
            ZianGui.LOGGER.warn("[ZianGUI] Command permission lookup failed; no command executed.", error);
            return false;
        }
    }
    private static boolean dispatch(Method dispatch, Object sender, String command) {
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
            var root = dispatcher.getRoot().getChild(command.split(" ", 2)[0]);
            if (root == null || !root.canUse(player.createCommandSourceStack())) return false;
            return dispatcher.execute(command, player.createCommandSourceStack()) > 0;
        } catch (Exception error) {
            ZianGui.LOGGER.warn("[ZianGUI] Vanilla command failed; no automatic retry.", error);
            return false;
        }
    }
}

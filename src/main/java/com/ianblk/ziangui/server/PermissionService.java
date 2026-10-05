package com.ianblk.ziangui.server;

import com.ianblk.ziangui.ZianGui;
import net.minecraft.server.level.ServerPlayer;
import java.lang.reflect.Method;
import java.util.function.BooleanSupplier;
import java.util.function.Supplier;

public final class PermissionService {
    private PermissionService() {}
    static boolean evaluate(Supplier<Boolean> provider, BooleanSupplier fallback) {
        try {
            Boolean result = provider.get();
            return result != null ? result : fallback.getAsBoolean();
        } catch (RuntimeException | LinkageError error) {
            return false; // An installed but broken provider never grants access.
        }
    }
    public static boolean allows(ServerPlayer player, String node) {
        return evaluate(() -> {
            try {
                Object sender = bukkitPlayer(player);
                if (sender == null) return null;
                Class<?> permissions = Class.forName("org.bukkit.permissions.Permissible");
                return (Boolean) permissions.getMethod("hasPermission", String.class).invoke(sender, node);
            } catch (ReflectiveOperationException error) {
                ZianGui.LOGGER.warn("[ZianGUI] Permission lookup failed; access denied.", error);
                throw new IllegalStateException(error);
            }
        }, () -> player.hasPermissions(2));
    }
    public static Object bukkitPlayer(ServerPlayer player) throws ReflectiveOperationException {
        Class<?> bukkit;
        try { bukkit = Class.forName("org.bukkit.Bukkit"); }
        catch (ClassNotFoundException absent) { return null; }
        try {
            Method method = player.getClass().getMethod("getBukkitEntity");
            Object sender = method.invoke(player);
            if (sender == null) throw new IllegalStateException("Bukkit player unavailable");
            return sender;
        } catch (NoSuchMethodException missingBridge) {
            Object sender = bukkit.getMethod("getPlayer", UUID_CLASS).invoke(null, player.getUUID());
            if (sender == null) throw new IllegalStateException("Bukkit player unavailable");
            return sender;
        }
    }
    private static final Class<?> UUID_CLASS = java.util.UUID.class;
}

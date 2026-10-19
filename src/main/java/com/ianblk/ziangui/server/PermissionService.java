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
                var decision=LuckPermsLookup.lookup(player,node);
                if(decision==LuckPermsLookup.Decision.TRUE)return true;
                if(decision==LuckPermsLookup.Decision.FALSE)return false;
                if(node.startsWith("zian.gui.")||publicDefault(node))return null;
                Object sender = bukkitPlayer(player);
                if (sender == null) return null;
                Class<?> permissions = Class.forName("org.bukkit.permissions.Permissible");
                return (Boolean) permissions.getMethod("hasPermission", String.class).invoke(sender, node);
            } catch (ReflectiveOperationException error) {
                ZianGui.LOGGER.warn("[ZianGUI] Permission lookup failed; access denied.", error);
                throw new IllegalStateException(error);
            }
        }, () -> defaultAllowed(node,player.hasPermissions(2)));
    }
    static boolean defaultAllowed(String node,boolean operator){
        return operator||publicDefault(node)||java.util.Set.of("zian.gui.open","zian.gui.menu.principal","zian.gui.menu.viajes",
            "zian.gui.menu.pokemon","zian.gui.menu.actividades","zian.gui.menu.utilidades").contains(node);
    }
    private static boolean publicDefault(String node){return java.util.Set.of("zian.gui.button.gacha","zian.gui.button.quests","ziangts.use","ziangts.open","zianrct.medals","zianutilities.breeding.use","minecraft.command.ZianUtilities","minecraft.command.ZianGTS","minecraft.command.medals","minecraft.command.companions").contains(node);}
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

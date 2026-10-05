package com.ianblk.ziangui.server;

import com.ianblk.ziangui.ZianGui;
import com.ianblk.ziangui.network.GuiPayloads;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.registration.NetworkRegistry;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.neoforged.fml.loading.FMLPaths;
import net.neoforged.neoforge.event.server.ServerStartingEvent;

public final class MenuManager {
    private static MenuConfig CONFIG = new MenuConfig();
    private static final SessionGate GATE = new SessionGate();
    private MenuManager() {}
    static MenuConfig configuration() { return CONFIG; }
    static java.nio.file.Path directory() { return FMLPaths.CONFIGDIR.get().resolve("zian_gui/menus"); }
    static void invalidate(net.minecraft.server.MinecraftServer server) {
        GATE.invalidate().forEach((id, nonce) -> {
            var player = server.getPlayerList().getPlayer(id);
            if (player != null) PacketDistributor.sendToPlayer(player, new GuiPayloads.Close(nonce));
        });
    }
    private static long now() { return System.nanoTime() / 1_000_000; }
    public static void registerCommands(RegisterCommandsEvent event) {
        event.getDispatcher().register(Commands.literal(ZianGui.COMMAND_ROOT)
            .requires(source -> true) // Explicit public root prevents Youer implicit OP gating.
            .executes(ctx -> openCommand(ctx.getSource()))
            .then(Commands.literal("open").executes(ctx -> openCommand(ctx.getSource()))
                .then(Commands.argument("menu", StringArgumentType.word())
                    .suggests((ctx, builder) -> {
                        CONFIG.menus().keySet().forEach(builder::suggest); return builder.buildFuture();
                    })
                    .executes(ctx -> ctx.getSource().getEntity() instanceof ServerPlayer player
                        && open(player, StringArgumentType.getString(ctx, "menu")) ? 1 : 0)))
            .then(Commands.literal("edit").requires(source -> source.getEntity() instanceof ServerPlayer player
                && PermissionService.allows(player, "zian.gui.edit"))
                .executes(ctx -> MenuEditor.open(ctx.getSource().getPlayerOrException(), "principal") ? 1 : 0)
                .then(Commands.argument("menu", StringArgumentType.word())
                    .suggests((ctx, builder) -> { CONFIG.menus().keySet().forEach(builder::suggest); return builder.buildFuture(); })
                    .executes(ctx -> MenuEditor.open(ctx.getSource().getPlayerOrException(), StringArgumentType.getString(ctx, "menu")) ? 1 : 0)))
            .then(Commands.literal("reload").requires(source ->
                source.getEntity() instanceof ServerPlayer player
                    ? PermissionService.allows(player, "zian.gui.reload") : source.hasPermission(2))
                .executes(ctx -> reload(ctx.getSource()))));
    }
    public static void onStart(ServerStartingEvent event) {
        CONFIG = new MenuConfig(); GATE.clear(); MenuEditor.clear();
        try { CONFIG.reload(FMLPaths.CONFIGDIR.get().resolve("zian_gui/menus")); }
        catch (java.io.IOException error) { ZianGui.LOGGER.error("[ZianGUI] Menús desactivados: {}", error.getMessage()); }
    }
    private static int reload(net.minecraft.commands.CommandSourceStack source) {
        try {
            CONFIG.reload(FMLPaths.CONFIGDIR.get().resolve("zian_gui/menus"));
            invalidate(source.getServer()); MenuEditor.invalidate(source.getServer());
            source.sendSuccess(() -> Component.literal("Zian GUI: " + CONFIG.menus().size()
                + " menú(s) recargados. Las pantallas anteriores se cerraron."), true);
            return 1;
        } catch (java.io.IOException error) {
            source.sendFailure(Component.literal("No se aplicó la recarga: " + error.getMessage()));
            ZianGui.LOGGER.warn("[ZianGUI] Recarga rechazada: {}", error.getMessage());
            return 0;
        }
    }
    private static int openCommand(net.minecraft.commands.CommandSourceStack source) {
        if (!(source.getEntity() instanceof ServerPlayer player)) {
            source.sendFailure(Component.literal("Abre Zian GUI desde el juego.")); return 0;
        }
        return open(player, "principal") ? 1 : 0;
    }
    public static boolean open(ServerPlayer player, String menu) {
        if (!GATE.request(player.getUUID(), now())) return false;
        var definition = CONFIG.get(menu);
        if (definition == null) {
            player.sendSystemMessage(Component.literal("Menú inexistente o configuración inválida.")); return false;
        }
        if (!PermissionService.allows(player, "zian.gui.open") || !PermissionService.allows(player, definition.permission())) {
            player.sendSystemMessage(Component.literal("No tienes permiso para abrir este menú.")); return false;
        }
        if (!NetworkRegistry.hasChannel(player.connection, GuiPayloads.OpenMenu.TYPE.id())) {
            player.sendSystemMessage(Component.literal("Instala la misma versión de Zian GUI en el cliente.")); return false;
        }
        long nonce = ThreadLocalRandom.current().nextLong();
        GATE.open(player.getUUID(), menu, nonce, now());
        PacketDistributor.sendToPlayer(player, new GuiPayloads.OpenMenu(menu, nonce, definition.title(), definition.buttons().stream()
            .map(a -> new GuiPayloads.ButtonView(a.id(), a.label(), a.icon(),
                a.allowed(node -> PermissionService.allows(player, node)))).toList(), PermissionService.allows(player, "zian.gui.edit")));
        return true;
    }
    public static void click(ServerPlayer player, GuiPayloads.Click click) {
        if (!GATE.valid(player.getUUID(), click.menuId(), click.session(), now()) || !GATE.action(player.getUUID(), now())) return;
        var definition = CONFIG.get(click.menuId());
        if (definition == null) return;
        var action = definition.buttons().stream().filter(a -> a.id().equals(click.buttonId())).findFirst().orElse(null);
        if (action == null) return;
        if (!PermissionService.allows(player, "zian.gui.open") || !PermissionService.allows(player, definition.permission())
            || !action.allowed(node -> PermissionService.allows(player, node))) {
            PacketDistributor.sendToPlayer(player, new GuiPayloads.Feedback(click.session(), "No tienes permiso para usar este botón."));
            return;
        }
        // Consume the session before dispatch: replay/spam cannot execute a second command.
        GATE.close(player.getUUID(), click.session());
        PacketDistributor.sendToPlayer(player, new GuiPayloads.Close(click.session()));
        boolean result = CommandBridge.execute(player, action.command());
        ZianGui.LOGGER.info("[ZianGUI] action=button playerUuid={} menu={} button={} result={}", player.getUUID(), definition.id(), action.id(), result ? "DISPATCHED" : "NOT_CONFIRMED");
        if (!result) player.sendSystemMessage(Component.literal("No se pudo confirmar la ejecución. Revisa los permisos y que el comando exista."));
    }
    public static void closed(ServerPlayer player, long session) { GATE.close(player.getUUID(), session); }
    public static void onLogout(PlayerEvent.PlayerLoggedOutEvent event) { GATE.remove(event.getEntity().getUUID()); MenuEditor.remove(event.getEntity().getUUID()); }
    public static void onStop(ServerStoppedEvent event) { GATE.clear(); MenuEditor.clear(); }
}

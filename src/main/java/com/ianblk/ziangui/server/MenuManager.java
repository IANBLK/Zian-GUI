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

public final class MenuManager {
    private record Action(String id, String label, String icon, String permission, String command) {}
    private static final List<Action> ACTIONS = List.of(
        new Action("spawn", "Spawn", "minecraft:compass", "eternalcore.spawn", "spawn"),
        new Action("heal", "Curar Pokémon", "minecraft:golden_apple", "cobblemon.command.healpokemon", "healpokemon"));
    private static final SessionGate GATE = new SessionGate();
    private MenuManager() {}
    private static long now() { return System.nanoTime() / 1_000_000; }
    public static void registerCommands(RegisterCommandsEvent event) {
        event.getDispatcher().register(Commands.literal(ZianGui.COMMAND_ROOT)
            .requires(source -> true) // Explicit public root prevents Youer implicit OP gating.
            .executes(ctx -> openCommand(ctx.getSource()))
            .then(Commands.literal("open").executes(ctx -> openCommand(ctx.getSource()))));
    }
    private static int openCommand(net.minecraft.commands.CommandSourceStack source) {
        if (!(source.getEntity() instanceof ServerPlayer player)) {
            source.sendFailure(Component.literal("Abre Zian GUI desde el juego.")); return 0;
        }
        return open(player, "principal") ? 1 : 0;
    }
    public static boolean open(ServerPlayer player, String menu) {
        if (!GATE.request(player.getUUID(), now())) return false;
        if (!menu.equals("principal")) return false;
        if (!PermissionService.allows(player, "zian.gui.open") || !PermissionService.allows(player, "zian.gui.menu.principal")) {
            player.sendSystemMessage(Component.literal("No tienes permiso para abrir este menú.")); return false;
        }
        if (!NetworkRegistry.hasChannel(player.connection, GuiPayloads.OpenMenu.TYPE.id())) {
            player.sendSystemMessage(Component.literal("Instala la misma versión de Zian GUI en el cliente.")); return false;
        }
        long nonce = ThreadLocalRandom.current().nextLong();
        GATE.open(player.getUUID(), menu, nonce, now());
        PacketDistributor.sendToPlayer(player, new GuiPayloads.OpenMenu(menu, nonce, ACTIONS.stream()
            .map(a -> new GuiPayloads.ButtonView(a.id, a.label, a.icon, PermissionService.allows(player, a.permission))).toList()));
        return true;
    }
    public static void click(ServerPlayer player, GuiPayloads.Click click) {
        if (!GATE.valid(player.getUUID(), click.menuId(), click.session(), now()) || !GATE.action(player.getUUID(), now())) return;
        var action = ACTIONS.stream().filter(a -> a.id.equals(click.buttonId())).findFirst().orElse(null);
        if (action == null) return;
        if (!PermissionService.allows(player, "zian.gui.open") || !PermissionService.allows(player, "zian.gui.menu.principal")
            || !PermissionService.allows(player, action.permission)) {
            PacketDistributor.sendToPlayer(player, new GuiPayloads.Feedback(click.session(), "No tienes permiso para usar este botón."));
            return;
        }
        // Consume the session before dispatch: replay/spam cannot execute a second command.
        GATE.close(player.getUUID(), click.session());
        PacketDistributor.sendToPlayer(player, new GuiPayloads.Close(click.session()));
        boolean result = CommandBridge.execute(player, action.command);
        ZianGui.LOGGER.info("[ZianGUI] action=button playerUuid={} button={} result={}", player.getUUID(), action.id, result ? "DISPATCHED" : "NOT_CONFIRMED");
        if (!result) player.sendSystemMessage(Component.literal("No se pudo confirmar la ejecución. Revisa los permisos y que el comando exista."));
    }
    public static void closed(ServerPlayer player, long session) { GATE.close(player.getUUID(), session); }
    public static void onLogout(PlayerEvent.PlayerLoggedOutEvent event) { GATE.remove(event.getEntity().getUUID()); }
    public static void onStop(ServerStoppedEvent event) { GATE.clear(); }
}

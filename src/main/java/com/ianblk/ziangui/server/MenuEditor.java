package com.ianblk.ziangui.server;

import com.ianblk.ziangui.ZianGui;
import com.ianblk.ziangui.network.EditorPayloads;
import com.ianblk.ziangui.network.GuiPayloads;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.registration.NetworkRegistry;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

public final class MenuEditor {
    /** Validation failure with a translatable reason for the admin; the Spanish message stays in the log. */
    private static final class Rejected extends IllegalArgumentException {
        final Component reason;
        Rejected(String log, Component reason) { super(log); this.reason = reason; }
    }
    private record EditSession(String menu, long nonce, long expires, String revision) {}
    private static final Map<UUID, EditSession> SESSIONS = new HashMap<>();
    private static final SessionGate LIMITS = new SessionGate();
    private static long now() { return System.nanoTime() / 1_000_000; }
    private MenuEditor() {}
    public static boolean open(ServerPlayer player, String menu) {
        if (!LIMITS.request(player.getUUID(), now())) return false;
        if (!PermissionService.allows(player, "zian.gui.edit")) {
            player.sendSystemMessage(com.ianblk.ziangui.Messages.format(Component.translatableWithFallback("ziangui.editor.no_permission", "No tienes permiso para editar menús."))); return false;
        }
        if (!NetworkRegistry.hasChannel(player.connection, EditorPayloads.Open.TYPE.id())) {
            player.sendSystemMessage(com.ianblk.ziangui.Messages.format(Component.translatableWithFallback("ziangui.editor.client_version", "Instala la misma versión de Zian GUI en cliente y servidor."))); return false;
        }
        return show(player, menu, Component.translatableWithFallback("ziangui.editor.select", "Selecciona un botón o pulsa Añadir."));
    }
    private static boolean show(ServerPlayer player, String menu, Component message) {
        var config = MenuManager.configuration(); var definition = config.get(menu);
        if (definition == null) { player.sendSystemMessage(com.ianblk.ziangui.Messages.format(Component.translatableWithFallback("ziangui.menu.missing", "Menú inexistente o configuración inválida."))); return false; }
        long nonce = ThreadLocalRandom.current().nextLong();
        SESSIONS.put(player.getUUID(), new EditSession(menu, nonce, now() + 300_000, config.revision()));
        PacketDistributor.sendToPlayer(player, new EditorPayloads.Open(nonce, MenuConfig.json(definition), message));
        return true;
    }
    public static void change(ServerPlayer player, EditorPayloads.Change request) {
        var session = SESSIONS.get(player.getUUID());
        if (session == null || session.nonce != request.session() || !session.menu.equals(request.menuId())) return;
        if (now() >= session.expires) {
            SESSIONS.remove(player.getUUID()); feedback(player, session, Component.translatableWithFallback("ziangui.editor.expired", "La sesión caducó. Cierra y vuelve a abrir el editor.")); return;
        }
        if (!LIMITS.action(player.getUUID(), now())) { feedback(player, session, Component.translatableWithFallback("ziangui.editor.wait", "Espera un instante antes de guardar otra vez.")); return; }
        if (!PermissionService.allows(player, "zian.gui.edit")) { feedback(player, session, Component.translatableWithFallback("ziangui.editor.no_permission", "No tienes permiso para editar menús.")); return; }
        try {
            var config = MenuManager.configuration();
            if (!config.revision().equals(session.revision)) throw new Rejected("El menú cambió; vuelve a abrir el editor.", Component.translatableWithFallback("ziangui.editor.menu_changed", "El menú cambió; vuelve a abrir el editor."));
            var menu = config.get(session.menu);
            String icon = menu.buttons().stream().filter(a -> a.id().equals(request.existingId())).map(MenuConfig.Action::icon).findFirst().orElse("");
            if (!request.delete() && request.handIcon()) {
                var hand = player.getMainHandItem();
                if (hand.isEmpty()) throw new Rejected("Sostén el objeto del icono en la mano principal al guardar.", Component.translatableWithFallback("ziangui.editor.hand_empty", "Sostén el objeto del icono en la mano principal al guardar."));
                icon = BuiltInRegistries.ITEM.getKey(hand.getItem()).toString();
            }
            if (!request.delete() && icon.isEmpty()) throw new Rejected("Para añadir un botón, selecciona el icono de la mano.", Component.translatableWithFallback("ziangui.editor.need_icon", "Para añadir un botón, selecciona el icono de la mano."));
            var next = MenuEdits.apply(menu, request.existingId(), request.delete(), request.id(), request.name(), icon,
                request.command(), request.permissions(), request.position());
            if(!request.delete()){
                String editedId=next.buttons().get(request.position()-1).id();
                var buttons=next.buttons().stream().map(a->a.id().equals(editedId)?new MenuConfig.Action(a.id(),a.label(),a.icon(),a.permissions(),a.command(),request.requirement()):a).toList();
                next=new MenuConfig.Menu(next.id(),next.title(),next.permission(),buttons);
            }
            config.save(MenuManager.directory(), session.revision, next);
            MenuManager.invalidate(player.server);
            invalidate(player.server);
            ZianGui.LOGGER.info("[ZianGUI] action=edit admin={} menu={} button={} result={}", player.getUUID(), menu.id(), request.delete() ? request.existingId() : request.id(), request.delete() ? "DELETED" : "SAVED");
            show(player, menu.id(), Component.translatableWithFallback("ziangui.editor.saved", "Guardado. Respaldo: archivo del menú .json.bak"));
        } catch (java.io.IOException | IllegalArgumentException error) {
            ZianGui.LOGGER.warn("[ZianGUI] action=edit admin={} menu={} result=REJECTED reason={}", player.getUUID(), request.menuId(), error.getMessage());
            Component reason = error instanceof Rejected r ? r.reason : Component.literal(String.valueOf(error.getMessage()));
            feedback(player, session, Component.translatableWithFallback("ziangui.editor.rejected", "No se guardó: %s", reason));
        }
    }
    private static void feedback(ServerPlayer player, EditSession session, Component message) {
        PacketDistributor.sendToPlayer(player, new GuiPayloads.Feedback(session.nonce, message));
    }
    public static void close(ServerPlayer player, long nonce) {
        var session = SESSIONS.get(player.getUUID());
        if (session != null && session.nonce == nonce) SESSIONS.remove(player.getUUID());
    }
    public static void remove(UUID player) { SESSIONS.remove(player); LIMITS.remove(player); }
    public static void clear() { SESSIONS.clear(); LIMITS.clear(); }
    public static void invalidate(MinecraftServer server) {
        SESSIONS.forEach((id, session) -> {
            var player = server.getPlayerList().getPlayer(id);
            if (player != null) PacketDistributor.sendToPlayer(player, new GuiPayloads.Close(session.nonce));
        });
        SESSIONS.clear();
    }
}

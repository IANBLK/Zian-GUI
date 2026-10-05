package com.ianblk.ziangui.network;

import com.ianblk.ziangui.ZianGui;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/** Admin-only wire data. Clients never choose an arbitrary item/NBT for the hand icon. */
public final class EditorPayloads {
    private EditorPayloads() {}
    private static <T extends CustomPacketPayload> CustomPacketPayload.Type<T> type(String path) {
        return new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(ZianGui.MOD_ID, path));
    }
    private static String bounded(String s, int max) {
        if (s == null || s.length() > max || s.chars().anyMatch(Character::isISOControl)) throw new IllegalArgumentException("Invalid editor text");
        return s;
    }
    public record Request(String menuId) implements CustomPacketPayload {
        public static final Type<Request> TYPE = EditorPayloads.type("editor_request");
        public static final StreamCodec<RegistryFriendlyByteBuf, Request> CODEC = StreamCodec.of(
            (b, p) -> b.writeUtf(p.menuId, 32), b -> new Request(b.readUtf(32)));
        public Request { if (!bounded(menuId, 32).matches("[a-z0-9_]{1,32}")) throw new IllegalArgumentException("Invalid menu"); }
        public Type<Request> type() { return TYPE; }
    }
    public record Open(long session, String json, String message) implements CustomPacketPayload {
        public static final Type<Open> TYPE = EditorPayloads.type("editor_open");
        public static final StreamCodec<RegistryFriendlyByteBuf, Open> CODEC = StreamCodec.of(
            (b, p) -> { b.writeLong(p.session); b.writeUtf(p.json, 65536); b.writeUtf(p.message, 256); },
            b -> new Open(b.readLong(), b.readUtf(65536), b.readUtf(256)));
        public Open { if (json == null || json.length() > 65536) throw new IllegalArgumentException("Editor menu too large"); bounded(message, 256); }
        public Type<Open> type() { return TYPE; }
    }
    public record Change(String menuId, long session, String existingId, boolean delete,
            String id, String name, String command, String permissions, int position, boolean handIcon) implements CustomPacketPayload {
        public static final Type<Change> TYPE = EditorPayloads.type("editor_change");
        public static final StreamCodec<RegistryFriendlyByteBuf, Change> CODEC = StreamCodec.of(
            (b, p) -> { b.writeUtf(p.menuId, 32); b.writeLong(p.session); b.writeUtf(p.existingId, 32); b.writeBoolean(p.delete);
                b.writeUtf(p.id, 32); b.writeUtf(p.name, 64); b.writeUtf(p.command, 256); b.writeUtf(p.permissions, 1031);
                b.writeVarInt(p.position); b.writeBoolean(p.handIcon); },
            b -> new Change(b.readUtf(32), b.readLong(), b.readUtf(32), b.readBoolean(), b.readUtf(32), b.readUtf(64), b.readUtf(256), b.readUtf(1031), b.readVarInt(), b.readBoolean()));
        public Change {
            if (!bounded(menuId, 32).matches("[a-z0-9_]{1,32}")) throw new IllegalArgumentException("Invalid menu");
            bounded(existingId, 32); bounded(id, 32); bounded(name, 64); bounded(command, 256); bounded(permissions, 1031);
            if (position < 1 || position > 24) throw new IllegalArgumentException("Invalid position");
        }
        public Type<Change> type() { return TYPE; }
    }
    public record Closed(long session) implements CustomPacketPayload {
        public static final Type<Closed> TYPE = EditorPayloads.type("editor_closed");
        public static final StreamCodec<RegistryFriendlyByteBuf, Closed> CODEC = StreamCodec.of(
            (b, p) -> b.writeLong(p.session), b -> new Closed(b.readLong()));
        public Type<Closed> type() { return TYPE; }
    }
}

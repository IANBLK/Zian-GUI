package com.ianblk.ziangui.network;

import com.ianblk.ziangui.ZianGui;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import java.util.List;

/** Wire-only views: no commands, permission nodes or client-supplied actions. */
public final class GuiPayloads {
    private GuiPayloads() {}
    private static <T extends CustomPacketPayload> CustomPacketPayload.Type<T> type(String path) {
        return new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(ZianGui.MOD_ID, path));
    }
    private static String id(String value) {
        if (value == null || !value.matches("[a-z0-9_]{1,32}")) throw new IllegalArgumentException("Invalid menu/button ID");
        return value;
    }
    public record OpenRequest(String menuId) implements CustomPacketPayload {
        public static final Type<OpenRequest> TYPE = GuiPayloads.type("open_request");
        public static final StreamCodec<RegistryFriendlyByteBuf, OpenRequest> CODEC = StreamCodec.of(
            (buf, p) -> buf.writeUtf(p.menuId, 32), buf -> new OpenRequest(buf.readUtf(32)));
        public OpenRequest { menuId = id(menuId); }
        public Type<OpenRequest> type() { return TYPE; }
    }
    public record ButtonView(String id, String label, String icon, boolean enabled) {
        public ButtonView {
            id = GuiPayloads.id(id);
            if (label == null || label.length() > 64 || icon == null || icon.length() > 128)
                throw new IllegalArgumentException("Invalid button view");
        }
    }
    public record OpenMenu(String menuId, long session, String title, List<ButtonView> buttons, boolean editable) implements CustomPacketPayload {
        public static final Type<OpenMenu> TYPE = GuiPayloads.type("open_menu");
        public static final StreamCodec<RegistryFriendlyByteBuf, OpenMenu> CODEC = StreamCodec.of(
            (buf, p) -> {
                buf.writeUtf(p.menuId, 32); buf.writeLong(p.session); buf.writeUtf(p.title, 64); buf.writeVarInt(p.buttons.size());
                for (var b : p.buttons) {
                    buf.writeUtf(b.id, 32); buf.writeUtf(b.label, 64); buf.writeUtf(b.icon, 128); buf.writeBoolean(b.enabled);
                }
                buf.writeBoolean(p.editable);
            }, buf -> {
                String menu = buf.readUtf(32); long session = buf.readLong(); String title = buf.readUtf(64); int count = buf.readVarInt();
                if (count < 0 || count > 24) throw new IllegalArgumentException("Invalid button count");
                var buttons = new java.util.ArrayList<ButtonView>(count);
                for (int i = 0; i < count; i++) buttons.add(new ButtonView(buf.readUtf(32), buf.readUtf(64), buf.readUtf(128), buf.readBoolean()));
                return new OpenMenu(menu, session, title, buttons, buf.readBoolean());
            });
        public OpenMenu {
            menuId = id(menuId); buttons = List.copyOf(buttons);
            if (title == null || title.length() > 64) throw new IllegalArgumentException("Invalid menu title");
            if (buttons.size() > 24 || buttons.stream().map(ButtonView::id).distinct().count() != buttons.size())
                throw new IllegalArgumentException("Invalid button list");
        }
        public Type<OpenMenu> type() { return TYPE; }
    }
    public record Click(String menuId, String buttonId, long session) implements CustomPacketPayload {
        public static final Type<Click> TYPE = GuiPayloads.type("click");
        public static final StreamCodec<RegistryFriendlyByteBuf, Click> CODEC = StreamCodec.of(
            (buf, p) -> { buf.writeUtf(p.menuId, 32); buf.writeUtf(p.buttonId, 32); buf.writeLong(p.session); },
            buf -> new Click(buf.readUtf(32), buf.readUtf(32), buf.readLong()));
        public Click { menuId = id(menuId); buttonId = id(buttonId); }
        public Type<Click> type() { return TYPE; }
    }
    public record Closed(long session) implements CustomPacketPayload {
        public static final Type<Closed> TYPE = GuiPayloads.type("closed");
        public static final StreamCodec<RegistryFriendlyByteBuf, Closed> CODEC = StreamCodec.of(
            (buf, p) -> buf.writeLong(p.session), buf -> new Closed(buf.readLong()));
        public Type<Closed> type() { return TYPE; }
    }
    public record Close(long session) implements CustomPacketPayload {
        public static final Type<Close> TYPE = GuiPayloads.type("close");
        public static final StreamCodec<RegistryFriendlyByteBuf, Close> CODEC = StreamCodec.of(
            (buf, p) -> buf.writeLong(p.session), buf -> new Close(buf.readLong()));
        public Type<Close> type() { return TYPE; }
    }
    public record Feedback(long session, String text) implements CustomPacketPayload {
        public static final Type<Feedback> TYPE = GuiPayloads.type("feedback");
        public static final StreamCodec<RegistryFriendlyByteBuf, Feedback> CODEC = StreamCodec.of(
            (buf, p) -> { buf.writeLong(p.session); buf.writeUtf(p.text, 256); },
            buf -> new Feedback(buf.readLong(), buf.readUtf(256)));
        public Feedback {
            if (text == null || text.length() > 256) throw new IllegalArgumentException("Invalid feedback");
        }
        public Type<Feedback> type() { return TYPE; }
    }
}

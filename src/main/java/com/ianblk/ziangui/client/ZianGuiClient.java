package com.ianblk.ziangui.client;

import com.ianblk.ziangui.network.GuiNetwork;
import com.ianblk.ziangui.network.GuiPayloads;
import com.mojang.blaze3d.platform.InputConstants;
import org.lwjgl.glfw.GLFW;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.network.PacketDistributor;

public final class ZianGuiClient {
    private static final KeyMapping OPEN = new KeyMapping("key.ziangui.open", InputConstants.Type.KEYSYM,
        GLFW.GLFW_KEY_Z, "key.categories.ziangui");
    private ZianGuiClient() {}
    public static void init(IEventBus modBus) {
        modBus.addListener((RegisterKeyMappingsEvent event) -> event.register(OPEN));
        NeoForge.EVENT_BUS.addListener(ZianGuiClient::tick);
        NeoForge.EVENT_BUS.addListener(ZianGuiClient::logout);
        GuiNetwork.openClient = p -> {
            var mc = Minecraft.getInstance();
            if (mc.player != null && mc.level != null) mc.setScreen(new ZianGuiScreen(p));
        };
        GuiNetwork.closeClient = p -> {
            var mc = Minecraft.getInstance();
            if (mc.screen instanceof ZianGuiScreen screen && screen.session() == p.session()) mc.setScreen(null);
            if (mc.screen instanceof MenuEditorScreen screen && screen.session() == p.session()) mc.setScreen(null);
        };
        GuiNetwork.editorClient = p -> {
            var mc = Minecraft.getInstance();
            if (mc.player != null && mc.level != null) mc.setScreen(new MenuEditorScreen(p));
        };
        GuiNetwork.feedbackClient = p -> {
            var mc = Minecraft.getInstance();
            if (mc.screen instanceof ZianGuiScreen screen && screen.session() == p.session()) screen.feedback(p.text());
            else if (mc.screen instanceof MenuEditorScreen screen && screen.session() == p.session()) screen.feedback(p.text());
            else if (mc.player != null) mc.player.displayClientMessage(net.minecraft.network.chat.Component.literal(p.text()), false);
        };
    }
    private static void tick(ClientTickEvent.Post event) {
        var mc = Minecraft.getInstance();
        while (OPEN.consumeClick()) {
            if (mc.player != null && mc.level != null && mc.screen == null)
                PacketDistributor.sendToServer(new GuiPayloads.OpenRequest("principal"));
        }
    }
    private static void logout(ClientPlayerNetworkEvent.LoggingOut event) {
        var mc = Minecraft.getInstance();
        if (mc.screen instanceof ZianGuiScreen || mc.screen instanceof MenuEditorScreen) mc.setScreen(null);
        while (OPEN.consumeClick()) { /* discard requests from the old connection */ }
    }
}

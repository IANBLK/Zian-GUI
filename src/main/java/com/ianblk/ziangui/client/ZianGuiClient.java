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
    private static String returnMenu,returnOwner;private static long returnUntil;
    private static void armReturn(String menu){returnMenu=menu;returnOwner=null;returnUntil=System.nanoTime()+2_000_000_000L;}
    public static boolean returnToLastMenu(){var mc=Minecraft.getInstance();if(returnMenu==null||mc.screen==null||!mc.screen.getClass().getName().equals(returnOwner)||mc.getConnection()==null)return false;String menu=returnMenu;returnMenu=null;returnOwner=null;mc.setScreen(null);PacketDistributor.sendToServer(new GuiPayloads.OpenRequest(menu));return true;}
    private ZianGuiClient() {}
    public static void init(IEventBus modBus) {
        GuiVisualSmoke.register();
        modBus.addListener((RegisterKeyMappingsEvent event) -> event.register(OPEN));
        NeoForge.EVENT_BUS.addListener(ZianGuiClient::tick);
        NeoForge.EVENT_BUS.addListener(ZianGuiClient::logout);
        GuiNetwork.openClient = p -> {
            var mc = Minecraft.getInstance();
            if (mc.player != null && mc.level != null) mc.setScreen(new ZianGuiScreen(p));
        };
        GuiNetwork.closeClient = p -> {
            var mc = Minecraft.getInstance();
            if (mc.screen instanceof ZianGuiScreen screen && screen.session() == p.session()) {if(screen.wasSubmitted())armReturn(screen.menuId());mc.setScreen(null);}
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
        if(returnMenu!=null&&returnOwner==null){if(System.nanoTime()>returnUntil)returnMenu=null;else if(mc.screen!=null){String name=mc.screen.getClass().getName();if(name.startsWith("com.ianblk.zianmobs.")||name.startsWith("com.ianblk.zianrct.")||name.startsWith("com.zianblk.ziangts.")||name.startsWith("com.zianblk.zianutilities."))returnOwner=name;}}
        while (OPEN.consumeClick()) {
            if (mc.player != null && mc.level != null && mc.screen == null)
                PacketDistributor.sendToServer(new GuiPayloads.OpenRequest("principal"));
        }
    }
    private static void logout(ClientPlayerNetworkEvent.LoggingOut event) {
        var mc = Minecraft.getInstance();
        if (mc.screen instanceof ZianGuiScreen || mc.screen instanceof MenuEditorScreen) mc.setScreen(null);
        ZianGuiScreen.clearNavigation();returnMenu=null;returnOwner=null;
        while (OPEN.consumeClick()) { /* discard requests from the old connection */ }
    }
}

package com.ianblk.ziangui.network;

import com.ianblk.ziangui.server.MenuManager;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import java.util.function.Consumer;

/** Client callbacks are installed exclusively by the client entry point. */
public final class GuiNetwork {
    public static Consumer<GuiPayloads.OpenMenu> openClient = p -> {};
    public static Consumer<GuiPayloads.Close> closeClient = p -> {};
    public static Consumer<GuiPayloads.Feedback> feedbackClient = p -> {};
    private GuiNetwork() {}
    public static void register(RegisterPayloadHandlersEvent event) {
        // Required on both endpoints: do not use optional() on this registrar.
        var r = event.registrar("2");
        r.playToServer(GuiPayloads.OpenRequest.TYPE, GuiPayloads.OpenRequest.CODEC,
            (p, ctx) -> ctx.enqueueWork(() -> {
                if (ctx.player() instanceof ServerPlayer player) MenuManager.open(player, p.menuId());
            }));
        r.playToServer(GuiPayloads.Click.TYPE, GuiPayloads.Click.CODEC,
            (p, ctx) -> ctx.enqueueWork(() -> {
                if (ctx.player() instanceof ServerPlayer player) MenuManager.click(player, p);
            }));
        r.playToServer(GuiPayloads.Closed.TYPE, GuiPayloads.Closed.CODEC,
            (p, ctx) -> ctx.enqueueWork(() -> {
                if (ctx.player() instanceof ServerPlayer player) MenuManager.closed(player, p.session());
            }));
        r.playToClient(GuiPayloads.OpenMenu.TYPE, GuiPayloads.OpenMenu.CODEC,
            (p, ctx) -> ctx.enqueueWork(() -> openClient.accept(p)));
        r.playToClient(GuiPayloads.Close.TYPE, GuiPayloads.Close.CODEC,
            (p, ctx) -> ctx.enqueueWork(() -> closeClient.accept(p)));
        r.playToClient(GuiPayloads.Feedback.TYPE, GuiPayloads.Feedback.CODEC,
            (p, ctx) -> ctx.enqueueWork(() -> feedbackClient.accept(p)));
    }
}

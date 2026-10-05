package com.ianblk.ziangui.client;

import com.ianblk.ziangui.network.GuiPayloads;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.neoforge.network.PacketDistributor;

/** Local adaptation of the Zian RCT charcoal/gold card style; no dependency on RCT. */
public final class ZianGuiScreen extends Screen {
    private final GuiPayloads.OpenMenu menu;
    private String feedback = "";
    private int left, top, panelWidth, panelHeight;
    public ZianGuiScreen(GuiPayloads.OpenMenu menu) {
        super(Component.literal("Zian GUI")); this.menu = menu;
    }
    public long session() { return menu.session(); }
    public void feedback(String text) { feedback = text; }
    @Override protected void init() {
        panelWidth = Math.min(260, width - 16);
        panelHeight = Math.min(176, height - 16);
        left = (width - panelWidth) / 2; top = (height - panelHeight) / 2;
        int count = menu.buttons().size();
        int cardWidth = panelWidth - 24;
        int cardHeight = 30;
        for (int i = 0; i < count; i++) {
            var view = menu.buttons().get(i);
            addRenderableWidget(new Card(left + 12, top + 48 + i * (cardHeight + 8), cardWidth, cardHeight, view,
                button -> PacketDistributor.sendToServer(new GuiPayloads.Click(menu.menuId(), view.id(), menu.session()))));
        }
        addRenderableWidget(new ThemedButton(width / 2 - 48, top + panelHeight - 30, 96, 20,
            Component.literal("Cerrar"), button -> onClose()));
    }
    @Override public boolean isPauseScreen() { return false; }
    @Override public void renderBackground(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        graphics.fill(0, 0, width, height, 0x90000000);
    }
    @Override public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(graphics, mouseX, mouseY, partialTick);
        graphics.fill(left, top, left + panelWidth, top + panelHeight, 0xF0181818);
        graphics.fill(left, top, left + panelWidth, top + 3, 0xFFF2C14E);
        graphics.drawCenteredString(font, title, width / 2, top + 15, 0xFFF2C14E);
        graphics.drawCenteredString(font, Component.literal("Menú principal"), width / 2, top + 30, 0xFFAAAAAA);
        if (!feedback.isEmpty()) graphics.drawWordWrap(font, Component.literal(feedback), left + 12,
            top + panelHeight - 54, panelWidth - 24, 0xFFFFAA55);
        super.render(graphics, mouseX, mouseY, partialTick);
    }
    @Override public void removed() {
        if (minecraft != null && minecraft.getConnection() != null)
            PacketDistributor.sendToServer(new GuiPayloads.Closed(menu.session()));
    }
    private static class ThemedButton extends Button {
        protected ThemedButton(int x, int y, int width, int height, Component label, OnPress press) {
            super(x, y, width, height, label, press, DEFAULT_NARRATION);
        }
        protected void frame(GuiGraphics graphics, boolean enabled) {
            int x = getX(), y = getY();
            graphics.fill(x, y, x + width, y + height, enabled ? 0xFFF2C14E : 0xFF666666);
            graphics.fill(x + 1, y + 1, x + width - 1, y + height - 1,
                isHoveredOrFocused() ? 0xFF444444 : 0xFF2B2B2B);
        }
        @Override protected void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
            frame(graphics, true);
            var font = Minecraft.getInstance().font;
            graphics.drawCenteredString(font, getMessage(), getX() + width / 2,
                getY() + (height - font.lineHeight) / 2, 0xFFFFFFFF);
        }
    }
    private static final class Card extends ThemedButton {
        private final GuiPayloads.ButtonView view;
        private final ItemStack icon;
        private Card(int x, int y, int width, int height, GuiPayloads.ButtonView view, OnPress press) {
            super(x, y, width, height, Component.literal(view.label()), press);
            this.view = view;
            ResourceLocation id = ResourceLocation.tryParse(view.icon());
            this.icon = new ItemStack(id != null && BuiltInRegistries.ITEM.containsKey(id)
                ? BuiltInRegistries.ITEM.get(id) : Items.BARRIER);
            setTooltip(Tooltip.create(Component.literal(view.enabled() ? view.label() : "Bloqueado: necesitas permiso para usar este botón.")));
        }
        @Override protected void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
            frame(graphics, view.enabled());
            int x = getX(), y = getY();
            graphics.renderItem(icon, x + 8, y + (height - 16) / 2);
            var font = Minecraft.getInstance().font;
            graphics.drawString(font, getMessage(), x + 32, y + (height - font.lineHeight) / 2,
                view.enabled() ? 0xFFFFFFFF : 0xFF999999);
        }
    }
}

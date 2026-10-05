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
        panelWidth = Math.min(380, width - 16);
        panelHeight = Math.min(230, height - 16);
        left = (width - panelWidth) / 2; top = (height - panelHeight) / 2;
        int count = menu.buttons().size();
        int cardWidth = Math.min(124, (panelWidth - 40) / Math.max(1, count));
        int cardHeight = Math.max(48, Math.min(100, panelHeight - 112));
        int start = (width - (count * cardWidth + Math.max(0, count - 1) * 12)) / 2;
        for (int i = 0; i < count; i++) {
            var view = menu.buttons().get(i);
            addRenderableWidget(new Card(start + i * (cardWidth + 12), top + 48, cardWidth, cardHeight, view,
                button -> PacketDistributor.sendToServer(new GuiPayloads.Click(menu.menuId(), view.id(), menu.session()))));
        }
        addRenderableWidget(Button.builder(Component.literal("Cerrar"), button -> onClose())
            .bounds(width / 2 - 50, top + panelHeight - 30, 100, 20).build());
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
            top + panelHeight - 68, panelWidth - 24, 0xFFFFAA55);
        super.render(graphics, mouseX, mouseY, partialTick);
    }
    @Override public void removed() {
        if (minecraft != null && minecraft.getConnection() != null)
            PacketDistributor.sendToServer(new GuiPayloads.Closed(menu.session()));
    }
    private static final class Card extends Button {
        private final GuiPayloads.ButtonView view;
        private final ItemStack icon;
        private Card(int x, int y, int width, int height, GuiPayloads.ButtonView view, OnPress press) {
            super(x, y, width, height, Component.literal(view.label()), press, DEFAULT_NARRATION);
            this.view = view;
            ResourceLocation id = ResourceLocation.tryParse(view.icon());
            this.icon = new ItemStack(id != null && BuiltInRegistries.ITEM.containsKey(id)
                ? BuiltInRegistries.ITEM.get(id) : Items.BARRIER);
            setTooltip(Tooltip.create(Component.literal(view.enabled() ? view.label() : "Bloqueado: necesitas permiso para usar este botón.")));
        }
        @Override protected void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
            int x = getX(), y = getY(), border = view.enabled() ? 0xFFF2C14E : 0xFF666666;
            graphics.fill(x, y, x + width, y + height, border);
            graphics.fill(x + 1, y + 1, x + width - 1, y + height - 1, isHoveredOrFocused() ? 0xFF444444 : 0xFF2B2B2B);
            int scale = height >= 80 ? 2 : 1;
            graphics.pose().pushPose();
            graphics.pose().translate(x + (width - 16 * scale) / 2.0F, y + 10, 0);
            graphics.pose().scale(scale, scale, 1);
            graphics.renderItem(icon, 0, 0);
            graphics.pose().popPose();
            var font = Minecraft.getInstance().font;
            graphics.drawWordWrap(font, getMessage(), x + 6, y + height - 29, width - 12,
                view.enabled() ? 0xFFFFFFFF : 0xFF999999);
            if (!view.enabled()) graphics.drawCenteredString(font, Component.literal("Bloqueado"), x + width / 2,
                y + height - 12, 0xFF999999);
        }
    }
}

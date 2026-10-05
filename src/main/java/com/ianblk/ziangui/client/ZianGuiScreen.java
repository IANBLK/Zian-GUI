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
    private int page, pages;
    public ZianGuiScreen(GuiPayloads.OpenMenu menu) {
        super(Component.literal("Zian GUI")); this.menu = menu;
    }
    public long session() { return menu.session(); }
    public void feedback(String text) { feedback = text; }
    @Override protected void init() {
        panelWidth = Math.min(304, width - 16);
        int rowsPerPage = Math.max(1, Math.min(6, (height - 16 - 108) / 38));
        int perPage = rowsPerPage * 2;
        pages = Math.max(1, (menu.buttons().size() + perPage - 1) / perPage);
        page = Math.min(page, pages - 1);
        int first = page * perPage;
        int count = Math.min(perPage, menu.buttons().size() - first);
        int rows = Math.max(1, (count + 1) / 2);
        panelHeight = Math.min(108 + rows * 38, height - 16);
        left = (width - panelWidth) / 2; top = (height - panelHeight) / 2;
        int cardWidth = (panelWidth - 32) / 2;
        int cardHeight = 30;
        for (int i = 0; i < count; i++) {
            var view = menu.buttons().get(first + i);
            addRenderableWidget(new Card(left + 12 + (i % 2) * (cardWidth + 8),
                top + 48 + (i / 2) * (cardHeight + 8), cardWidth, cardHeight, view,
                button -> PacketDistributor.sendToServer(new GuiPayloads.Click(menu.menuId(), view.id(), menu.session()))));
        }
        addRenderableWidget(new ThemedButton(width / 2 - 48, top + panelHeight - 30, 96, 20,
            Component.literal("Cerrar"), button -> onClose()));
        if (pages > 1) {
            var prev = addRenderableWidget(new ThemedButton(left + 12, top + panelHeight - 30, 28, 20,
                Component.literal("<"), button -> { page--; rebuildWidgets(); }));
            prev.active = page > 0;
            var next = addRenderableWidget(new ThemedButton(left + panelWidth - 40, top + panelHeight - 30, 28, 20,
                Component.literal(">"), button -> { page++; rebuildWidgets(); }));
            next.active = page + 1 < pages;
        }
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
        String heading = menu.title() + (pages > 1 ? " · " + (page + 1) + "/" + pages : "");
        if (font.width(heading) > panelWidth - 24) heading = font.plainSubstrByWidth(heading, panelWidth - 36) + "…";
        graphics.drawCenteredString(font, Component.literal(heading), width / 2, top + 30, 0xFFAAAAAA);
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
            frame(graphics, active);
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
            var lines = font.split(getMessage(), width - 40);
            int lineY = y + (height - Math.min(2, lines.size()) * font.lineHeight) / 2;
            for (int i = 0; i < Math.min(2, lines.size()); i++)
                graphics.drawString(font, lines.get(i), x + 32, lineY + i * font.lineHeight,
                    view.enabled() ? 0xFFFFFFFF : 0xFF999999);
        }
    }
}

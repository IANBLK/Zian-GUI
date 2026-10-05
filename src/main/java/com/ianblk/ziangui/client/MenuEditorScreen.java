package com.ianblk.ziangui.client;

import com.ianblk.ziangui.network.EditorPayloads;
import com.ianblk.ziangui.server.MenuConfig;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.neoforge.network.PacketDistributor;

/** Administrative UI; changes are requests, never local authority or command execution. */
public final class MenuEditorScreen extends Screen {
    private final EditorPayloads.Open data;
    private final MenuConfig.Menu menu;
    private int left, top, panelWidth, panelHeight, page, rows, rowHeight;
    private String feedback;
    private Draft draft;
    private boolean deleteConfirmed, pending;
    private static final String[] LABELS = {"ID", "Nombre", "Comando", "Permisos", "Posición"};
    private static final int[] LIMITS = {32, 64, 256, 1031, 2};
    private static final class Draft {
        String existing = "", icon = "minecraft:barrier";
        String[] fields = {"", "", "", "", "1"};
        boolean hand;
    }
    public MenuEditorScreen(EditorPayloads.Open data) {
        super(Component.literal("Zian GUI · Editor"));
        this.data = data; this.menu = MenuConfig.parse(data.json()); this.feedback = data.message();
    }
    public long session() { return data.session(); }
    public void feedback(String message) { feedback = message; pending = false; deleteConfirmed = false; rebuildWidgets(); }
    @Override protected void init() {
        panelWidth = Math.min(440, width - 16); panelHeight = Math.min(244, height - 16);
        left = (width - panelWidth) / 2; top = (height - panelHeight) / 2;
        if (draft == null) list(); else form();
    }
    private ZianGuiScreen.ThemedButton button(int x, int y, int w, String label, net.minecraft.client.gui.components.Button.OnPress press) {
        return addRenderableWidget(new ZianGuiScreen.ThemedButton(x, y, w, 18, Component.literal(label), press));
    }
    private void list() {
        rows = Math.max(1, Math.min(6, (panelHeight - 110) / 22));
        int pages = Math.max(1, (menu.buttons().size() + rows - 1) / rows);
        page = Math.min(page, pages - 1);
        int first = page * rows;
        for (int i = first; i < Math.min(first + rows, menu.buttons().size()); i++) {
            var action = menu.buttons().get(i); int position = i + 1;
            var b = button(left + 12, top + 40 + (i - first) * 22, panelWidth - 24,
                position + ". " + clipped(action.label(), panelWidth - 60), ignored -> select(action, position));
            b.setTooltip(Tooltip.create(Component.literal(action.id() + " · /" + action.command())));
        }
        int y = top + panelHeight - 28;
        var prev = button(left + 12, y, 22, "<", b -> { page--; rebuildWidgets(); }); prev.active = page > 0;
        var next = button(left + panelWidth - 34, y, 22, ">", b -> { page++; rebuildWidgets(); }); next.active = page + 1 < pages;
        int w = Math.min(104, (panelWidth - 88) / 2);
        var add = button(width / 2 - w - 4, y, w, "Añadir", b -> {
            draft = new Draft(); draft.hand = true; draft.fields[4] = Integer.toString(menu.buttons().size() + 1);
            feedback = "Sostén el icono en la mano principal al guardar."; rebuildWidgets();
        }); add.active = menu.buttons().size() < MenuConfig.MAX_BUTTONS;
        button(width / 2 + 4, y, w, "Cerrar", b -> onClose());
    }
    private void select(MenuConfig.Action action, int position) {
        draft = new Draft(); draft.existing = action.id(); draft.icon = action.icon();
        draft.fields = new String[]{action.id(), action.label(), action.command(), String.join(",", action.permissions()), Integer.toString(position)};
        feedback = "Edita los datos y guarda; el comando se ejecuta como jugador."; deleteConfirmed = false; rebuildWidgets();
    }
    private void form() {
        rowHeight = Math.max(19, (panelHeight - 110) / 5);
        for (int i = 0; i < LABELS.length; i++) {
            final int field = i;
            var box = addRenderableWidget(new EditBox(font, left + 88, top + 38 + i * rowHeight,
                panelWidth - 100, 16, Component.literal(LABELS[i])));
            box.setMaxLength(LIMITS[i]); box.setValue(draft.fields[i]); box.setEditable(!pending);
            box.setResponder(value -> draft.fields[field] = value);
            if (i == 2) box.setTooltip(Tooltip.create(Component.literal("Un comando de jugador. Ejemplo: ZianUtilities gacha")));
            if (i == 3) box.setTooltip(Tooltip.create(Component.literal("Permisos separados por comas; vacío si no necesitas requisitos adicionales.")));
            if (i == 4) box.setTooltip(Tooltip.create(Component.literal("Orden de lectura: izquierda, derecha, siguiente fila. Empieza en 1.")));
        }
        var hand = button(left + 36, top + 38 + 5 * rowHeight + 2, panelWidth - 48,
            draft.hand ? "Icono: objeto en la mano" : "Icono: conservar actual", b -> { draft.hand = !draft.hand; rebuildWidgets(); });
        hand.active = !pending;
        int w = Math.min(104, (panelWidth - 40) / 3), y = top + panelHeight - 28;
        var back = button(width / 2 - w * 3 / 2 - 8, y, w, "Volver", b -> {
            draft = null; deleteConfirmed = false; feedback = data.message(); rebuildWidgets();
        }); back.active = !pending;
        var remove = button(width / 2 - w / 2, y, w, deleteConfirmed ? "¿Eliminar?" : "Eliminar", b -> {
            if (!deleteConfirmed) { deleteConfirmed = true; feedback = "Pulsa ¿Eliminar? para confirmar."; rebuildWidgets(); }
            else send(true);
        }); remove.active = !pending && !draft.existing.isEmpty();
        var save = button(width / 2 + w / 2 + 8, y, w, "Guardar", b -> send(false)); save.active = !pending;
    }
    private void send(boolean delete) {
        try {
            int position = delete ? 1 : Integer.parseInt(draft.fields[4]);
            var p = new EditorPayloads.Change(menu.id(), data.session(), draft.existing, delete,
                draft.fields[0], draft.fields[1], draft.fields[2], draft.fields[3], position, draft.hand);
            PacketDistributor.sendToServer(p); pending = true; feedback = "Guardando…"; rebuildWidgets();
        } catch (IllegalArgumentException error) { feedback("Revisa los campos y usa una posición entre 1 y 24."); }
    }
    private String clipped(String text, int max) { return font.width(text) > max ? font.plainSubstrByWidth(text, Math.max(1, max - 12)) + "…" : text; }
    @Override public boolean isPauseScreen() { return false; }
    @Override public void renderBackground(GuiGraphics g, int x, int y, float tick) { g.fill(0, 0, width, height, 0x90000000); }
    @Override public void render(GuiGraphics g, int x, int y, float tick) {
        renderBackground(g, x, y, tick);
        g.fill(left, top, left + panelWidth, top + panelHeight, 0xF0181818);
        g.fill(left, top, left + panelWidth, top + 3, 0xFFF2C14E);
        g.drawCenteredString(font, title, width / 2, top + 10, 0xFFF2C14E);
        g.drawCenteredString(font, clipped(menu.title() + (draft == null ? " · " + menu.buttons().size() + "/24 botones" : " · Editar botón"), panelWidth - 24), width / 2, top + 24, 0xFFAAAAAA);
        if (draft != null) {
            for (int i = 0; i < LABELS.length; i++) g.drawString(font, LABELS[i], left + 12, top + 42 + i * rowHeight, 0xFFFFFFFF);
            ItemStack icon;
            if (draft.hand) icon = minecraft.player == null ? ItemStack.EMPTY : minecraft.player.getMainHandItem();
            else { var id = ResourceLocation.tryParse(draft.icon); icon = new ItemStack(id != null && BuiltInRegistries.ITEM.containsKey(id) ? BuiltInRegistries.ITEM.get(id) : Items.BARRIER); }
            g.renderItem(icon, left + 12, top + 40 + 5 * rowHeight);
        }
        var lines = font.split(Component.literal(feedback), panelWidth - 24);
        for (int i = 0; i < Math.min(2, lines.size()); i++) g.drawString(font, lines.get(i), left + 12, top + panelHeight - 54 + i * 10, 0xFFFFCC66);
        super.render(g, x, y, tick);
    }
    @Override public void removed() {
        if (minecraft != null && minecraft.getConnection() != null) PacketDistributor.sendToServer(new EditorPayloads.Closed(data.session()));
    }
}

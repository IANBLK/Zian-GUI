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
public final class MenuEditorScreen extends ResponsiveScreen {
    private final EditorPayloads.Open data;
    private final MenuConfig.Menu menu;
    private int left, top, panelWidth, panelHeight, page, rows, rowHeight;
    private Component feedback;
    private Draft draft;
    private boolean deleteConfirmed, pending;
    private static final Component[] LABELS = {Component.translatableWithFallback("ziangui.editor.field.id", "ID"), Component.translatableWithFallback("ziangui.editor.field.name", "Nombre"), Component.translatableWithFallback("ziangui.editor.field.command", "Comando"), Component.translatableWithFallback("ziangui.editor.field.permissions", "Permisos"), Component.translatableWithFallback("ziangui.editor.field.position", "Posición"), Component.translatableWithFallback("ziangui.editor.field.requirement", "Requisito")};
    private static final int[] LIMITS = {32, 64, 256, 1031, 2, 160};
    private static final class Draft {
        String existing = "", icon = "minecraft:barrier";
        String[] fields = {"", "", "", "", "1", ""};
        boolean hand;
    }
    public MenuEditorScreen(EditorPayloads.Open data) {
        super(Component.translatableWithFallback("ui.ziangui.858f785a9c06", "Zian GUI · Editor"));
        this.data = data; this.menu = MenuConfig.parse(data.json()); this.feedback = data.message();
    }
    public long session() { return data.session(); }
    public void feedback(Component message) { feedback = message; pending = false; deleteConfirmed = false; rebuildWidgets(); }
    @Override protected void init() {fitViewport(440,370);
        panelWidth = Math.min(440, width - 16); panelHeight = Math.min(284, height - 16);
        left = (width - panelWidth) / 2; top = (height - panelHeight) / 2;
        if (draft == null) list(); else form();
    }
    private ZianGuiScreen.ThemedButton button(int x, int y, int w, Component label, net.minecraft.client.gui.components.Button.OnPress press) {
        return addRenderableWidget(new ZianGuiScreen.ThemedButton(x, y, w, 18, label, press));
    }
    private void list() {
        rows = Math.max(1, Math.min(6, (panelHeight - 110) / 22));
        int pages = Math.max(1, (menu.buttons().size() + rows - 1) / rows);
        page = Math.min(page, pages - 1);
        int first = page * rows;
        for (int i = first; i < Math.min(first + rows, menu.buttons().size()); i++) {
            var action = menu.buttons().get(i); int position = i + 1;
            var b = button(left + 12, top + 40 + (i - first) * 22, panelWidth - 24,
                Component.literal(position + ". " + clipped(action.label(), panelWidth - 60)), ignored -> select(action, position));
            b.setTooltip(Tooltip.create(Component.literal(action.id() + " · /" + action.command())));
        }
        int y = top + panelHeight - 28;
        var prev = button(left + 12, y, 22, Component.literal("<"), b -> { page--; rebuildWidgets(); }); prev.active = page > 0;
        var next = button(left + panelWidth - 34, y, 22, Component.literal(">"), b -> { page++; rebuildWidgets(); }); next.active = page + 1 < pages;
        int w = Math.min(104, (panelWidth - 88) / 2);
        var add = button(width / 2 - w - 4, y, w, Component.translatableWithFallback("ziangui.editor.add", "Añadir"), b -> {
            draft = new Draft(); draft.hand = true; draft.fields[4] = Integer.toString(menu.buttons().size() + 1);
            feedback = Component.translatableWithFallback("ziangui.editor.feedback.hand", "Sostén el icono en la mano principal al guardar."); rebuildWidgets();
        }); add.active = menu.buttons().size() < MenuConfig.MAX_BUTTONS;
        button(width / 2 + 4, y, w, Component.translatableWithFallback("ui.ziangui.4b0816bbd5e9", "Cerrar"), b -> onClose());
    }
    private void select(MenuConfig.Action action, int position) {
        draft = new Draft(); draft.existing = action.id(); draft.icon = action.icon();
        draft.fields = new String[]{action.id(), action.label(), action.command(), String.join(",", action.permissions()), Integer.toString(position),action.requirement()};
        feedback = Component.translatableWithFallback("ziangui.editor.feedback.edit", "Edita los datos y guarda; el comando se ejecuta como jugador."); deleteConfirmed = false; rebuildWidgets();
    }
    private void form() {
        rowHeight = Math.max(19, (panelHeight - 110) / 6);
        for (int i = 0; i < LABELS.length; i++) {
            final int field = i;
            var box = addRenderableWidget(new EditBox(font, left + 88, top + 38 + i * rowHeight,
                panelWidth - 100, 16, LABELS[i]));
            box.setMaxLength(LIMITS[i]); box.setValue(draft.fields[i]); box.setEditable(!pending);
            box.setResponder(value -> draft.fields[field] = value);
            if(i==0)box.setHint(Component.translatableWithFallback("ziangui.editor.hint.id", "Automático si lo dejas vacío"));
            if(i==5)box.setTooltip(Tooltip.create(Component.translatableWithFallback("ziangui.editor.tip.requirement", "Ayuda para el jugador: rango, coste o condición. Ejemplo: Disponible desde Guardián.")));
            if (i == 2) box.setTooltip(Tooltip.create(Component.translatableWithFallback("ui.ziangui.5864bb78a301", "Un comando de jugador. Ejemplo: ZianUtilities gacha")));
            if (i == 3) box.setTooltip(Tooltip.create(Component.translatableWithFallback("ui.ziangui.0c513fe843e8", "Permisos separados por comas; vacío si no necesitas requisitos adicionales.")));
            if (i == 4) box.setTooltip(Tooltip.create(Component.translatableWithFallback("ui.ziangui.ddaab737dabf", "Orden de lectura: izquierda, derecha, siguiente fila. Empieza en 1.")));
        }
        var hand = button(left + 36, top + 38 + 6 * rowHeight + 2, panelWidth - 48,
            draft.hand ? Component.translatableWithFallback("ziangui.editor.icon.hand", "Icono: objeto en la mano") : Component.translatableWithFallback("ziangui.editor.icon.keep", "Icono: conservar actual"), b -> { draft.hand = !draft.hand; rebuildWidgets(); });
        hand.active = !pending;
        int w = Math.min(104, (panelWidth - 40) / 3), y = top + panelHeight - 28;
        var back = button(width / 2 - w * 3 / 2 - 8, y, w, Component.translatableWithFallback("ui.ziangui.84a715a785a9", "Volver"), b -> {
            draft = null; deleteConfirmed = false; feedback = data.message(); rebuildWidgets();
        }); back.active = !pending;
        var remove = button(width / 2 - w / 2, y, w, deleteConfirmed ? Component.translatableWithFallback("ziangui.editor.delete_confirm", "¿Eliminar?") : Component.translatableWithFallback("ziangui.editor.delete", "Eliminar"), b -> {
            if (!deleteConfirmed) { deleteConfirmed = true; feedback = Component.translatableWithFallback("ziangui.editor.feedback.confirm_delete", "Pulsa ¿Eliminar? para confirmar."); rebuildWidgets(); }
            else send(true);
        }); remove.active = !pending && !draft.existing.isEmpty();
        var save = button(width / 2 + w / 2 + 8, y, w, Component.translatableWithFallback("ziangui.editor.save", "Guardar"), b -> send(false)); save.active = !pending;
    }
    private void send(boolean delete) {
        try {
            int position = delete ? 1 : Integer.parseInt(draft.fields[4]);
            var p = new EditorPayloads.Change(menu.id(), data.session(), draft.existing, delete,
                draft.fields[0], draft.fields[1], draft.fields[2], draft.fields[3], position, draft.hand, draft.fields[5]);
            PacketDistributor.sendToServer(p); pending = true; feedback = Component.translatableWithFallback("ziangui.editor.feedback.saving", "Guardando…"); rebuildWidgets();
        } catch (IllegalArgumentException error) { feedback(Component.translatableWithFallback("ziangui.editor.feedback.invalid_fields", "Revisa los campos y usa una posición entre 1 y 24.")); }
    }
    private String clipped(String text, int max) { return font.width(text) > max ? font.plainSubstrByWidth(text, Math.max(1, max - 12)) + "…" : text; }
    @Override public boolean isPauseScreen() { return false; }
    @Override public void renderBackground(GuiGraphics g, int x, int y, float tick) { g.fill(0, 0, width, height, 0x90000000); }
    @Override protected void renderCanvas(GuiGraphics g, int x, int y, float tick) {
        renderBackground(g, x, y, tick);
        g.fill(left, top, left + panelWidth, top + panelHeight, 0xF0181818);
        g.fill(left, top, left + panelWidth, top + 3, 0xFFF2C14E);
        g.drawCenteredString(font, title, width / 2, top + 10, 0xFFF2C14E);
        g.drawCenteredString(font, clipped(menu.title() + (draft == null ? " · " + Component.translatableWithFallback("ziangui.editor.count", "%s/24 botones", menu.buttons().size()).getString() : " · " + Component.translatableWithFallback("ziangui.editor.edit_button", "Editar botón").getString()), panelWidth - 24), width / 2, top + 24, 0xFFAAAAAA);
        if (draft != null) {
            for (int i = 0; i < LABELS.length; i++) g.drawString(font, LABELS[i], left + 12, top + 42 + i * rowHeight, 0xFFFFFFFF);
            ItemStack icon;
            if (draft.hand) icon = minecraft.player == null ? ItemStack.EMPTY : minecraft.player.getMainHandItem();
            else { var id = ResourceLocation.tryParse(draft.icon); icon = new ItemStack(id != null && BuiltInRegistries.ITEM.containsKey(id) ? BuiltInRegistries.ITEM.get(id) : Items.BARRIER); }
            g.renderItem(icon, left + 12, top + 40 + 6 * rowHeight);
        }
        var lines = font.split(feedback, panelWidth - 24);
        for (int i = 0; i < Math.min(2, lines.size()); i++) g.drawString(font, lines.get(i), left + 12, top + panelHeight - 54 + i * 10, 0xFFFFCC66);
        renderWidgets(g, x, y, tick);
    }
    @Override public void removed() {
        if (minecraft != null && minecraft.getConnection() != null) PacketDistributor.sendToServer(new EditorPayloads.Closed(data.session()));
    }
}

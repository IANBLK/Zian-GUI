package com.ianblk.ziangui.server;

import java.util.ArrayList;
import java.util.Arrays;

/** Pure mutations; every result passes the same validation as a disk configuration. */
public final class MenuEdits {
    private MenuEdits() {}
    public static MenuConfig.Menu apply(MenuConfig.Menu menu, String existingId, boolean delete,
            String id, String name, String icon, String command, String permissions, int position) {
        var actions = new ArrayList<>(menu.buttons());
        int index = -1;
        for (int i = 0; i < actions.size(); i++) if (actions.get(i).id().equals(existingId)) index = i;
        if (!existingId.isEmpty() && index < 0) throw new IllegalArgumentException("Botón inexistente");
        if (delete && index < 0) throw new IllegalArgumentException("Selecciona un botón para eliminar");
        if (index >= 0) actions.remove(index);
        if (!delete) {
            if (position < 1 || position > actions.size() + 1) throw new IllegalArgumentException("Posición entre 1 y " + (actions.size() + 1));
            String normalized = command.trim();
            if (normalized.startsWith("/")) normalized = normalized.substring(1);
            var nodes = permissions.isBlank() ? java.util.List.<String>of()
                : Arrays.stream(permissions.split(",", -1)).map(String::trim).toList();
            actions.add(position - 1, new MenuConfig.Action(id.trim(), name.trim(), icon, nodes, normalized));
        }
        return MenuConfig.parse(MenuConfig.json(new MenuConfig.Menu(menu.id(), menu.title(), menu.permission(), actions)));
    }
}

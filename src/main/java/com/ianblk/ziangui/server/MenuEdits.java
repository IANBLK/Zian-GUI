package com.ianblk.ziangui.server;

import java.util.ArrayList;
import java.util.Arrays;

/** Pure mutations; every result passes the same validation as a disk configuration. */
public final class MenuEdits {
    private MenuEdits() {}
    public static MenuConfig.Menu apply(MenuConfig.Menu menu, String existingId, boolean delete,
            String id, String name, String icon, String command, String permissions, int position) {
        if(id.isBlank()){
            String base=java.text.Normalizer.normalize(name,java.text.Normalizer.Form.NFD).replaceAll("\\p{M}", "").toLowerCase(java.util.Locale.ROOT).replaceAll("[^a-z0-9]+","_").replaceAll("^_|_$","");
            if(base.isBlank())base="boton";base=base.substring(0,Math.min(24,base.length()));String candidate=base;int suffix=1;
            var used=menu.buttons().stream().map(MenuConfig.Action::id).collect(java.util.stream.Collectors.toSet());while(used.contains(candidate)&&!candidate.equals(existingId))candidate=base+"_"+(suffix++);id=candidate;
        }
        var actions = new ArrayList<>(menu.buttons());
        String requirement=actions.stream().filter(a->a.id().equals(existingId)).map(MenuConfig.Action::requirement).findFirst().orElse("");
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
            actions.add(position - 1, new MenuConfig.Action(id.trim(), name.trim(), icon, nodes, normalized, requirement));
        }
        return MenuConfig.parse(MenuConfig.json(new MenuConfig.Menu(menu.id(), menu.title(), menu.permission(), actions)));
    }
}

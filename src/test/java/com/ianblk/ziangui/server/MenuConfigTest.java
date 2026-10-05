package com.ianblk.ziangui.server;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import java.nio.file.*;
import java.util.UUID;
import static org.junit.jupiter.api.Assertions.*;

class MenuConfigTest {
    @TempDir Path directory;
    private final String json = """
        {"id":"principal","title":"Prueba","permission":"zian.gui.menu.principal",
         "buttons":[{"id":"spawn","name":"Spawn","icon":"minecraft:compass",
          "permissions":["eternalcore.spawn"],"command":"spawn"}]}
        """;
    @Test void firstLoadCreatesNineActionsAndNeverOverwritesCustomMenus() throws Exception {
        var config = new MenuConfig(); config.reload(directory);
        assertEquals(9, config.get("principal").buttons().size());
        Files.writeString(directory.resolve("principal.json"), json);
        config.reload(directory);
        assertEquals("Prueba", config.get("principal").title());
        assertEquals(json, Files.readString(directory.resolve("principal.json")));
    }
    @Test void invalidReloadPreservesPreviousSnapshotAcrossAllFiles() throws Exception {
        Files.writeString(directory.resolve("principal.json"), json);
        var config = new MenuConfig(); config.reload(directory);
        var old = config.menus();
        Files.writeString(directory.resolve("principal.json"), json.replace("Prueba", "Nuevo"));
        Files.writeString(directory.resolve("bad.json"), "{broken");
        assertThrows(java.io.IOException.class, () -> config.reload(directory));
        assertSame(old, config.menus());
        assertEquals("Prueba", config.get("principal").title());
    }
    @Test void rejectsDuplicateFieldsAndUnknownPrivilegedActions() {
        assertThrows(IllegalArgumentException.class, () -> MenuConfig.parse(json.replace("\"title\":", "\"id\":\"otro\",\"title\":")));
        assertThrows(IllegalArgumentException.class, () -> MenuConfig.parse(json.replace("\"command\":", "\"as\":\"console\",\"command\":")));
        assertThrows(IllegalArgumentException.class, () -> MenuConfig.parse(json.replace("\"command\":\"spawn\"", "\"command\":\"spawn;op user\"")));
    }
    @Test void rejectsRepeatedButtonsAndMenus() throws Exception {
        var root = com.google.gson.JsonParser.parseString(json).getAsJsonObject();
        root.getAsJsonArray("buttons").add(root.getAsJsonArray("buttons").get(0).deepCopy());
        assertThrows(IllegalArgumentException.class, () -> MenuConfig.parse(root.toString()));
        Files.writeString(directory.resolve("principal.json"), json);
        Files.writeString(directory.resolve("copy.json"), json);
        assertThrows(java.io.IOException.class, () -> new MenuConfig().reload(directory));
    }
    @Test void boundsConfigSizeAndButtonCount() throws Exception {
        Files.writeString(directory.resolve("principal.json"), " ".repeat(65_537));
        assertThrows(java.io.IOException.class, () -> new MenuConfig().reload(directory));
        var root = com.google.gson.JsonParser.parseString(json).getAsJsonObject();
        var buttons = root.getAsJsonArray("buttons");
        for (int i=1; i<25; i++) {
            var button = buttons.get(0).deepCopy().getAsJsonObject();
            button.addProperty("id", "button" + i); buttons.add(button);
        }
        assertThrows(IllegalArgumentException.class, () -> MenuConfig.parse(root.toString()));
    }
    @Test void successfulReloadInvalidationRejectsOldClickWithoutClearingCooldown() {
        var gate = new SessionGate(); var player = UUID.randomUUID();
        gate.open(player, "principal", 77, 0); assertTrue(gate.action(player, 0));
        assertEquals(77L, gate.invalidate().get(player));
        assertFalse(gate.valid(player, "principal", 77, 1));
        gate.open(player, "principal", 88, 1);
        assertFalse(gate.action(player, 1));
    }
    @Test void defaultIntegrationCommandsOnlyOpenPlayerScreens() throws Exception {
        var config = new MenuConfig(); config.reload(directory);
        var buttons = config.get("principal").buttons();
        assertEquals("ZianUtilities gacha", buttons.stream().filter(b -> b.id().equals("gacha")).findFirst().orElseThrow().command());
        assertEquals("ZianUtilities quest", buttons.stream().filter(b -> b.id().equals("quests")).findFirst().orElseThrow().command());
        assertEquals("ZianGTS", buttons.stream().filter(b -> b.id().equals("gts")).findFirst().orElseThrow().command());
        assertEquals("medals", buttons.stream().filter(b -> b.id().equals("medals")).findFirst().orElseThrow().command());
    }
}

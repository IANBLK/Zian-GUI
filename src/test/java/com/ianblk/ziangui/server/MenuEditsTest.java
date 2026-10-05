package com.ianblk.ziangui.server;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import java.nio.file.*;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

class MenuEditsTest {
    @TempDir Path directory;
    private MenuConfig.Menu original() {
        return new MenuConfig.Menu("principal", "Menú", "zian.gui.menu.principal", List.of(
            new MenuConfig.Action("spawn", "Spawn", "minecraft:compass", List.of("eternalcore.spawn"), "spawn"),
            new MenuConfig.Action("heal", "Curar", "minecraft:apple", List.of("minecraft.command.healpokemon"), "healpokemon")));
    }
    @Test void addsAtRequestedPositionAndPreservesExistingPermissions() {
        var updated = MenuEdits.apply(original(), "", false, "pc", "Mi PC", "minecraft:chest", "/pc", "minecraft.command.pc,cobblemon.command.pc", 2);
        assertEquals(List.of("spawn", "pc", "heal"), updated.buttons().stream().map(MenuConfig.Action::id).toList());
        assertEquals("pc", updated.buttons().get(1).command());
        assertEquals(List.of("minecraft.command.pc", "cobblemon.command.pc"), updated.buttons().get(1).permissions());
        assertEquals(original().buttons().get(0), updated.buttons().get(0));
    }
    @Test void editsRenamesAndReordersWithoutExecutingCommands() {
        var updated = MenuEdits.apply(original(), "heal", false, "curar", "Curación", "minecraft:golden_apple", "healpokemon", "minecraft.command.healpokemon", 1);
        assertEquals(List.of("curar", "spawn"), updated.buttons().stream().map(MenuConfig.Action::id).toList());
        assertEquals("minecraft:golden_apple", updated.buttons().get(0).icon());
    }
    @Test void deletingEveryButtonKeepsValidEmptyMenu() {
        var one = MenuEdits.apply(original(), "spawn", true, "", "", "", "", "", 1);
        var empty = MenuEdits.apply(one, "heal", true, "", "", "", "", "", 1);
        assertTrue(MenuConfig.parse(MenuConfig.json(empty)).buttons().isEmpty());
    }
    @Test void rejectsUnknownTargetsDuplicatesInvalidPermissionsAndInjectedCommands() {
        assertThrows(IllegalArgumentException.class, () -> MenuEdits.apply(original(), "absent", true, "", "", "", "", "", 1));
        assertThrows(IllegalArgumentException.class, () -> MenuEdits.apply(original(), "heal", false, "spawn", "Test", "minecraft:apple", "spawn", "", 1));
        assertThrows(IllegalArgumentException.class, () -> MenuEdits.apply(original(), "heal", false, "heal", "Test", "minecraft:apple", "spawn;op test", "", 1));
        assertThrows(IllegalArgumentException.class, () -> MenuEdits.apply(original(), "heal", false, "heal", "Test", "minecraft:apple", "spawn", "*", 1));
        assertThrows(IllegalArgumentException.class, () -> MenuEdits.apply(original(), "heal", false, "heal", "Test", "minecraft:apple", "spawn", "", 3));
    }
    private MenuConfig load() throws Exception {
        Files.writeString(directory.resolve("custom-file.json"), MenuConfig.json(original()));
        var config = new MenuConfig(); config.reload(directory); return config;
    }
    @Test void saveUsesActualSourceFilenameAndMakesBackupThenSurvivesReload() throws Exception {
        var config = load(); String before = Files.readString(directory.resolve("custom-file.json"));
        var updated = MenuEdits.apply(original(), "spawn", true, "", "", "", "", "", 1);
        config.save(directory, config.revision(), updated);
        assertEquals(before, Files.readString(directory.resolve("custom-file.json.bak")));
        assertFalse(Files.exists(directory.resolve("principal.json")));
        var restarted = new MenuConfig(); restarted.reload(directory);
        assertEquals(updated, restarted.get("principal"));
        assertEquals(updated, config.get("principal"));
        try (var files = Files.list(directory)) { assertEquals(2, files.count()); }
    }
    @Test void staleAdminSnapshotCannotOverwriteLaterSave() throws Exception {
        var config = load(); String stale = config.revision();
        var first = MenuEdits.apply(original(), "spawn", true, "", "", "", "", "", 1);
        config.save(directory, stale, first);
        assertThrows(java.io.IOException.class, () -> config.save(directory, stale, original()));
        assertEquals(first, config.get("principal"));
        assertEquals(first, MenuConfig.parse(Files.readString(directory.resolve("custom-file.json"))));
    }
    @Test void externalChangesOrInvalidOtherMenuPreventSaving() throws Exception {
        var config = load(); String revision = config.revision();
        Files.writeString(directory.resolve("custom-file.json"), MenuConfig.json(original()).replace("Menú", "Externamente cambiado"));
        String external = Files.readString(directory.resolve("custom-file.json"));
        assertThrows(java.io.IOException.class, () -> config.save(directory, revision, original()));
        assertEquals(external, Files.readString(directory.resolve("custom-file.json")));
        assertEquals(original(), config.get("principal"));
        Files.writeString(directory.resolve("custom-file.json"), MenuConfig.json(original()));
        Files.writeString(directory.resolve("invalid.json"), "{broken");
        assertThrows(java.io.IOException.class, () -> config.save(directory, revision, original()));
        assertFalse(Files.exists(directory.resolve("custom-file.json.bak")));
    }
    @Test void invalidReplacementDoesNotWriteOrPublishIt() throws Exception {
        var config = load(); String before = Files.readString(directory.resolve("custom-file.json"));
        var invalid = new MenuConfig.Menu("principal", "Menú", "zian.gui.menu.principal", List.of(new MenuConfig.Action("bad", "Bad", "minecraft:apple", List.of(), "spawn;op test")));
        assertThrows(IllegalArgumentException.class, () -> config.save(directory, config.revision(), invalid));
        assertEquals(before, Files.readString(directory.resolve("custom-file.json")));
        assertEquals(original(), config.get("principal"));
    }
    @Test void deletedSourceDoesNotRegenerateDefaultsDuringSave() throws Exception {
        var config = load(); String revision = config.revision();
        Files.delete(directory.resolve("custom-file.json"));
        assertThrows(java.io.IOException.class, () -> config.save(directory, revision, original()));
        assertFalse(Files.exists(directory.resolve("principal.json")));
        assertEquals(original(), config.get("principal"));
    }
    @Test void invalidBackupPathPreservesCurrentFileAndSnapshot() throws Exception {
        var config = load(); String before = Files.readString(directory.resolve("custom-file.json"));
        Files.createDirectory(directory.resolve("custom-file.json.bak"));
        var next = MenuEdits.apply(original(), "spawn", true, "", "", "", "", "", 1);
        assertThrows(java.io.IOException.class, () -> config.save(directory, config.revision(), next));
        assertEquals(before, Files.readString(directory.resolve("custom-file.json")));
        assertEquals(original(), config.get("principal"));
    }
}

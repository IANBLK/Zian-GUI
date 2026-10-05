package com.ianblk.ziangui.server;

import org.junit.jupiter.api.Test;
import java.util.Set;
import static org.junit.jupiter.api.Assertions.*;

class MenuActionPermissionsTest {
    @Test void healRequiresBothSelfAndYouerWrapperPermissions() {
        var heal = MenuManager.ACTIONS.stream().filter(a -> a.id().equals("heal")).findFirst().orElseThrow();
        assertFalse(heal.allowed(Set.of("cobblemon.command.healpokemon.self")::contains));
        assertFalse(heal.allowed(Set.of("minecraft.command.healpokemon")::contains));
        assertFalse(heal.allowed(Set.of("cobblemon.command.healpokemon")::contains));
        assertTrue(heal.allowed(Set.of("cobblemon.command.healpokemon.self", "minecraft.command.healpokemon")::contains));
        assertEquals("healpokemon", heal.command()); // No target: only the player's own party.
    }
    @Test void spawnKeepsPluginPermissionWithoutMinecraftWrapperRequirement() {
        var spawn = MenuManager.ACTIONS.stream().filter(a -> a.id().equals("spawn")).findFirst().orElseThrow();
        assertTrue(spawn.allowed(Set.of("eternalcore.spawn")::contains));
        assertFalse(spawn.allowed(node -> false));
    }
}

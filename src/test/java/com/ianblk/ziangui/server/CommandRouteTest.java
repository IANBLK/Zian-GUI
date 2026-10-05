package com.ianblk.ziangui.server;

import org.junit.jupiter.api.Test;
import java.util.concurrent.atomic.AtomicInteger;
import static org.junit.jupiter.api.Assertions.*;

class CommandRouteTest {
    @Test void missingBukkitRegistrationUsesNativeOnce() {
        var nativeCalls = new AtomicInteger();
        assertTrue(CommandRoute.execute(false, () -> { fail("No Bukkit permission lookup"); return false; },
            () -> { fail("No Bukkit dispatch"); return false; },
            () -> { nativeCalls.incrementAndGet(); return true; }));
        assertEquals(1, nativeCalls.get());
    }
    @Test void missingBothRoutesFails() {
        assertFalse(CommandRoute.execute(false, () -> true, () -> true, () -> false));
    }
    @Test void bukkitPermissionDenialNeverTriesNative() {
        assertFalse(CommandRoute.execute(true, () -> false,
            () -> { fail("Permission denied"); return true; },
            () -> { fail("No fallback after denial"); return true; }));
    }
    @Test void falseBukkitResultNeverRetriesNative() {
        assertFalse(CommandRoute.execute(true, () -> true, () -> false,
            () -> { fail("No retry after attempted dispatch"); return true; }));
    }
    @Test void thrownBukkitDispatchNeverRetriesNative() {
        assertThrows(IllegalStateException.class, () -> CommandRoute.execute(true, () -> true,
            () -> { throw new IllegalStateException("Uncertain effect"); },
            () -> { fail("No retry after uncertain effect"); return true; }));
    }
    @Test void registeredBukkitCommandKeepsItsRoute() {
        assertTrue(CommandRoute.execute(true, () -> true, () -> true,
            () -> { fail("Plugin route takes priority"); return false; }));
    }
}

package com.ianblk.ziangui.server;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class PermissionServiceTest {
    @Test void explicitDenialCannotBeOverriddenByOperatorFallback() {
        assertFalse(PermissionService.evaluate(() -> false, () -> true));
        assertTrue(PermissionService.evaluate(() -> true, () -> false));
    }
    @Test void absentProviderUsesFallbackButBrokenProviderFailsClosed() {
        assertTrue(PermissionService.evaluate(() -> null, () -> true));
        assertFalse(PermissionService.evaluate(() -> null, () -> false));
        assertFalse(PermissionService.evaluate(() -> { throw new IllegalStateException("Unavailable provider"); }, () -> true));
        assertFalse(PermissionService.evaluate(() -> { throw new LinkageError("Missing API"); }, () -> true));
    }
    @Test void changedPermissionIsReadAgainInsteadOfCached() {
        var value = new java.util.concurrent.atomic.AtomicBoolean(true);
        assertTrue(PermissionService.evaluate(value::get, () -> false));
        value.set(false);
        assertFalse(PermissionService.evaluate(value::get, () -> true));
    }
}

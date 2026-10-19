package com.ianblk.ziangui.server;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class PermissionServiceTest {
    @Test void playerDefaultsDoNotRequireLuckPermsButAdministrationDoes(){
        assertTrue(PermissionService.defaultAllowed("zian.gui.open",false));
        assertTrue(PermissionService.defaultAllowed("zian.gui.menu.principal",false));
        assertTrue(PermissionService.defaultAllowed("ziangts.open",false));
        assertFalse(PermissionService.defaultAllowed("zian.gui.edit",false));
        assertFalse(PermissionService.defaultAllowed("zian.gui.menu.private",false));
        assertFalse(PermissionService.defaultAllowed("eternalcore.heal",false));
        assertTrue(PermissionService.defaultAllowed("zian.gui.edit",true));
    }
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

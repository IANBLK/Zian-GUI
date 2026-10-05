package com.ianblk.ziangui.server;

import org.junit.jupiter.api.Test;
import java.util.UUID;
import static org.junit.jupiter.api.Assertions.*;

class SessionGateTest {
    @Test void rejectsUnopenedWrongMenuOtherPlayersAndExpiredSessions() {
        var gate = new SessionGate(); var player = UUID.randomUUID();
        assertFalse(gate.valid(player, "principal", 7, 0));
        gate.open(player, "principal", 7, 0);
        assertTrue(gate.valid(player, "principal", 7, 299_999));
        assertFalse(gate.valid(player, "other", 7, 1));
        assertFalse(gate.valid(UUID.randomUUID(), "principal", 7, 1));
        assertFalse(gate.valid(player, "principal", 7, 300_000));
    }
    @Test void reopeningInvalidatesOldClicksAndOldCloseCannotCloseTheNewMenu() {
        var gate = new SessionGate(); var player = UUID.randomUUID();
        gate.open(player, "principal", 7, 0);
        gate.open(player, "principal", 8, 10);
        gate.close(player, 7);
        assertFalse(gate.valid(player, "principal", 7, 11));
        assertTrue(gate.valid(player, "principal", 8, 11));
        gate.close(player, 8);
        assertFalse(gate.valid(player, "principal", 8, 12));
    }
    @Test void reopeningDoesNotBypassActionCooldownAndLogoutClearsState() {
        var gate = new SessionGate(); var player = UUID.randomUUID();
        assertTrue(gate.request(player, 0));
        assertFalse(gate.request(player, 499));
        assertTrue(gate.request(player, 500));
        assertTrue(gate.action(player, 0));
        gate.open(player, "principal", 5, 50);
        assertFalse(gate.action(player, 999));
        assertTrue(gate.action(player, 1000));
        gate.remove(player);
        assertFalse(gate.valid(player, "principal", 5, 1001));
        assertTrue(gate.request(player, 1001));
        assertTrue(gate.action(player, 1001));
    }
}

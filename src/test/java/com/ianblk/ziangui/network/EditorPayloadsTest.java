package com.ianblk.ziangui.network;

import io.netty.buffer.Unpooled;
import net.minecraft.core.RegistryAccess;
import net.minecraft.network.RegistryFriendlyByteBuf;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class EditorPayloadsTest {
    @Test void editRequestRoundTripsWithServerHandFlagAndSession() {
        var p = new EditorPayloads.Change("principal", 72, "spawn", false, "spawn", "Spawn", "spawn", "eternalcore.spawn", 2, true);
        var buf = new RegistryFriendlyByteBuf(Unpooled.buffer(), RegistryAccess.EMPTY);
        try { EditorPayloads.Change.CODEC.encode(buf, p); assertEquals(p, EditorPayloads.Change.CODEC.decode(buf)); }
        finally { buf.release(); }
    }
    @Test void boundsAdminTextAndPositions() {
        assertThrows(IllegalArgumentException.class, () -> new EditorPayloads.Change("principal", 1, "", false, "id", "n", "x".repeat(257), "", 1, true));
        assertThrows(IllegalArgumentException.class, () -> new EditorPayloads.Change("principal", 1, "", false, "id", "n", "spawn\nop test", "", 1, true));
        assertThrows(IllegalArgumentException.class, () -> new EditorPayloads.Change("principal", 1, "", false, "id", "n", "spawn", "", 25, true));
        assertThrows(IllegalArgumentException.class, () -> new EditorPayloads.Open(1, "x".repeat(65537), ""));
    }
    @Test void rejectsOversizedCommandDuringDecoding() {
        var buf = new RegistryFriendlyByteBuf(Unpooled.buffer(), RegistryAccess.EMPTY);
        try {
            buf.writeUtf("principal"); buf.writeLong(1); buf.writeUtf(""); buf.writeBoolean(false); buf.writeUtf("id"); buf.writeUtf("Name"); buf.writeUtf("x".repeat(257));
            assertThrows(io.netty.handler.codec.DecoderException.class, () -> EditorPayloads.Change.CODEC.decode(buf));
        } finally { buf.release(); }
    }
}

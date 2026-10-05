package com.ianblk.ziangui.network;

import io.netty.buffer.Unpooled;
import net.minecraft.core.RegistryAccess;
import net.minecraft.network.RegistryFriendlyByteBuf;
import org.junit.jupiter.api.Test;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

class GuiPayloadsTest {
    @Test void menuViewRoundTripsWithoutServerCommandsOrPermissionNodes() {
        var p = new GuiPayloads.OpenMenu("principal", 123, "Menú principal",
            List.of(new GuiPayloads.ButtonView("spawn", "Spawn", "minecraft:compass", false)), false);
        var buf = new RegistryFriendlyByteBuf(Unpooled.buffer(), RegistryAccess.EMPTY);
        try {
            GuiPayloads.OpenMenu.CODEC.encode(buf, p);
            assertEquals(p, GuiPayloads.OpenMenu.CODEC.decode(buf));
        } finally { buf.release(); }
    }
    @Test void rejectsCommandInjectionAndOversizedIdentifiers() {
        assertThrows(IllegalArgumentException.class, () -> new GuiPayloads.Click("principal", "spawn;op player", 1));
        assertThrows(IllegalArgumentException.class, () -> new GuiPayloads.OpenRequest("x".repeat(33)));
        assertThrows(IllegalArgumentException.class, () -> new GuiPayloads.Feedback(1, "x".repeat(257)));
    }
    @Test void decoderRejectsOversizedButtonListBeforeAllocating() {
        var buf = new RegistryFriendlyByteBuf(Unpooled.buffer(), RegistryAccess.EMPTY);
        try {
            buf.writeUtf("principal", 32); buf.writeLong(1); buf.writeUtf("Principal", 64); buf.writeVarInt(Integer.MAX_VALUE);
            assertThrows(IllegalArgumentException.class, () -> GuiPayloads.OpenMenu.CODEC.decode(buf));
        } finally { buf.release(); }
    }
}

package com.ianblk.ziangui.server;

import java.util.function.BooleanSupplier;

/** Pick a single route before execution. A denial or uncertain result never switches routes. */
final class CommandRoute {
    private CommandRoute() {}
    static boolean execute(boolean registeredBukkit, BooleanSupplier permission,
                           BooleanSupplier bukkit, BooleanSupplier nativeCommand) {
        if (!registeredBukkit) return nativeCommand.getAsBoolean();
        if (!permission.getAsBoolean()) return false;
        return bukkit.getAsBoolean();
    }
}

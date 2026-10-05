package com.ianblk.ziangui.server;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/** Accessed on the server thread only. A fresh nonce invalidates older screen clicks. */
public final class SessionGate {
    private record Session(String menu, long nonce, long expires) {}
    private final Map<UUID, Session> sessions = new HashMap<>();
    private final Map<UUID, Long> nextRequest = new HashMap<>();
    private final Map<UUID, Long> nextAction = new HashMap<>();
    public boolean request(UUID player, long now) {
        if (now < nextRequest.getOrDefault(player, Long.MIN_VALUE)) return false;
        nextRequest.put(player, now + 500); return true;
    }
    public void open(UUID player, String menu, long nonce, long now) {
        sessions.put(player, new Session(menu, nonce, now + 300_000));
    }
    public boolean valid(UUID player, String menu, long nonce, long now) {
        var s = sessions.get(player);
        return s != null && s.menu.equals(menu) && s.nonce == nonce && now < s.expires;
    }
    public boolean action(UUID player, long now) {
        if (now < nextAction.getOrDefault(player, Long.MIN_VALUE)) return false;
        nextAction.put(player, now + 1000); return true;
    }
    public void close(UUID player, long nonce) {
        var s = sessions.get(player);
        if (s != null && s.nonce == nonce) sessions.remove(player);
    }
    public void remove(UUID player) {
        sessions.remove(player); nextRequest.remove(player); nextAction.remove(player);
    }
    public void clear() { sessions.clear(); nextRequest.clear(); nextAction.clear(); }
}

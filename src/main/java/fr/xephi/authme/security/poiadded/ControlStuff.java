package fr.xephi.authme.security.poiadded;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public final class ControlStuff {

    private static volatile boolean lockdown = false;
    private static volatile boolean muteAll = false;
    private static final Set<UUID> frozen = Collections.synchronizedSet(new HashSet<>());

    private ControlStuff() {}

    public static boolean isLockdown() {
        return lockdown;
    }

    public static void setLockdown(boolean value) {
        lockdown = value;
    }

    public static boolean isMuteAll() {
        return muteAll;
    }

    public static void setMuteAll(boolean value) {
        muteAll = value;
    }

    public static void freeze(UUID uuid) {
        frozen.add(uuid);
    }

    public static void unfreeze(UUID uuid) {
        frozen.remove(uuid);
    }

    public static boolean isFrozen(UUID uuid) {
        return frozen.contains(uuid);
    }

    public static void freezeAll() {
        for (Player p : Bukkit.getOnlinePlayers()) {
            frozen.add(p.getUniqueId());
        }
    }

    public static void unfreezeAll() {
        frozen.clear();
    }
}

package fr.xephi.authme.security.poiadded;

import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Persistent allowlist for bypassing "console-only" restrictions on certain security commands.
 *
 * Stored as UUIDs in plugins/PLUGIN_NAME/trusted.yml:
 * trusted:
 * - "uuid"
 */
public final class TrustedPlayers {

    private static final String FILE_NAME = "trusted.yml";
    private static final String KEY_TRUSTED = "trusted";

    private static final Set<UUID> TRUSTED = Collections.synchronizedSet(new HashSet<>());
    private static volatile File file;

    private TrustedPlayers() {
    }

    public static void init(File dataFolder) {
        file = new File(dataFolder, FILE_NAME);
        load();
    }

    public static boolean isTrusted(UUID uuid) {
        return TRUSTED.contains(uuid);
    }

    /** @return true if newly trusted, false if was already trusted */
    public static boolean trust(UUID uuid) {
        return TRUSTED.add(uuid);
    }

    /** @return true if removed, false if wasn't trusted */
    public static boolean untrust(UUID uuid) {
        return TRUSTED.remove(uuid);
    }

    public static void load() {
        File f = file;
        if (f == null) {
            return;
        }
        if (!f.exists()) {
            return;
        }
        YamlConfiguration cfg = YamlConfiguration.loadConfiguration(f);
        List<String> raw = cfg.getStringList(KEY_TRUSTED);
        Set<UUID> loaded = new HashSet<>();
        for (String s : raw) {
            try {
                loaded.add(UUID.fromString(s));
            } catch (IllegalArgumentException ignore) {
                // Ignore malformed UUIDs to avoid breaking startup.
            }
        }
        TRUSTED.clear();
        TRUSTED.addAll(loaded);
    }

    public static void save() {
        File f = file;
        if (f == null) {
            return;
        }
        if (!f.getParentFile().exists()) {
            //noinspection ResultOfMethodCallIgnored
            f.getParentFile().mkdirs();
        }

        YamlConfiguration cfg = new YamlConfiguration();
        cfg.set(KEY_TRUSTED, TRUSTED.stream().map(UUID::toString).sorted().collect(Collectors.toList()));
        try {
            cfg.save(f);
        } catch (IOException ignore) {
            // Best-effort persistence; command gating still works for the current runtime.
        }
    }
}

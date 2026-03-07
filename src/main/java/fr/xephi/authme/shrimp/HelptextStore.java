package fr.xephi.authme.shrimp;

import fr.xephi.authme.ConsoleLogger;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;

public final class HelptextStore {

    private static volatile String latestHelptext = null;
    private static YamlConfiguration config;
    private static File configFile;
    private static ConsoleLogger logger;

    private HelptextStore() {}

    public static void init(YamlConfiguration cfg, File file, ConsoleLogger log) {
        config = cfg;
        configFile = file;
        logger = log;
        latestHelptext = config.getString("latestHelptext", null);
    }

    public static void set(String text) {
        latestHelptext = text;
        if (config != null && configFile != null) {
            config.set("latestHelptext", text);
            try {
                config.save(configFile);
            } catch (IOException e) {
                if (logger != null) {
                    logger.logException("Could not save helptext to shrimpbot.yml", e);
                }
            }
        }
    }

    public static boolean hasHelptext() {
        return latestHelptext != null && !latestHelptext.isEmpty();
    }

    public static String text() {
        return latestHelptext;
    }
}

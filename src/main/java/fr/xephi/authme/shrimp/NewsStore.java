package fr.xephi.authme.shrimp;

import fr.xephi.authme.ConsoleLogger;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;

public final class NewsStore {
    private static volatile String latestTopic = null;
    private static volatile String latestNews = null;
    private static YamlConfiguration config;
    private static File configFile;
    private static ConsoleLogger logger;

    private NewsStore() {}

    public static void init(YamlConfiguration cfg, File file, ConsoleLogger log) {
        config = cfg;
        configFile = file;
        logger = log;
        latestTopic = config.getString("latestTopic", null);
        latestNews = config.getString("latestNews", null);
    }

    public static void set(String topic, String news) {
        latestTopic = topic;
        latestNews = news;
        if (config != null && configFile != null) {
            config.set("latestTopic", topic);
            config.set("latestNews", news);
            try {
                config.save(configFile);
            } catch (IOException e) {
                if (logger != null) {
                    logger.logException("Could not save news to shrimpbot.yml", e);
                }
            }
        }
    }

    public static boolean hasNews() {
        return latestTopic != null && latestNews != null && !latestTopic.isEmpty() && !latestNews.isEmpty();
    }

    public static String topic() {
        return latestTopic;
    }

    public static String news() {
        return latestNews;
    }
}

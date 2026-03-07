package fr.xephi.authme.shrimp.handlers;

import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;

public final class NewsHandler {

    private NewsHandler() {}

    public static void handleSlashNews(SlashCommandInteractionEvent event, String topic, String news) {
        fr.xephi.authme.shrimp.NewsStore.set(topic, news);
        String reply = "Saved news. Topic: " + topic + (news.isEmpty() ? "" : " | News: " + news);
        event.reply(reply).setEphemeral(true).queue();
        org.bukkit.Bukkit.getScheduler().runTask(org.bukkit.Bukkit.getPluginManager().getPlugin("AuthMe"), () -> {
            org.bukkit.Sound sound = org.bukkit.Sound.BLOCK_NOTE_BLOCK_CHIME;
            org.bukkit.Bukkit.getOnlinePlayers().forEach(p -> {
                p.sendMessage(org.bukkit.ChatColor.GOLD + "[News] " + org.bukkit.ChatColor.YELLOW + topic);
                if (!news.isEmpty()) {
                    p.sendMessage(org.bukkit.ChatColor.WHITE + news);
                }
                p.playSound(p.getLocation(), sound, 1.0f, 1.0f);
            });
        });
    }
}

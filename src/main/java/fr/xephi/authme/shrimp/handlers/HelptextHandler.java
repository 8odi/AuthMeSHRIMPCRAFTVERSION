package fr.xephi.authme.shrimp.handlers;

import fr.xephi.authme.shrimp.HelptextStore;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Sound;
import org.bukkit.plugin.Plugin;

public final class HelptextHandler {

    private HelptextHandler() {}

    public static void handleSlashHelptext(SlashCommandInteractionEvent event, String text) {
        HelptextStore.set(text);
        event.reply("Saved helptext.").setEphemeral(true).queue();
        Plugin plugin = Bukkit.getPluginManager().getPlugin("AuthMe");
        if (plugin == null) {
            return;
        }
        Bukkit.getScheduler().runTask(plugin, () -> {
            Bukkit.getOnlinePlayers().forEach(p -> {
                p.sendMessage(ChatColor.AQUA + "[Help] " + ChatColor.WHITE + text);
                p.playSound(p.getLocation(), Sound.BLOCK_NOTE_BLOCK_CHIME, 1.0f, 1.0f);
            });
        });
    }
}

package fr.xephi.authme.security.poiadded;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.UUID;

public final class Trust {

    private Trust() {
    }

    public static boolean handle(CommandSender sender, String[] args) {

        if (sender instanceof Player) {
            sender.sendMessage(ChatColor.RED + "Ur not cool enough to run this command");
            return true;
        }
        if (args.length != 1) {
            sender.sendMessage(ChatColor.YELLOW + "Usage: /trust <player>");
            return true;
        }

        String targetName = args[0];
        OfflinePlayer offline = Bukkit.getOfflinePlayer(targetName);
        if (offline == null || offline.getUniqueId() == null) {
            sender.sendMessage(ChatColor.RED + "Player not found: " + targetName);
            return true;
        }
        if (!offline.isOnline() && !offline.hasPlayedBefore()) {
            sender.sendMessage(ChatColor.RED + "Player not found:" + targetName);
            return true;
        }

        UUID targetUuid = offline.getUniqueId();
        boolean wasTrusted = TrustedPlayers.isTrusted(targetUuid);
        if (wasTrusted) {
            TrustedPlayers.untrust(targetUuid);
            TrustedPlayers.save();
            sender.sendMessage(ChatColor.YELLOW + "Untrusted " + offline.getName());
        } else {
            TrustedPlayers.trust(targetUuid);
            TrustedPlayers.save();
            sender.sendMessage(ChatColor.GREEN + "Trusted " + offline.getName());
        }
        return true;
    }
}

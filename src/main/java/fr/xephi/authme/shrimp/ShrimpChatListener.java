package fr.xephi.authme.shrimp;

import fr.xephi.authme.ConsoleLogger;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;

public class ShrimpChatListener implements Listener {

    private final ShrimpBotService service;
    private final ConsoleLogger logger;

    public ShrimpChatListener(ShrimpBotService service, ConsoleLogger logger) {
        this.service = service;
        this.logger = logger;
    }

    @EventHandler
    public void onChat(AsyncPlayerChatEvent event) {
        if (!service.isActive()) return;
        if (!service.hasSession(event.getPlayer().getUniqueId())) return;
        event.setCancelled(true);
        String message = event.getMessage();
        service.forwardPlayerMessage(event.getPlayer(), message);
        event.getPlayer().sendMessage(org.bukkit.ChatColor.YELLOW + "[To Staff] " + org.bukkit.ChatColor.WHITE + message);
    }
}

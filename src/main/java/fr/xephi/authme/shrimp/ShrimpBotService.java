package fr.xephi.authme.shrimp;

import club.minnced.discord.webhook.WebhookClient;
import club.minnced.discord.webhook.send.WebhookMessageBuilder;
import fr.xephi.authme.ConsoleLogger;
import fr.xephi.authme.security.poiadded.ControlStuff;
import fr.xephi.authme.security.poiadded.TrustedPlayers;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.entities.channel.attribute.IPermissionContainer;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.requests.GatewayIntent;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class ShrimpBotService {

    private final org.bukkit.plugin.Plugin plugin;
    private final ConsoleLogger logger;
    private final YamlConfiguration config;

    private JDA jda;
    private Guild guild;
    private String modRoleId;
    private boolean active;

    private final Map<UUID, Session> sessionsByPlayer = new ConcurrentHashMap<>();
    private final Map<Long, UUID> sessionsByChannel = new ConcurrentHashMap<>();

    public ShrimpBotService(org.bukkit.plugin.Plugin plugin, ConsoleLogger logger, YamlConfiguration config) {
        this.plugin = plugin;
        this.logger = logger;
        this.config = config;
    }

    public boolean isActive() {
        return active;
    }

    public void start() {
        String token = config.getString("token", "");
        String guildId = config.getString("guildId", "");
        modRoleId = config.getString("modRoleId", "");
        if (token.isEmpty() || guildId.isEmpty() || modRoleId.isEmpty()) {
            logger.info("[ShrimpBot] token/guildId/modRoleId missing; skipping startup.");
            return;
        }
        try {
            jda = JDABuilder.createDefault(token)
                .enableIntents(GatewayIntent.GUILD_MESSAGES, GatewayIntent.MESSAGE_CONTENT)
                .addEventListeners(new DiscordListener())
                .build();
            jda.awaitReady();
            guild = jda.getGuildById(guildId);
            if (guild == null) {
                logger.warning("[ShrimpBot] Guild not found: " + guildId);
                return;
            }
            guild.updateCommands().addCommands(
                Commands.slash("done", "Close the current session"),
                Commands.slash("news", "Shrimpcraft news").addOptions(
                    new OptionData(OptionType.STRING, "topic", "the big title", true),
                    new OptionData(OptionType.STRING, "thenews", "the news", true)
                ),
                Commands.slash("helptext", "Shrimpcraft help text")
                    .addOptions(new OptionData(OptionType.STRING, "text", "help text to show", true)),
                Commands.slash("lockdown", "Toggle lockdown").addOptions(
                    new OptionData(OptionType.BOOLEAN, "enable", "true to enable, false to disable", true)
                ),
                Commands.slash("muteall", "Toggle mute-all").addOptions(
                    new OptionData(OptionType.BOOLEAN, "enable", "true to enable, false to disable", true)
                ),
                Commands.slash("kickall", "Kick everyone"),
                Commands.slash("freeze", "Freeze a player or everyone").addOptions(
                    new OptionData(OptionType.STRING, "target", "player name or @a", true)
                ),
                Commands.slash("unfreeze", "Unfreeze a player or everyone").addOptions(
                    new OptionData(OptionType.STRING, "target", "player name or @a", true)
                ),
                Commands.slash("trust", "Make a player status between OP and Console basically").addOptions(
                    new OptionData(OptionType.STRING, "player", "Minecraft player name", true)
                )
            ).queue();
            active = true;
            logger.info("[ShrimpBot] Connected and ready (:");
        } catch (InterruptedException e) {
            logger.logException("[ShrimpBot] Failed to start JDA ):", e);
        }
    }

    public void shutdown() {
        if (jda != null) {
            jda.shutdown();
        }
    }

    public void startSession(Player player) {
        if (!active) {
            player.sendMessage("ShrimpBot not configured.");
            return;
        }
        if (sessionsByPlayer.containsKey(player.getUniqueId())) {
            player.sendMessage("You already have an active session.");
            return;
        }
        Role modRole = guild.getRoleById(modRoleId);
        if (modRole == null) {
            player.sendMessage("mcmod role not found ):");
            return;
        }
        String channelName = "session-" + player.getUniqueId().toString().substring(0, 8);
        TextChannel channel = guild.createTextChannel(channelName).complete();
        // permissions
        channel.upsertPermissionOverride(guild.getPublicRole())
            .deny(Permission.VIEW_CHANNEL, Permission.MESSAGE_SEND).complete();
        channel.upsertPermissionOverride(modRole)
            .grant(Permission.VIEW_CHANNEL, Permission.MESSAGE_SEND).complete();
        channel.upsertPermissionOverride(guild.getSelfMember())
            .grant(Permission.VIEW_CHANNEL, Permission.MESSAGE_SEND, Permission.MANAGE_CHANNEL).complete();

        String webhookUrl = channel.createWebhook("MC-" + player.getName()).complete().getUrl();

        Session session = new Session(player.getUniqueId(), channel.getIdLong(), webhookUrl);
        sessionsByPlayer.put(player.getUniqueId(), session);
        sessionsByChannel.put(channel.getIdLong(), player.getUniqueId());

        player.sendMessage("Opened session channel: #" + channelName);
        channel.sendMessage("Session opened for player " + player.getName() + " at " + Instant.now()).queue();
    }

    public void endSessionByPlayer(UUID playerId, boolean notifyPlayer) {
        Session session = sessionsByPlayer.remove(playerId);
        if (session == null) {
            if (notifyPlayer) {
                Player p = Bukkit.getPlayer(playerId);
                if (p != null) p.sendMessage("No active session.");
            }
            return;
        }
        sessionsByChannel.remove(session.getChannelId());
        deleteChannel(session.getChannelId());
        if (notifyPlayer) {
            Player p = Bukkit.getPlayer(playerId);
            if (p != null) p.sendMessage("Session closed.");
        }
    }

    public void endSessionByChannel(long channelId) {
        UUID playerId = sessionsByChannel.remove(channelId);
        if (playerId != null) {
            sessionsByPlayer.remove(playerId);
            Player p = Bukkit.getPlayer(playerId);
            if (p != null) {
                p.sendMessage("Session closed.");
            }
        }
        deleteChannel(channelId);
    }

    private void deleteChannel(long channelId) {
        TextChannel channel = guild.getTextChannelById(channelId);
        if (channel != null) {
            channel.delete().queue();
        }
    }

    public boolean hasSession(UUID playerId) {
        return sessionsByPlayer.containsKey(playerId);
    }

    public void forwardPlayerMessage(Player player, String message) {
        Session session = sessionsByPlayer.get(player.getUniqueId());
        if (session == null) return;
        WebhookClient client = session.getWebhook();
        if (client == null) return;
        WebhookMessageBuilder builder = new WebhookMessageBuilder();
        builder.setUsername(player.getName());
        builder.setContent(message);
        client.send(builder.build());
    }

    private class DiscordListener extends ListenerAdapter {
        @Override
        public void onMessageReceived(@NotNull MessageReceivedEvent event) {
            if (!active) return;
            long channelId = event.getChannel().getIdLong();
            if (!sessionsByChannel.containsKey(channelId)) return;
            if (event.getAuthor().isBot()) return;
            String content = event.getMessage().getContentDisplay();
            if (content.equalsIgnoreCase("/done")) {
                if (!hasModRole(event.getMember())) {
                    event.getMessage().reply("You need the mcmod role to close sessions.").queue();
                    return;
                }
                endSessionByChannel(channelId);
                return;
            }
            UUID playerId = sessionsByChannel.get(channelId);
            if (playerId == null) return;
            Player player = Bukkit.getPlayer(playerId);
            if (player != null && player.isOnline()) {
                String displayName = event.isWebhookMessage()
                    ? event.getAuthor().getName()
                    : event.getMember() != null ? event.getMember().getEffectiveName() : event.getAuthor().getName();
                String msg = org.bukkit.ChatColor.AQUA + "[Admin] " + org.bukkit.ChatColor.WHITE + displayName + ": " + content;
                Bukkit.getScheduler().runTask(plugin, () -> player.sendMessage(msg));
            }
        }

        @Override
        public void onSlashCommandInteraction(@NotNull SlashCommandInteractionEvent event) {
            if (!active) return;
            String name = event.getName();
            if (!hasModRole(event.getMember())) {
                event.reply("You need the mcmod role to run this command.").setEphemeral(true).queue();
                return;
            }
            if ("done".equalsIgnoreCase(name)) {
                long channelId = event.getChannel().getIdLong();
                if (!sessionsByChannel.containsKey(channelId)) {
                    event.reply("This channel is not a session.").setEphemeral(true).queue();
                    return;
                }
                event.reply("Closing session…").setEphemeral(true).queue();
                endSessionByChannel(channelId);
            } else if ("news".equalsIgnoreCase(name)) {
                String topic = event.getOption("topic") != null ? event.getOption("topic").getAsString() : "general";
                String news = event.getOption("thenews") != null ? event.getOption("thenews").getAsString() : "";
                fr.xephi.authme.shrimp.handlers.NewsHandler.handleSlashNews(event, topic, news);
            } else if ("helptext".equalsIgnoreCase(name)) {
                String text = event.getOption("text") != null ? event.getOption("text").getAsString() : "";
                fr.xephi.authme.shrimp.handlers.HelptextHandler.handleSlashHelptext(event, text);
            } else if ("lockdown".equalsIgnoreCase(name)) {
                boolean enable = event.getOption("enable").getAsBoolean();
                ControlStuff.setLockdown(enable);
                event.reply("Lockdown " + (enable ? "enabled" : "disabled")).setEphemeral(true).queue();
            } else if ("muteall".equalsIgnoreCase(name)) {
                boolean enable = event.getOption("enable").getAsBoolean();
                ControlStuff.setMuteAll(enable);
                event.reply("MuteAll " + (enable ? "enabled" : "disabled")).setEphemeral(true).queue();
            } else if ("kickall".equalsIgnoreCase(name)) {
                Bukkit.getScheduler().runTask(plugin, () -> Bukkit.getOnlinePlayers()
                    .forEach(p -> p.kickPlayer(org.bukkit.ChatColor.RED + "Kicked by staff.")));
                event.reply("Everyone kicked.").setEphemeral(true).queue();
            } else if ("freeze".equalsIgnoreCase(name)) {
                String target = event.getOption("target").getAsString();
                handleFreezeCommand(target, true);
                event.reply("Freeze applied to " + target).setEphemeral(true).queue();
            } else if ("unfreeze".equalsIgnoreCase(name)) {
                String target = event.getOption("target").getAsString();
                handleFreezeCommand(target, false);
                event.reply("Unfreeze applied to " + target).setEphemeral(true).queue();
            } else if ("trust".equalsIgnoreCase(name)) {
                String targetName = event.getOption("player").getAsString();
                event.deferReply(true).queue();
                Bukkit.getScheduler().runTask(plugin, () -> {
                    String msg = toggleTrust(targetName);
                    event.getHook().editOriginal(msg).queue();
                });
            }
        }
    }

    private String toggleTrust(String targetName) {
        OfflinePlayer offline = Bukkit.getOfflinePlayer(targetName);
        if (offline == null || offline.getUniqueId() == null) {
            return "Player not found: " + targetName;
        }
        if (!offline.isOnline() && !offline.hasPlayedBefore()) {
            return "Player not found: " + targetName;
        }

        boolean wasTrusted = TrustedPlayers.isTrusted(offline.getUniqueId());
        if (wasTrusted) {
            TrustedPlayers.untrust(offline.getUniqueId());
            TrustedPlayers.save();
            return "Untrusted " + offline.getName();
        } else {
            TrustedPlayers.trust(offline.getUniqueId());
            TrustedPlayers.save();
            return "Trusted " + offline.getName();
        }
    }

    private void handleFreezeCommand(String target, boolean freeze) {
        Bukkit.getScheduler().runTask(plugin, () -> {
            if ("@a".equalsIgnoreCase(target)) {
                if (freeze) {
                    ControlStuff.freezeAll();
                } else {
                    ControlStuff.unfreezeAll();
                }
                return;
            }
            Player p = Bukkit.getPlayerExact(target);
            if (p == null) {
                return;
            }
            if (freeze) {
                ControlStuff.freeze(p.getUniqueId());
                p.sendMessage(org.bukkit.ChatColor.RED + "You have been frozen.");
            } else {
                ControlStuff.unfreeze(p.getUniqueId());
                p.sendMessage(org.bukkit.ChatColor.GREEN + "You have been unfrozen.");
            }
        });
    }

    private boolean hasModRole(Member member) {
        if (member == null) {
            return false;
        }
        Role modRole = guild.getRoleById(modRoleId);
        return modRole != null && member.getRoles().stream().anyMatch(r -> r.getId().equals(modRoleId));
    }

    private static class Session {
        private final UUID playerId;
        private final long channelId;
        private final String webhookUrl;
        private WebhookClient webhookClient;

        Session(UUID playerId, long channelId, String webhookUrl) {
            this.playerId = playerId;
            this.channelId = channelId;
            this.webhookUrl = webhookUrl;
            this.webhookClient = WebhookClient.withUrl(webhookUrl);
        }

        public long getChannelId() {
            return channelId;
        }

        public WebhookClient getWebhook() {
            return webhookClient;
        }
    }
}

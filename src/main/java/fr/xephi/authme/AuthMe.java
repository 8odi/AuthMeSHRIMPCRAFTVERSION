package fr.xephi.authme;

import ch.jalu.injector.Injector;
import ch.jalu.injector.InjectorBuilder;
import com.google.common.annotations.VisibleForTesting;
import fr.xephi.authme.api.v3.AuthMeApi;
import fr.xephi.authme.command.CommandHandler;
import fr.xephi.authme.datasource.DataSource;
import fr.xephi.authme.initialization.DataFolder;
import fr.xephi.authme.initialization.DataSourceProvider;
import fr.xephi.authme.initialization.OnShutdownPlayerSaver;
import fr.xephi.authme.initialization.OnStartupTasks;
import fr.xephi.authme.initialization.SettingsProvider;
import fr.xephi.authme.initialization.TaskCloser;
import fr.xephi.authme.listener.BlockListener;
import fr.xephi.authme.listener.EntityListener;
import fr.xephi.authme.listener.PlayerListener;
import fr.xephi.authme.listener.PlayerListener111;
import fr.xephi.authme.listener.PlayerListener19;
import fr.xephi.authme.listener.PlayerListener19Spigot;
import fr.xephi.authme.listener.ServerListener;
import fr.xephi.authme.output.ConsoleLoggerFactory;
import fr.xephi.authme.security.crypts.Sha256;
import fr.xephi.authme.service.BackupService;
import fr.xephi.authme.service.BukkitService;
import fr.xephi.authme.service.MigrationService;
import fr.xephi.authme.service.bungeecord.BungeeReceiver;
import fr.xephi.authme.service.yaml.YamlParseException;
import fr.xephi.authme.settings.Settings;
import fr.xephi.authme.settings.SettingsWarner;
import fr.xephi.authme.settings.properties.SecuritySettings;
import fr.xephi.authme.security.poiadded.ControlStuff;
import fr.xephi.authme.security.poiadded.Trust;
import fr.xephi.authme.security.poiadded.TrustedPlayers;
import fr.xephi.authme.task.CleanupTask;
import fr.xephi.authme.task.purge.PurgeService;
import fr.xephi.authme.util.ExceptionUtils;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Server;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.PluginDescriptionFile;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.plugin.java.JavaPluginLoader;
import org.bukkit.scheduler.BukkitScheduler;

import java.io.File;
import java.io.IOException;
import java.util.Locale;
import java.util.function.Consumer;

import static fr.xephi.authme.service.BukkitService.TICKS_PER_MINUTE;
import static fr.xephi.authme.util.Utils.isClassLoaded;

/**
 * The AuthMe main class.
 */
public class AuthMe extends JavaPlugin {

    // Constants
    private static final String PLUGIN_NAME = "authMeSHRIMPCRAFT";
    private static final String LOG_FILENAME = "authme.log";
    private static final int CLEANUP_INTERVAL = 5 * TICKS_PER_MINUTE;

    // Version and build number values
    private static String pluginVersion = "N/D";
    private static String pluginBuildNumber = "Unknown";

    // Private instances
    private CommandHandler commandHandler;
    private Settings settings;
    private DataSource database;
    private BukkitService bukkitService;
    private Injector injector;
    private BackupService backupService;
    private ConsoleLogger logger;
    private YamlConfiguration shrimpConfig;
    private File shrimpConfigFile;
    private fr.xephi.authme.shrimp.ShrimpBotService shrimpBotService;

    /**
     * Constructor.
     */
    public AuthMe() {
    }

    /*
     * Constructor for unit testing.
     */
    @VisibleForTesting
    AuthMe(JavaPluginLoader loader, PluginDescriptionFile description, File dataFolder, File file) {
        super(loader, description, dataFolder, file);
    }

    /**
     * Get the plugin's name.
     *
     * @return The plugin's name.
     */
    public static String getPluginName() {
        return PLUGIN_NAME;
    }

    /**
     * Get the plugin's version.
     *
     * @return The plugin's version.
     */
    public static String getPluginVersion() {
        return pluginVersion;
    }

    /**
     * Get the plugin's build number.
     *
     * @return The plugin's build number.
     */
    public static String getPluginBuildNumber() {
        return pluginBuildNumber;
    }

    /**
     * Method called when the server enables the plugin.
     */
    @Override
    public void onEnable() {
        // Load the plugin version data from the plugin description file
        loadPluginInfo(getDescription().getVersion());

        // Set the Logger instance and log file path
        ConsoleLogger.initialize(getLogger(), new File(getDataFolder(), LOG_FILENAME));
        logger = ConsoleLoggerFactory.get(AuthMe.class);

        // Check server version
        if (!isClassLoaded("org.spigotmc.event.player.PlayerSpawnLocationEvent")
            || !isClassLoaded("org.bukkit.event.player.PlayerInteractAtEntityEvent")) {
            logger.warning("You are running an unsupported server version (" + getServerNameVersionSafe() + "). "
                + "AuthMe requires Spigot 1.8.X or later!");
            stopOrUnload();
            return;
        }

        // Prevent running AuthMeBridge due to major exploit issues
        if (getServer().getPluginManager().isPluginEnabled("AuthMeBridge")) {
            logger.warning("Detected AuthMeBridge, support for it has been dropped as it was "
                + "causing exploit issues, please use AuthMeBungee instead! Aborting!");
            stopOrUnload();
            return;
        }

        // Initialize the plugin
        try {
            initialize();
        } catch (Throwable th) {
            YamlParseException yamlParseException = ExceptionUtils.findThrowableInCause(YamlParseException.class, th);
            if (yamlParseException == null) {
                logger.logException("Aborting initialization of AuthMe:", th);
                th.printStackTrace();
            } else {
                logger.logException("File '" + yamlParseException.getFile() + "' contains invalid YAML. "
                    + "Please run its contents through http://yamllint.com", yamlParseException);
            }
            stopOrUnload();
            return;
        }

        // Show settings warnings
        injector.getSingleton(SettingsWarner.class).logWarningsForMisconfigurations();

        // Schedule clean up task
        CleanupTask cleanupTask = injector.getSingleton(CleanupTask.class);
        cleanupTask.runTaskTimerAsynchronously(this, CLEANUP_INTERVAL, CLEANUP_INTERVAL);

        // Do a backup on start
        backupService.doBackup(BackupService.BackupCause.START);

        // Set up Metrics
        OnStartupTasks.sendMetrics(this, settings);

        // Successful message
        logger.info("AuthMe  SHRIMPCRAFT AWESOME EDITION " + getPluginVersion() + " build n." + getPluginBuildNumber() + " successfully enabled WITH MILADY SECURITY");

        // Purge on start if enabled
        PurgeService purgeService = injector.getSingleton(PurgeService.class);
        purgeService.runAutoPurge();
    }

    /**
     * Load the version and build number of the plugin from the description file.
     *
     * @param versionRaw the version as given by the plugin description file
     */
    private static void loadPluginInfo(String versionRaw) {
        int index = versionRaw.lastIndexOf("-");
        if (index != -1) {
            pluginVersion = versionRaw.substring(0, index);
            pluginBuildNumber = versionRaw.substring(index + 1);
            if (pluginBuildNumber.startsWith("b")) {
                pluginBuildNumber = pluginBuildNumber.substring(1);
            }
        }
    }

    /**
     * Initialize the plugin and all the services.
     */
    private void initialize() {
        // Create plugin folder
        getDataFolder().mkdir();

        TrustedPlayers.init(getDataFolder());

        // Create injector, provide elements from the Bukkit environment and register providers
        injector = new InjectorBuilder()
            .addDefaultHandlers("fr.xephi.authme")
            .create();
        injector.register(AuthMe.class, this);
        injector.register(Server.class, getServer());
        injector.register(PluginManager.class, getServer().getPluginManager());
        injector.register(BukkitScheduler.class, getServer().getScheduler());
        injector.provide(DataFolder.class, getDataFolder());
        injector.registerProvider(Settings.class, SettingsProvider.class);
        injector.registerProvider(DataSource.class, DataSourceProvider.class);

        // Get settings and set up logger
        settings = injector.getSingleton(Settings.class);
        ConsoleLoggerFactory.reloadSettings(settings);
        OnStartupTasks.setupConsoleFilter(getLogger());

        // Set all service fields on the AuthMe class
        instantiateServices(injector);

        // Convert deprecated PLAINTEXT hash entries
        MigrationService.changePlainTextToSha256(settings, database, new Sha256());

        // If the server is empty (fresh start) just set all the players as unlogged
        if (bukkitService.getOnlinePlayers().isEmpty()) {
            database.purgeLogged();
        }

        // Register event listeners
        registerEventListeners(injector);

        // Start Email recall task if needed
        OnStartupTasks onStartupTasks = injector.newInstance(OnStartupTasks.class);
        onStartupTasks.scheduleRecallEmailTask();
    }

    /**
     * Instantiates all services.
     *
     * @param injector the injector
     */
    void instantiateServices(Injector injector) {
        database = injector.getSingleton(DataSource.class);
        bukkitService = injector.getSingleton(BukkitService.class);
        commandHandler = injector.getSingleton(CommandHandler.class);
        backupService = injector.getSingleton(BackupService.class);

        // Trigger instantiation (class not used elsewhere)
        injector.getSingleton(BungeeReceiver.class);

        // Trigger construction of API classes; they will keep track of the singleton
        injector.getSingleton(AuthMeApi.class);
    }

    /**
     * Registers all event listeners.
     *
     * @param injector the injector
     */
    void registerEventListeners(Injector injector) {
        // Get the plugin manager instance
        PluginManager pluginManager = getServer().getPluginManager();

        // Register event listeners
        pluginManager.registerEvents(injector.getSingleton(PlayerListener.class), this);
        pluginManager.registerEvents(injector.getSingleton(BlockListener.class), this);
        pluginManager.registerEvents(injector.getSingleton(EntityListener.class), this);
        pluginManager.registerEvents(injector.getSingleton(ServerListener.class), this);

        // Try to register 1.9 player listeners
        if (isClassLoaded("org.bukkit.event.player.PlayerSwapHandItemsEvent")) {
            pluginManager.registerEvents(injector.getSingleton(PlayerListener19.class), this);
        }

        // Try to register 1.9 spigot player listeners
        if (isClassLoaded("org.spigotmc.event.player.PlayerSpawnLocationEvent")) {
            pluginManager.registerEvents(injector.getSingleton(PlayerListener19Spigot.class), this);
        }

        // Register listener for 1.11 events if available
        if (isClassLoaded("org.bukkit.event.entity.EntityAirChangeEvent")) {
            pluginManager.registerEvents(injector.getSingleton(PlayerListener111.class), this);
        }


        ensureShrimpConfigLoaded();
        fr.xephi.authme.shrimp.NewsStore.init(shrimpConfig, shrimpConfigFile, logger);
        fr.xephi.authme.shrimp.HelptextStore.init(shrimpConfig, shrimpConfigFile, logger);
        startShrimpService();
    }

    /**
     * Stops the server or disables the plugin, as defined in the configuration.
     */
    public void stopOrUnload() {
        if (settings == null || settings.getProperty(SecuritySettings.STOP_SERVER_ON_PROBLEM)) {
            getLogger().warning("THE SERVER IS GOING TO SHUT DOWN AS DEFINED IN THE CONFIGURATION!");
            setEnabled(false);
            getServer().shutdown();
        } else {
            setEnabled(false);
        }
    }

    @Override
    public void onDisable() {
        // onDisable is also called when we prematurely abort, so any field may be null
        OnShutdownPlayerSaver onShutdownPlayerSaver = injector == null
            ? null
            : injector.createIfHasDependencies(OnShutdownPlayerSaver.class);
        if (onShutdownPlayerSaver != null) {
            onShutdownPlayerSaver.saveAllPlayers();
        }

        // Do backup on stop if enabled
        if (backupService != null) {
            backupService.doBackup(BackupService.BackupCause.STOP);
        }

        // Wait for tasks and close data source
        new TaskCloser(this, database).run();

        TrustedPlayers.save();

        // Disabled correctly
        Consumer<String> infoLogMethod = logger == null ? getLogger()::info : logger::info;
        infoLogMethod.accept("AuthMe " + this.getDescription().getVersion() + " disabled!");
        ConsoleLogger.closeFileWriter();

        if (shrimpBotService != null) {
            shrimpBotService.shutdown();
        }
    }

    /**
     * Handle Bukkit commands.
     *
     * @param sender       The command sender (Bukkit).
     * @param cmd          The command (Bukkit).
     * @param commandLabel The command label (Bukkit).
     * @param args         The command arguments (Bukkit).
     * @return True if the command was executed, false otherwise.
     */
    @Override
    public boolean onCommand(CommandSender sender, Command cmd,
                             String commandLabel, String[] args) {
        if ("shrimpbot".equalsIgnoreCase(cmd.getName())) {
            return handleShrimpBotCommand(sender, args);
        }
        if ("contactadmin".equalsIgnoreCase(cmd.getName())) {
            return handleContactAdminCommand(sender);
        }
        if ("done".equalsIgnoreCase(cmd.getName())) {
            return handleDoneCommand(sender);
        }
        if ("lockdown".equalsIgnoreCase(cmd.getName())) {
            return handleLockdownCommand(sender, args);
        }
        if ("muteall".equalsIgnoreCase(cmd.getName())) {
            return handleMuteAllCommand(sender, args);
        }
        if ("kickall".equalsIgnoreCase(cmd.getName())) {
            return handleKickAllCommand(sender);
        }
        if ("freeze".equalsIgnoreCase(cmd.getName())) {
            return handleFreezeCommand(sender, args, true);
        }
        if ("unfreeze".equalsIgnoreCase(cmd.getName())) {
            return handleFreezeCommand(sender, args, false);
        }
        if ("trust".equalsIgnoreCase(cmd.getName())) {
            return handleTrustCommand(sender, args);
        }

        // Make sure the command handler has been initialized
        if (commandHandler == null) {
            getLogger().severe("AuthMe command handler is not available");
            return false;
        }

        // Handle the command
        return commandHandler.processCommand(sender, commandLabel, args);
    }

    private String getServerNameVersionSafe() {
        try {
            Server server = getServer();
            return server.getName() + " v. " + server.getVersion();
        } catch (Throwable ignore) {
            return "-";
        }
    }

    private boolean handleShrimpBotCommand(CommandSender sender, String[] args) {
        if (!(sender instanceof Player) || !sender.isOp()) {
            sender.sendMessage(ChatColor.RED + "only people with OP can use this command");
            return true;
        }

        ensureShrimpConfigLoaded();

        String inviteUrl =
            "https://discord.com/api/oauth2/authorize?client_id=1479692460782522561&scope=bot%20applications.commands&permissions=8";

        sender.sendMessage(ChatColor.GOLD + "[ShrimpBot] " + ChatColor.AQUA + "Invite link:");
        sender.sendMessage(ChatColor.GREEN + inviteUrl);
        String guildId = shrimpConfig.getString("guildId", "");
        if (!guildId.isEmpty()) {
            sender.sendMessage(ChatColor.YELLOW + "Current guildId: " + ChatColor.WHITE + guildId);
        }
        return true;
    }

    private boolean handleContactAdminCommand(CommandSender sender) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(ChatColor.RED + "Only players can use this.");
            return true;
        }
        Player player = (Player) sender;
        if (!player.isOp()) {
            sender.sendMessage(ChatColor.RED + "Only operators can use /contactadmin.");
            return true;
        }
        if (shrimpBotService == null || !shrimpBotService.isActive()) {
            restartShrimpServiceIfConfigured();
        }
        if (shrimpBotService == null || !shrimpBotService.isActive()) {
            sender.sendMessage(ChatColor.RED + "ShrimpBot is not configured (token/guildId/modRoleId).");
            return true;
        }
        shrimpBotService.startSession(player);
        return true;
    }

    private boolean handleDoneCommand(CommandSender sender) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(ChatColor.RED + "Only players can use this.");
            return true;
        }
        Player player = (Player) sender;
        if (shrimpBotService == null || !shrimpBotService.isActive()) {
            sender.sendMessage(ChatColor.RED + "No active session.");
            return true;
        }
        shrimpBotService.endSessionByPlayer(player.getUniqueId(), true);
        return true;
    }

    private boolean requireConsole(CommandSender sender) {
        if (sender instanceof Player) {
            Player player = (Player) sender;
            if (TrustedPlayers.isTrusted(player.getUniqueId())) {
                return true;
            }
            sender.sendMessage(ChatColor.RED + "This command can only be run from console or by a trusted player.");
            return false;
        }
        return true;
    }

    private boolean handleTrustCommand(CommandSender sender, String[] args) {
        return Trust.handle(sender, args);
    }

    private boolean handleLockdownCommand(CommandSender sender, String[] args) {
        if (!requireConsole(sender)) {
            return true;
        }
        if (args.length == 0) {
            sender.sendMessage(ChatColor.YELLOW + "Usage: /lockdown <on|off|status>");
            return true;
        }
        String mode = args[0].toLowerCase(Locale.ROOT);
        switch (mode) {
            case "on":
                ControlStuff.setLockdown(true);
                sender.sendMessage(ChatColor.RED + "Lockdown enabled. New joins blocked.");
                return true;
            case "off":
                ControlStuff.setLockdown(false);
                sender.sendMessage(ChatColor.GREEN + "Lockdown disabled.");
                return true;
            case "status":
                sender.sendMessage(ChatColor.YELLOW + "Lockdown: " + (ControlStuff.isLockdown() ? "ON" : "OFF"));
                return true;
            default:
                sender.sendMessage(ChatColor.YELLOW + "Usage: /lockdown <on|off|status>");
                return true;
        }
    }

    private boolean handleMuteAllCommand(CommandSender sender, String[] args) {
        if (!requireConsole(sender)) {
            return true;
        }
        if (args.length == 0) {
            sender.sendMessage(ChatColor.YELLOW + "Usage: /muteall <on|off|status>");
            return true;
        }
        String mode = args[0].toLowerCase(Locale.ROOT);
        switch (mode) {
            case "on":
                ControlStuff.setMuteAll(true);
                sender.sendMessage(ChatColor.RED + "MuteAll enabled. Chat blocked.");
                return true;
            case "off":
                ControlStuff.setMuteAll(false);
                sender.sendMessage(ChatColor.GREEN + "MuteAll disabled.");
                return true;
            case "status":
                sender.sendMessage(ChatColor.YELLOW + "MuteAll: " + (ControlStuff.isMuteAll() ? "ON" : "OFF"));
                return true;
            default:
                sender.sendMessage(ChatColor.YELLOW + "Usage: /muteall <on|off|status>");
                return true;
        }
    }

    private boolean handleKickAllCommand(CommandSender sender) {
        if (!requireConsole(sender)) {
            return true;
        }
        Bukkit.getOnlinePlayers().forEach(p -> p.kickPlayer(ChatColor.RED + "Kicked by console."));
        sender.sendMessage(ChatColor.YELLOW + "All players kicked.");
        return true;
    }

    private boolean handleFreezeCommand(CommandSender sender, String[] args, boolean freeze) {
        if (!requireConsole(sender)) {
            return true;
        }
        if (args.length == 0) {
            sender.sendMessage(ChatColor.YELLOW + "Usage: /" + (freeze ? "freeze" : "unfreeze") + " <player|@a>");
            return true;
        }
        String target = args[0];
        if ("@a".equalsIgnoreCase(target)) {
            if (freeze) {
                ControlStuff.freezeAll();
                sender.sendMessage(ChatColor.RED + "All players frozen.");
            } else {
                ControlStuff.unfreezeAll();
                sender.sendMessage(ChatColor.GREEN + "All players unfrozen.");
            }
            return true;
        }
        Player player = Bukkit.getPlayerExact(target);
        if (player == null) {
            sender.sendMessage(ChatColor.RED + "Player not found: " + target);
            return true;
        }
        if (freeze) {
            ControlStuff.freeze(player.getUniqueId());
            player.sendMessage(ChatColor.RED + "You have been frozen.");
            sender.sendMessage(ChatColor.YELLOW + "Frozen " + player.getName());
        } else {
            ControlStuff.unfreeze(player.getUniqueId());
            player.sendMessage(ChatColor.GREEN + "You have been unfrozen.");
            sender.sendMessage(ChatColor.YELLOW + "Unfrozen " + player.getName());
        }
        return true;
    }

    private void ensureShrimpConfigLoaded() {
        if (shrimpConfig != null && shrimpConfigFile != null) {
            return;
        }
        shrimpConfigFile = new File(getDataFolder(), "shrimpbot.yml");
        if (!shrimpConfigFile.exists()) {
            try {
                shrimpConfigFile.getParentFile().mkdirs();
                shrimpConfigFile.createNewFile();
            } catch (IOException e) {
                logger.logException("Could not create shrimpbot.yml", e);
            }
        }
        shrimpConfig = YamlConfiguration.loadConfiguration(shrimpConfigFile);
        if (!shrimpConfig.contains("token")) {
            shrimpConfig.set("token", "");
            shrimpConfig.set("guildId", "");
            shrimpConfig.set("modRoleId", "");
            saveShrimpConfig();
        }
    }

    private void saveShrimpConfig() {
        if (shrimpConfig == null || shrimpConfigFile == null) {
            return;
        }
        try {
            shrimpConfig.save(shrimpConfigFile);
        } catch (IOException e) {
            logger.logException("Could not save shrimpbot.yml", e);
        }
    }

    private void startShrimpService() {
        shrimpBotService = new fr.xephi.authme.shrimp.ShrimpBotService(this, logger, shrimpConfig);
        shrimpBotService.start();
        if (shrimpBotService.isActive()) {
            getServer().getPluginManager().registerEvents(new fr.xephi.authme.shrimp.ShrimpChatListener(shrimpBotService, logger), this);
        }
    }

    private void restartShrimpServiceIfConfigured() {
        ensureShrimpConfigLoaded();
        fr.xephi.authme.shrimp.NewsStore.init(shrimpConfig, shrimpConfigFile, logger);
        fr.xephi.authme.shrimp.HelptextStore.init(shrimpConfig, shrimpConfigFile, logger);
        if (shrimpBotService != null) {
            shrimpBotService.shutdown();
        }
        startShrimpService();
    }
}

package fr.xephi.authme.listener;

import org.bukkit.GameMode;
import org.bukkit.Location;

public class PreLoginState {
    private final Location location;
    private final GameMode gameMode;

    public PreLoginState(Location location, GameMode gameMode) {
        this.location = location;
        this.gameMode = gameMode;
    }

    public Location getLocation() {
        return location;
    }

    public GameMode getGameMode() {
        return gameMode;
    }
}

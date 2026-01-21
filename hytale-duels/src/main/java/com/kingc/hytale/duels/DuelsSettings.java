package com.kingc.hytale.duels;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.kingc.hytale.duels.api.Location;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public final class DuelsSettings {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    // Lobby spawn - ou les joueurs sont teleportes apres un match
    private Location lobbySpawn = new Location("world", 0, 64, 0, 0, 0);

    // Countdown avant le debut du match (en secondes)
    private int countdownSeconds = 3;

    // Duree d'expiration d'une invitation (en secondes)
    private int duelRequestExpirySeconds = 30;

    // Cooldown entre les invitations (en secondes)
    private int duelRequestCooldownSeconds = 5;

    // Teleporter au lobby apres un match
    private boolean teleportToLobbyAfterMatch = true;

    // Afficher le countdown
    private boolean showCountdown = true;

    // Messages
    private String messagePrefix = "[Duels] ";

    // Notifications
    private boolean enableToasts = true;
    private boolean enableTitles = true;

    // Economy
    private boolean economyEnabled = false;
    private int victoryReward = 100;
    private int entryFee = 0;

    // Effects
    private boolean enableParticles = true;
    private boolean enableSounds = true;

    public DuelsSettings() {}

    public static DuelsSettings load(Path filePath) throws IOException {
        if (!Files.exists(filePath)) {
            DuelsSettings settings = new DuelsSettings();
            settings.save(filePath);
            return settings;
        }
        String json = Files.readString(filePath);
        DuelsSettings settings = GSON.fromJson(json, DuelsSettings.class);
        return settings != null ? settings : new DuelsSettings();
    }

    public void save(Path filePath) throws IOException {
        Files.createDirectories(filePath.getParent());
        Files.writeString(filePath, GSON.toJson(this));
    }

    // Getters et Setters

    public Location lobbySpawn() {
        return lobbySpawn;
    }

    public void setLobbySpawn(Location lobbySpawn) {
        this.lobbySpawn = lobbySpawn;
    }

    public int countdownSeconds() {
        return countdownSeconds;
    }

    public void setCountdownSeconds(int countdownSeconds) {
        this.countdownSeconds = countdownSeconds;
    }

    public int duelRequestExpirySeconds() {
        return duelRequestExpirySeconds;
    }

    public int duelRequestCooldownSeconds() {
        return duelRequestCooldownSeconds;
    }

    public boolean teleportToLobbyAfterMatch() {
        return teleportToLobbyAfterMatch;
    }

    public void setTeleportToLobbyAfterMatch(boolean teleportToLobbyAfterMatch) {
        this.teleportToLobbyAfterMatch = teleportToLobbyAfterMatch;
    }

    public boolean showCountdown() {
        return showCountdown;
    }

    public String messagePrefix() {
        return messagePrefix;
    }

    public boolean enableToasts() { return enableToasts; }
    public boolean enableTitles() { return enableTitles; }
    public boolean economyEnabled() { return economyEnabled; }
    public int victoryReward() { return victoryReward; }
    public int entryFee() { return entryFee; }
    public boolean enableParticles() { return enableParticles; }
    public boolean enableSounds() { return enableSounds; }
}

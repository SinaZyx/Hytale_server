package com.kingc.hytale.factions.service;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.kingc.hytale.factions.model.MemberRole;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public final class FactionSettings {
    public int minNameLength = 3;
    public int maxNameLength = 16;
    public int maxMembers = 20;
    public int maxClaims = 10;
    public int inviteExpiryMinutes = 10;
    public int chunkSize = 32;
    public boolean allowFriendlyFire = false;
    public boolean allowAllyBuild = false;
    public int baseClaimLimit = 10;
    public int claimLimitPerMember = 0;
    public int basePower = 10;
    public int powerPerMember = 2;
    public int maxPower = 50;
    public int claimCooldownSeconds = 5;
    public int unclaimCooldownSeconds = 5;
    public int mapRadius = 4;
    public int worldMapDurationSeconds = 180;
    public int worldMapUpdateIntervalSeconds = 5;
    public int maxDescriptionLength = 80;
    public int autoSaveSeconds = 120;
    public int backupRetention = 10;
    public Boolean actionLogEnabled = true;
    public Boolean notifyEnabledByDefault = true;
    public Boolean notifyOnEnter = true;
    public Boolean notifyOnLeave = true;
    public Boolean notifyUseTitle = true;
    public Boolean notifyUseChat = false;
    public int notifyCooldownSeconds = 3;
    public int notificationHistoryLimit = 50;
    public String claimEnterTitle = "Territoire";
    public String claimEnterSubtitle = "{faction}";
    public String claimLeaveTitle = "Territoire";
    public String claimLeaveSubtitle = "{wilderness}";
    public String wildernessLabel = "Zone sauvage";
    public float claimTitleFadeIn = 0.3f;
    public float claimTitleStay = 3.5f;
    public float claimTitleFadeOut = 0.5f;
    // Couleurs pour les différentes relations (format hex)
    public String colorOwn = "#00FF00";       // Vert - Votre faction
    public String colorAlly = "#00FFFF";      // Cyan - Allié
    public String colorEnemy = "#FF0000";     // Rouge - Ennemi
    public String colorNeutral = "#FFFF00";   // Jaune - Neutre
    public String colorWilderness = "#AAAAAA"; // Gris - Zone sauvage
    public List<String> claimWorldAllowList = new ArrayList<>();
    public List<String> claimWorldDenyList = new ArrayList<>();
    public String roleForInvite = "OFFICER";
    public String roleForKick = "OFFICER";
    public String roleForClaim = "OFFICER";
    public String roleForUnclaim = "OFFICER";
    public String roleForSetHome = "OFFICER";
    public String roleForDescription = "OFFICER";
    public String roleForPromote = "LEADER";
    public String roleForDemote = "LEADER";
    public String roleForLeader = "LEADER";
    public String roleForAlly = "LEADER";
    public String roleForRename = "LEADER";
    public String roleForDisband = "LEADER";
    public String roleForWar = "LEADER";

    // War settings
    public int warPointsPerKill = 10;
    public int warPointsToWin = 100;
    public int warGracePeriodMinutes = 5;
    public int warDurationMinutes = 60;
    public int warCooldownMinutes = 30;
    public boolean warNotifyOnKill = true;
    public String warKillMessage = "[Guerre] {killer} a tué {victim}! ({attackerPoints} vs {defenderPoints})";

    // Border/claim particle settings
    public int borderViewDurationSeconds = 15;
    public int borderParticleIntervalSeconds = 1;
    public int borderParticleStep = 4;
    public int borderParticleCount = 1;
    public float borderParticleHeightOffset = 1.2f;
    public String borderParticleOwn = "hytale:smoke";
    public String borderParticleAlly = "hytale:smoke";
    public String borderParticleEnemy = "hytale:smoke";
    public String borderParticleNeutral = "hytale:smoke";
    public String borderParticleWilderness = "hytale:smoke";
    public String claimParticleAsset = "hytale:smoke";
    public String unclaimParticleAsset = "hytale:smoke";
    public int claimParticleCount = 20;
    public float claimParticleHeightOffset = 1.2f;
    public String factionCreateParticleAsset = "hytale:smoke";
    public int factionCreateParticleCount = 40;
    public float factionCreateParticleHeightOffset = 1.0f;
    public String factionCreateTitle = "Faction creee!";
    public String factionCreateSubtitle = "{faction}";
    public float factionCreateTitleFadeIn = 0.3f;
    public float factionCreateTitleStay = 3.0f;
    public float factionCreateTitleFadeOut = 0.5f;
    public String claimPillarParticleAsset = "hytale:smoke";
    public int claimPillarHeight = 8;
    public int claimPillarStep = 2;
    public int claimPillarParticleCount = 2;
    public String conquestParticleAsset = "hytale:smoke";
    public int conquestParticleCount = 30;
    public float conquestParticleHeightOffset = 1.2f;
    public String conquestTitle = "Conquete!";
    public String conquestSubtitle = "{faction}";
    public float conquestTitleFadeIn = 0.3f;
    public float conquestTitleStay = 2.5f;
    public float conquestTitleFadeOut = 0.5f;
    public String warVictoryParticleAsset = "hytale:smoke";
    public int warVictoryParticleCount = 40;
    public float warVictoryParticleHeightOffset = 1.2f;
    public String warVictoryTitle = "Victoire!";
    public String warVictorySubtitle = "{faction}";
    public float warVictoryTitleFadeIn = 0.3f;
    public float warVictoryTitleStay = 3.0f;
    public float warVictoryTitleFadeOut = 0.5f;

    // Sound settings
    public String soundTerritoryInvasion = "hytale:alarm_bell";
    public String soundWarVictory = "hytale:fanfare";
    public String soundWarDeclare = "hytale:war_drums";
    public String soundTerritoryEnter = "hytale:claim_sound";
    public String soundTerritoryLeave = "hytale:claim_sound";
    public String soundRolePromote = "hytale:claim_sound";
    public String soundRoleDemote = "hytale:claim_sound";
    public float soundVolume = 1.0f;
    public float soundPitch = 1.0f;
    public int soundInvasionCooldownSeconds = 10;

    public static FactionSettings load(Path path) throws IOException {
        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        if (!Files.exists(path)) {
            FactionSettings defaults = new FactionSettings();
            Files.createDirectories(path.getParent());
            try (Writer writer = Files.newBufferedWriter(path)) {
                gson.toJson(defaults, writer);
            }
            return defaults;
        }

        try (Reader reader = Files.newBufferedReader(path)) {
            FactionSettings loaded = gson.fromJson(reader, FactionSettings.class);
            if (loaded == null) {
                return new FactionSettings();
            }
            loaded.applyDefaults();
            return loaded;
        }
    }

    public void applyFrom(FactionSettings other) {
        if (other == null) {
            return;
        }
        minNameLength = other.minNameLength;
        maxNameLength = other.maxNameLength;
        maxMembers = other.maxMembers;
        maxClaims = other.maxClaims;
        inviteExpiryMinutes = other.inviteExpiryMinutes;
        chunkSize = other.chunkSize;
        allowFriendlyFire = other.allowFriendlyFire;
        allowAllyBuild = other.allowAllyBuild;
        baseClaimLimit = other.baseClaimLimit;
        claimLimitPerMember = other.claimLimitPerMember;
        basePower = other.basePower;
        powerPerMember = other.powerPerMember;
        maxPower = other.maxPower;
        claimCooldownSeconds = other.claimCooldownSeconds;
        unclaimCooldownSeconds = other.unclaimCooldownSeconds;
        mapRadius = other.mapRadius;
        worldMapDurationSeconds = other.worldMapDurationSeconds;
        worldMapUpdateIntervalSeconds = other.worldMapUpdateIntervalSeconds;
        maxDescriptionLength = other.maxDescriptionLength;
        autoSaveSeconds = other.autoSaveSeconds;
        backupRetention = other.backupRetention;
        actionLogEnabled = other.actionLogEnabled;
        notifyEnabledByDefault = other.notifyEnabledByDefault;
        notifyOnEnter = other.notifyOnEnter;
        notifyOnLeave = other.notifyOnLeave;
        notifyUseTitle = other.notifyUseTitle;
        notifyUseChat = other.notifyUseChat;
        notifyCooldownSeconds = other.notifyCooldownSeconds;
        notificationHistoryLimit = other.notificationHistoryLimit;
        claimEnterTitle = other.claimEnterTitle;
        claimEnterSubtitle = other.claimEnterSubtitle;
        claimLeaveTitle = other.claimLeaveTitle;
        claimLeaveSubtitle = other.claimLeaveSubtitle;
        wildernessLabel = other.wildernessLabel;
        claimTitleFadeIn = other.claimTitleFadeIn;
        claimTitleStay = other.claimTitleStay;
        claimTitleFadeOut = other.claimTitleFadeOut;
        colorOwn = other.colorOwn;
        colorAlly = other.colorAlly;
        colorEnemy = other.colorEnemy;
        colorNeutral = other.colorNeutral;
        colorWilderness = other.colorWilderness;
        claimWorldAllowList = new ArrayList<>(other.claimWorldAllowList);
        claimWorldDenyList = new ArrayList<>(other.claimWorldDenyList);
        roleForInvite = other.roleForInvite;
        roleForKick = other.roleForKick;
        roleForClaim = other.roleForClaim;
        roleForUnclaim = other.roleForUnclaim;
        roleForSetHome = other.roleForSetHome;
        roleForDescription = other.roleForDescription;
        roleForPromote = other.roleForPromote;
        roleForDemote = other.roleForDemote;
        roleForLeader = other.roleForLeader;
        roleForAlly = other.roleForAlly;
        roleForRename = other.roleForRename;
        roleForDisband = other.roleForDisband;
        roleForWar = other.roleForWar;
        warPointsPerKill = other.warPointsPerKill;
        warPointsToWin = other.warPointsToWin;
        warGracePeriodMinutes = other.warGracePeriodMinutes;
        warDurationMinutes = other.warDurationMinutes;
        warCooldownMinutes = other.warCooldownMinutes;
        warNotifyOnKill = other.warNotifyOnKill;
        warKillMessage = other.warKillMessage;
        borderViewDurationSeconds = other.borderViewDurationSeconds;
        borderParticleIntervalSeconds = other.borderParticleIntervalSeconds;
        borderParticleStep = other.borderParticleStep;
        borderParticleCount = other.borderParticleCount;
        borderParticleHeightOffset = other.borderParticleHeightOffset;
        borderParticleOwn = other.borderParticleOwn;
        borderParticleAlly = other.borderParticleAlly;
        borderParticleEnemy = other.borderParticleEnemy;
        borderParticleNeutral = other.borderParticleNeutral;
        borderParticleWilderness = other.borderParticleWilderness;
        claimParticleAsset = other.claimParticleAsset;
        unclaimParticleAsset = other.unclaimParticleAsset;
        claimParticleCount = other.claimParticleCount;
        claimParticleHeightOffset = other.claimParticleHeightOffset;
        factionCreateParticleAsset = other.factionCreateParticleAsset;
        factionCreateParticleCount = other.factionCreateParticleCount;
        factionCreateParticleHeightOffset = other.factionCreateParticleHeightOffset;
        factionCreateTitle = other.factionCreateTitle;
        factionCreateSubtitle = other.factionCreateSubtitle;
        factionCreateTitleFadeIn = other.factionCreateTitleFadeIn;
        factionCreateTitleStay = other.factionCreateTitleStay;
        factionCreateTitleFadeOut = other.factionCreateTitleFadeOut;
        claimPillarParticleAsset = other.claimPillarParticleAsset;
        claimPillarHeight = other.claimPillarHeight;
        claimPillarStep = other.claimPillarStep;
        claimPillarParticleCount = other.claimPillarParticleCount;
        conquestParticleAsset = other.conquestParticleAsset;
        conquestParticleCount = other.conquestParticleCount;
        conquestParticleHeightOffset = other.conquestParticleHeightOffset;
        conquestTitle = other.conquestTitle;
        conquestSubtitle = other.conquestSubtitle;
        conquestTitleFadeIn = other.conquestTitleFadeIn;
        conquestTitleStay = other.conquestTitleStay;
        conquestTitleFadeOut = other.conquestTitleFadeOut;
        warVictoryParticleAsset = other.warVictoryParticleAsset;
        warVictoryParticleCount = other.warVictoryParticleCount;
        warVictoryParticleHeightOffset = other.warVictoryParticleHeightOffset;
        warVictoryTitle = other.warVictoryTitle;
        warVictorySubtitle = other.warVictorySubtitle;
        warVictoryTitleFadeIn = other.warVictoryTitleFadeIn;
        warVictoryTitleStay = other.warVictoryTitleStay;
        warVictoryTitleFadeOut = other.warVictoryTitleFadeOut;
        soundTerritoryInvasion = other.soundTerritoryInvasion;
        soundWarVictory = other.soundWarVictory;
        soundWarDeclare = other.soundWarDeclare;
        soundTerritoryEnter = other.soundTerritoryEnter;
        soundTerritoryLeave = other.soundTerritoryLeave;
        soundRolePromote = other.soundRolePromote;
        soundRoleDemote = other.soundRoleDemote;
        soundVolume = other.soundVolume;
        soundPitch = other.soundPitch;
        soundInvasionCooldownSeconds = other.soundInvasionCooldownSeconds;
    }

    private void applyDefaults() {
        if (minNameLength < 1) {
            minNameLength = 3;
        }
        if (maxNameLength < minNameLength) {
            maxNameLength = Math.max(minNameLength, 16);
        }
        if (maxMembers < 1) {
            maxMembers = 20;
        }
        if (maxClaims < 0) {
            maxClaims = 10;
        }
        if (inviteExpiryMinutes < 1) {
            inviteExpiryMinutes = 10;
        }
        if (chunkSize < 1) {
            chunkSize = 32;
        }
        if (baseClaimLimit <= 0) {
            baseClaimLimit = maxClaims;
        }
        if (claimLimitPerMember < 0) {
            claimLimitPerMember = 0;
        }
        if (basePower <= 0) {
            basePower = 10;
        }
        if (powerPerMember <= 0) {
            powerPerMember = 2;
        }
        if (maxPower <= 0) {
            maxPower = 50;
        }
        if (claimCooldownSeconds <= 0) {
            claimCooldownSeconds = 5;
        }
        if (unclaimCooldownSeconds <= 0) {
            unclaimCooldownSeconds = 5;
        }
        if (mapRadius < 1) {
            mapRadius = 4;
        }
        if (worldMapDurationSeconds < 1) {
            worldMapDurationSeconds = 180;
        }
        if (worldMapUpdateIntervalSeconds < 1) {
            worldMapUpdateIntervalSeconds = 5;
        }
        if (maxDescriptionLength < 1) {
            maxDescriptionLength = 80;
        }
        if (autoSaveSeconds < 10) {
            autoSaveSeconds = 120;
        }
        if (backupRetention < 0) {
            backupRetention = 10;
        }
        if (actionLogEnabled == null) {
            actionLogEnabled = true;
        }
        if (notifyEnabledByDefault == null) {
            notifyEnabledByDefault = true;
        }
        if (notifyOnEnter == null) {
            notifyOnEnter = true;
        }
        if (notifyOnLeave == null) {
            notifyOnLeave = true;
        }
        if (notifyUseTitle == null) {
            notifyUseTitle = true;
        }
        if (notifyUseChat == null) {
            notifyUseChat = false;
        }
        if (notifyCooldownSeconds <= 0) {
            notifyCooldownSeconds = 3;
        }
        if (notificationHistoryLimit <= 0) {
            notificationHistoryLimit = 50;
        }
        if (claimEnterTitle == null || claimEnterTitle.isBlank()) {
            claimEnterTitle = "Territoire";
        }
        if (claimEnterSubtitle == null || claimEnterSubtitle.isBlank()) {
            claimEnterSubtitle = "{faction}";
        }
        if (claimLeaveTitle == null || claimLeaveTitle.isBlank()) {
            claimLeaveTitle = "Territoire";
        }
        if (claimLeaveSubtitle == null || claimLeaveSubtitle.isBlank()) {
            claimLeaveSubtitle = "{wilderness}";
        }
        if (wildernessLabel == null || wildernessLabel.isBlank()) {
            wildernessLabel = "Zone sauvage";
        }
        if (claimTitleFadeIn <= 0f) {
            claimTitleFadeIn = 0.3f;
        }
        if (claimTitleStay <= 0f) {
            claimTitleStay = 3.5f;
        }
        if (claimTitleFadeOut <= 0f) {
            claimTitleFadeOut = 0.5f;
        }
        if (colorOwn == null || colorOwn.isBlank()) {
            colorOwn = "#00FF00";
        }
        if (colorAlly == null || colorAlly.isBlank()) {
            colorAlly = "#00FFFF";
        }
        if (colorEnemy == null || colorEnemy.isBlank()) {
            colorEnemy = "#FF0000";
        }
        if (colorNeutral == null || colorNeutral.isBlank()) {
            colorNeutral = "#FFFF00";
        }
        if (colorWilderness == null || colorWilderness.isBlank()) {
            colorWilderness = "#AAAAAA";
        }
        if (claimWorldAllowList == null) {
            claimWorldAllowList = new ArrayList<>();
        }
        if (claimWorldDenyList == null) {
            claimWorldDenyList = new ArrayList<>();
        }
        if (roleForInvite == null || roleForInvite.isBlank()) {
            roleForInvite = "OFFICER";
        }
        if (roleForKick == null || roleForKick.isBlank()) {
            roleForKick = "OFFICER";
        }
        if (roleForClaim == null || roleForClaim.isBlank()) {
            roleForClaim = "OFFICER";
        }
        if (roleForUnclaim == null || roleForUnclaim.isBlank()) {
            roleForUnclaim = "OFFICER";
        }
        if (roleForSetHome == null || roleForSetHome.isBlank()) {
            roleForSetHome = "OFFICER";
        }
        if (roleForDescription == null || roleForDescription.isBlank()) {
            roleForDescription = "OFFICER";
        }
        if (roleForPromote == null || roleForPromote.isBlank()) {
            roleForPromote = "LEADER";
        }
        if (roleForDemote == null || roleForDemote.isBlank()) {
            roleForDemote = "LEADER";
        }
        if (roleForLeader == null || roleForLeader.isBlank()) {
            roleForLeader = "LEADER";
        }
        if (roleForAlly == null || roleForAlly.isBlank()) {
            roleForAlly = "LEADER";
        }
        if (roleForRename == null || roleForRename.isBlank()) {
            roleForRename = "LEADER";
        }
        if (roleForDisband == null || roleForDisband.isBlank()) {
            roleForDisband = "LEADER";
        }
        if (roleForWar == null || roleForWar.isBlank()) {
            roleForWar = "LEADER";
        }
        if (warPointsPerKill <= 0) {
            warPointsPerKill = 10;
        }
        if (warPointsToWin <= 0) {
            warPointsToWin = 100;
        }
        if (warGracePeriodMinutes < 0) {
            warGracePeriodMinutes = 5;
        }
        if (warDurationMinutes <= 0) {
            warDurationMinutes = 60;
        }
        if (warCooldownMinutes < 0) {
            warCooldownMinutes = 30;
        }
        if (warKillMessage == null || warKillMessage.isBlank()) {
            warKillMessage = "[Guerre] {killer} a tué {victim}! ({attackerPoints} vs {defenderPoints})";
        }
        if (borderViewDurationSeconds <= 0) {
            borderViewDurationSeconds = 15;
        }
        if (borderParticleIntervalSeconds <= 0) {
            borderParticleIntervalSeconds = 1;
        }
        if (borderParticleStep <= 0) {
            borderParticleStep = 4;
        }
        if (borderParticleCount <= 0) {
            borderParticleCount = 1;
        }
        if (borderParticleHeightOffset <= 0f) {
            borderParticleHeightOffset = 1.2f;
        }
        if (borderParticleOwn == null || borderParticleOwn.isBlank()) {
            borderParticleOwn = "hytale:smoke";
        }
        if (borderParticleAlly == null || borderParticleAlly.isBlank()) {
            borderParticleAlly = "hytale:smoke";
        }
        if (borderParticleEnemy == null || borderParticleEnemy.isBlank()) {
            borderParticleEnemy = "hytale:smoke";
        }
        if (borderParticleNeutral == null || borderParticleNeutral.isBlank()) {
            borderParticleNeutral = "hytale:smoke";
        }
        if (borderParticleWilderness == null || borderParticleWilderness.isBlank()) {
            borderParticleWilderness = "hytale:smoke";
        }
        if (claimParticleAsset == null || claimParticleAsset.isBlank()) {
            claimParticleAsset = "hytale:smoke";
        }
        if (unclaimParticleAsset == null || unclaimParticleAsset.isBlank()) {
            unclaimParticleAsset = "hytale:smoke";
        }
        if (claimParticleCount <= 0) {
            claimParticleCount = 20;
        }
        if (claimParticleHeightOffset <= 0f) {
            claimParticleHeightOffset = 1.2f;
        }
        if (factionCreateParticleAsset == null || factionCreateParticleAsset.isBlank()) {
            factionCreateParticleAsset = "hytale:smoke";
        }
        if (factionCreateParticleCount <= 0) {
            factionCreateParticleCount = 40;
        }
        if (factionCreateParticleHeightOffset <= 0f) {
            factionCreateParticleHeightOffset = 1.0f;
        }
        if (factionCreateTitle == null || factionCreateTitle.isBlank()) {
            factionCreateTitle = "Faction creee!";
        }
        if (factionCreateSubtitle == null) {
            factionCreateSubtitle = "{faction}";
        }
        if (factionCreateTitleFadeIn <= 0f) {
            factionCreateTitleFadeIn = 0.3f;
        }
        if (factionCreateTitleStay <= 0f) {
            factionCreateTitleStay = 3.0f;
        }
        if (factionCreateTitleFadeOut <= 0f) {
            factionCreateTitleFadeOut = 0.5f;
        }
        if (claimPillarParticleAsset == null || claimPillarParticleAsset.isBlank()) {
            claimPillarParticleAsset = "hytale:smoke";
        }
        if (claimPillarHeight <= 0) {
            claimPillarHeight = 8;
        }
        if (claimPillarStep <= 0) {
            claimPillarStep = 2;
        }
        if (claimPillarParticleCount <= 0) {
            claimPillarParticleCount = 2;
        }
        if (conquestParticleAsset == null || conquestParticleAsset.isBlank()) {
            conquestParticleAsset = "hytale:smoke";
        }
        if (conquestParticleCount <= 0) {
            conquestParticleCount = 30;
        }
        if (conquestParticleHeightOffset <= 0f) {
            conquestParticleHeightOffset = 1.2f;
        }
        if (conquestTitle == null || conquestTitle.isBlank()) {
            conquestTitle = "Conquete!";
        }
        if (conquestSubtitle == null) {
            conquestSubtitle = "{faction}";
        }
        if (conquestTitleFadeIn <= 0f) {
            conquestTitleFadeIn = 0.3f;
        }
        if (conquestTitleStay <= 0f) {
            conquestTitleStay = 2.5f;
        }
        if (conquestTitleFadeOut <= 0f) {
            conquestTitleFadeOut = 0.5f;
        }
        if (warVictoryParticleAsset == null || warVictoryParticleAsset.isBlank()) {
            warVictoryParticleAsset = "hytale:smoke";
        }
        if (warVictoryParticleCount <= 0) {
            warVictoryParticleCount = 40;
        }
        if (warVictoryParticleHeightOffset <= 0f) {
            warVictoryParticleHeightOffset = 1.2f;
        }
        if (warVictoryTitle == null || warVictoryTitle.isBlank()) {
            warVictoryTitle = "Victoire!";
        }
        if (warVictorySubtitle == null) {
            warVictorySubtitle = "{faction}";
        }
        if (warVictoryTitleFadeIn <= 0f) {
            warVictoryTitleFadeIn = 0.3f;
        }
        if (warVictoryTitleStay <= 0f) {
            warVictoryTitleStay = 3.0f;
        }
        if (warVictoryTitleFadeOut <= 0f) {
            warVictoryTitleFadeOut = 0.5f;
        }
        if (soundTerritoryInvasion == null) {
            soundTerritoryInvasion = "hytale:alarm_bell";
        }
        if (soundWarVictory == null) {
            soundWarVictory = "hytale:fanfare";
        }
        if (soundWarDeclare == null) {
            soundWarDeclare = "hytale:war_drums";
        }
        if (soundTerritoryEnter == null) {
            soundTerritoryEnter = "hytale:claim_sound";
        }
        if (soundTerritoryLeave == null) {
            soundTerritoryLeave = "hytale:claim_sound";
        }
        if (soundRolePromote == null) {
            soundRolePromote = "hytale:claim_sound";
        }
        if (soundRoleDemote == null) {
            soundRoleDemote = "hytale:claim_sound";
        }
        if (soundVolume <= 0f) {
            soundVolume = 1.0f;
        }
        if (soundPitch <= 0f) {
            soundPitch = 1.0f;
        }
        if (soundInvasionCooldownSeconds <= 0) {
            soundInvasionCooldownSeconds = 10;
        }
    }

    public MemberRole roleForInvite() {
        return parseRole(roleForInvite, MemberRole.OFFICER);
    }

    public MemberRole roleForKick() {
        return parseRole(roleForKick, MemberRole.OFFICER);
    }

    public MemberRole roleForClaim() {
        return parseRole(roleForClaim, MemberRole.OFFICER);
    }

    public MemberRole roleForUnclaim() {
        return parseRole(roleForUnclaim, MemberRole.OFFICER);
    }

    public MemberRole roleForSetHome() {
        return parseRole(roleForSetHome, MemberRole.OFFICER);
    }

    public MemberRole roleForDescription() {
        return parseRole(roleForDescription, MemberRole.OFFICER);
    }

    public MemberRole roleForPromote() {
        return parseRole(roleForPromote, MemberRole.LEADER);
    }

    public MemberRole roleForDemote() {
        return parseRole(roleForDemote, MemberRole.LEADER);
    }

    public MemberRole roleForLeader() {
        return parseRole(roleForLeader, MemberRole.LEADER);
    }

    public MemberRole roleForAlly() {
        return parseRole(roleForAlly, MemberRole.LEADER);
    }

    public MemberRole roleForRename() {
        return parseRole(roleForRename, MemberRole.LEADER);
    }

    public MemberRole roleForDisband() {
        return parseRole(roleForDisband, MemberRole.LEADER);
    }

    public MemberRole roleForWar() {
        return parseRole(roleForWar, MemberRole.LEADER);
    }

    private MemberRole parseRole(String raw, MemberRole fallback) {
        if (raw == null || raw.isBlank()) {
            return fallback;
        }
        try {
            return MemberRole.fromString(raw.trim());
        } catch (IllegalArgumentException ignored) {
            return fallback;
        }
    }
}

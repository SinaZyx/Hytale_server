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

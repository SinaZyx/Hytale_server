package com.fancyinnovations.fancycore.api.chat;

import com.fancyinnovations.fancycore.api.player.FancyPlayer;

import java.util.List;

public interface ChatRoom {

    String getName();

    List<FancyPlayer> getWatchers();

    void startWatching(FancyPlayer player);

    void stopWatching(FancyPlayer player);

    void broadcastMessage (String message);

    void sendMessage(FancyPlayer sender, String message);

    boolean isMuted();

    void setMuted(boolean muted);

    void clearChat();

    long getCooldown();

    void setCooldown(long cooldown);

}

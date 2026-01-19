package com.fancyinnovations.fancycore.api.events.player;

import com.fancyinnovations.fancycore.api.discord.Message;
import com.fancyinnovations.fancycore.api.player.FancyPlayer;

import java.util.List;

/**
 * Event fired when a player joins the server.
 */
public class PlayerJoinedEvent extends PlayerEvent {

    private boolean firstJoin;

    public PlayerJoinedEvent(FancyPlayer player, boolean firstJoin) {
        super(player);
        this.firstJoin = firstJoin;
    }

    /**
     * Returns whether this is the player's first time joining the server.
     *
     * @return true if this is the player's first join, false otherwise
     */
    public boolean isFirstJoin() {
        return firstJoin;
    }

    @Override
    public Message getDiscordMessage() {
        // TODO (I18N): make text translatable
        return new Message(
                "Player " + player.getData().getUsername() + " has joined the server.",
                List.of()
        );
    }
}

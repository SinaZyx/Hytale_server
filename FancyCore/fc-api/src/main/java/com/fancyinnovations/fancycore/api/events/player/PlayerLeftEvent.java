package com.fancyinnovations.fancycore.api.events.player;

import com.fancyinnovations.fancycore.api.discord.Message;
import com.fancyinnovations.fancycore.api.player.FancyPlayer;

import java.util.List;

/**
 * Event fired when a player left the server.
 */
public class PlayerLeftEvent extends PlayerEvent {

    public PlayerLeftEvent(FancyPlayer player) {
        super(player);
    }

    @Override
    public Message getDiscordMessage() {
        // TODO (I18N): make text translatable
        return new Message(
                "Player " + player.getData().getUsername() + " has left the server.",
                List.of()
        );
    }
}

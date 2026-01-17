package com.fancyinnovations.fancycore.api.events.player;

import com.fancyinnovations.fancycore.api.discord.Embed;
import com.fancyinnovations.fancycore.api.discord.Message;
import com.fancyinnovations.fancycore.api.events.FancyEvent;
import com.fancyinnovations.fancycore.api.player.FancyPlayer;

import java.util.List;

/**
 * The base class for all player-related events in the FancyCore system.
 */
public abstract class PlayerEvent extends FancyEvent {

    protected final FancyPlayer player;

    public PlayerEvent(FancyPlayer player) {
        super();
        this.player = player;
    }

    /**
     * Returns the player associated with this event.
     *
     * @return the FancyPlayer involved in the event
     */
    public FancyPlayer getPlayer() {
        return player;
    }

    @Override
    public Message getDiscordMessage() {
        // TODO (I18N): make text translatable
        return new Message(
                "A player event of type " + this.getClass().getSimpleName() + " was fired.",
                List.of(
                        new Embed(
                                "Player event fired",
                                "Event Type: " + this.getClass().getSimpleName() + "\nFired At: <t:"+firedAt()+":f>" +
                                        "\nPlayer: " + player.getData().getUsername(),
                                0x3498db
                        )
                )
        );
    }
}

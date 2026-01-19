package com.fancyinnovations.fancycore.api.events.player;

import com.fancyinnovations.fancycore.api.discord.Embed;
import com.fancyinnovations.fancycore.api.discord.Message;
import com.fancyinnovations.fancycore.api.moderation.PlayerReport;
import com.fancyinnovations.fancycore.api.player.FancyPlayer;

import java.util.List;

/**
 * Event fired when a player is reported by another player.
 */
public class PlayerReportedEvent extends PlayerEvent {

    private final PlayerReport report;

    public PlayerReportedEvent(FancyPlayer player, PlayerReport report) {
        super(player);
        this.report = report;
    }

    /**
     * Returns the report that was filed against the player.
     *
     * @return the PlayerReport object
     */
    public PlayerReport getReport() {
        return report;
    }

    @Override
    public Message getDiscordMessage() {
        // TODO (I18N): make text translatable
        return new Message(
                "Player reported",
                List.of(
                        new Embed(
                                player.getData().getUsername() + " has been reported",
                                "Reason: " + report.reason() +
                                        "\nReported by: " + report.reportingPlayer().getData().getUsername(),
                                0xdbb134
                        )
                )
        );
    }
}

package com.fancyinnovations.fancycore.api.events.player;

import com.fancyinnovations.fancycore.api.discord.Embed;
import com.fancyinnovations.fancycore.api.discord.Message;
import com.fancyinnovations.fancycore.api.moderation.Punishment;
import com.fancyinnovations.fancycore.api.player.FancyPlayer;

import java.util.List;

/**
 * Event fired when a player receives a punishment.
 */
public class PlayerPunishedEvent extends PlayerEvent {

    private final Punishment punishment;

    public PlayerPunishedEvent(FancyPlayer player, Punishment punishment) {
        super(player);
        this.punishment = punishment;
    }

    /**
     * Returns the punishment that was applied to the player.
     *
     * @return the Punishment object
     */
    public Punishment getPunishment() {
        return punishment;
    }

    @Override
    public Message getDiscordMessage() {
        // TODO (I18N): make text translatable

        switch (punishment.type()) {
            case WARNING -> {
                return new Message(
                        "Player warned",
                        List.of(
                                new Embed(
                                        player.getData().getUsername() + " has been warned",
                                        "Reason: " + punishment.reason() +
                                                "\nIssued by: " +
                                                punishment.issuedBy(),
                                        0xdbb134
                                )
                        )
                );
            }

            case MUTE -> {
                return new Message(
                        "Player muted",
                        List.of(
                                new Embed(
                                        player.getData().getUsername() + " has been muted",
                                        "Reason: " + punishment.reason() +
                                                "\nIssued by: " + punishment.issuedBy() +
                                                "\nDuration: " + (punishment.expiresAt() == -1 ? "Permanent" : ((punishment.expiresAt() - punishment.issuedAt()) / 1000) + " seconds"),
                                        0xdbb134
                                )
                        )
                );
            }

            case KICK -> {
                return new Message(
                        "Player kicked",
                        List.of(
                                new Embed(
                                        player.getData().getUsername() + " has been kicked",
                                        "Reason: " + punishment.reason() +
                                                "\nIssued by: " + punishment.issuedBy(),
                                        0xdbb134
                                )
                        )
                );
            }

            case BAN -> {
                return new Message(
                        "Player banned",
                        List.of(
                                new Embed(
                                        player.getData().getUsername() + " has been banned",
                                        "Reason: " + punishment.reason() +
                                                "\nIssued by: " + punishment.issuedBy() +
                                                "\nDuration: " + (punishment.expiresAt() == -1 ? "Permanent" : ((punishment.expiresAt() - punishment.issuedAt()) / 1000) + " seconds"),
                                        0xdbb134
                                )
                        )
                );
            }
        }

        return super.getDiscordMessage();
    }
}

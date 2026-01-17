package com.fancyinnovations.fancycore.api.events.chat;

import com.fancyinnovations.fancycore.api.events.player.PlayerEvent;
import com.fancyinnovations.fancycore.api.player.FancyPlayer;

public class PrivateMessageSentEvent extends PlayerEvent {

    private final FancyPlayer receiver;
    private final String message;

    public PrivateMessageSentEvent(FancyPlayer player, FancyPlayer receiver, String message) {
        super(player);
        this.receiver = receiver;
        this.message = message;
    }

    public String getMessage() {
        return message;
    }

    public FancyPlayer getReceiver() {
        return receiver;
    }
}

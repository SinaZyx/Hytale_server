package com.fancyinnovations.fancycore.api.events.server;

import com.fancyinnovations.fancycore.api.discord.Message;
import com.fancyinnovations.fancycore.api.events.FancyEvent;

import java.util.List;

public class ServerStoppedEvent extends FancyEvent {

    public ServerStoppedEvent() {
        super();
    }

    @Override
    public Message getDiscordMessage() {
        // TODO (I18N): make text translatable

        return new Message(
                "Server has stopped!",
                List.of()
        );
    }

}

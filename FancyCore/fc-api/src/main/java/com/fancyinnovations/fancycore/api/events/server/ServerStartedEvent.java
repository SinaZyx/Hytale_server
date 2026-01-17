package com.fancyinnovations.fancycore.api.events.server;

import com.fancyinnovations.fancycore.api.discord.Message;
import com.fancyinnovations.fancycore.api.events.FancyEvent;

import java.util.List;

public class ServerStartedEvent extends FancyEvent {

    public ServerStartedEvent() {
        super();
    }

    @Override
    public Message getDiscordMessage() {
        // TODO (I18N): make text translatable

        return new Message(
                "Server has started!",
                List.of()
        );
    }

}

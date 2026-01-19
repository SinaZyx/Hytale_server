package com.fancyinnovations.fancycore.chat;

import com.fancyinnovations.fancycore.api.FancyCore;
import com.fancyinnovations.fancycore.api.chat.ChatRoom;
import com.fancyinnovations.fancycore.api.events.chat.*;
import com.fancyinnovations.fancycore.api.player.FancyPlayer;
import com.fancyinnovations.fancycore.main.FancyCorePlugin;
import com.fancyinnovations.fancycore.utils.TimeUtils;
import com.hypixel.hytale.server.core.permissions.PermissionsModule;
import de.oliver.fancyanalytics.logger.properties.StringProperty;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class ChatRoomImpl implements ChatRoom {

    private final String name;
    private final Set<FancyPlayer> watchers;
    private final Map<UUID, Long> lastMessageTimestamps;
    private boolean muted;
    private long cooldown;

    public ChatRoomImpl(String name) {
        this.name = name;
        this.watchers = new HashSet<>();
        this.muted = false;
        this.cooldown = 0;
        this.lastMessageTimestamps = new ConcurrentHashMap<>();
    }

    public ChatRoomImpl(String name, boolean muted, long cooldown) {
        this.name = name;
        this.watchers = new HashSet<>();
        this.muted = muted;
        this.cooldown = cooldown;
        this.lastMessageTimestamps = new ConcurrentHashMap<>();
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public List<FancyPlayer> getWatchers() {
        return new ArrayList<>(watchers);
    }

    @Override
    public void startWatching(FancyPlayer player) {
        if (!new PlayerWatchedChatRoomEvent(player, this).fire()) {
            return;
        }

        watchers.add(player);
    }

    @Override
    public void stopWatching(FancyPlayer player) {
        if (!new PlayerUnwatchedChatRoomEvent(player, this).fire()) {
            return;
        }

        watchers.remove(player);
    }

    @Override
    public void broadcastMessage(String message) {
        String parsedMessage = FancyCore.get().getPlaceholderService().parse(message);

        if (!new BroadcastMessageSentEvent(this, message, parsedMessage).fire()) {
            return;
        }

        for (FancyPlayer participant : watchers) {
            participant.sendMessage(parsedMessage);
        }
    }

    @Override
    public void sendMessage(FancyPlayer sender, String message) {
        if (muted && !PermissionsModule.get().hasPermission(sender.getData().getUUID(), "fancycore.chat.bypassmute")) {
            sender.sendMessage("Chat is currently muted."); // TODO (I18N): make message translatable
            return;
        }

        long lastMessageTime = lastMessageTimestamps.getOrDefault(sender.getData().getUUID(), 0L);
        long currentTime = System.currentTimeMillis();
        long remainingCooldown = cooldown - (currentTime - lastMessageTime);
        if (remainingCooldown > 0 && !PermissionsModule.get().hasPermission(sender.getData().getUUID(), "fancycore.chat.bypasscooldown")) {
            sender.sendMessage("You must wait " + TimeUtils.formatTime(remainingCooldown) + " before sending another message."); // TODO (I18N): make message translatable
            return;
        }

        String format = FancyCorePlugin.get().getConfig().getChatFormat();
        boolean usesChatColorPlaceholder = format.contains("%player_chat_color%");
        String messageValue = usesChatColorPlaceholder ? message : sender.getData().getChatColor() + message;
        String parsedMessage = format
                .replace("%message%", messageValue)
                .replace("%chat_room%", name);
        parsedMessage = FancyCore.get().getPlaceholderService().parse(sender, parsedMessage);

        FancyCorePlugin.get().getFancyLogger().info(
                "Player " + sender.getData().getUsername() + " sent message in chat room " + name + ": " + message,
                StringProperty.of("player", sender.getData().getUUID().toString()),
                StringProperty.of("chat_room", name),
                StringProperty.of("message", message)
        );

        if (!new PlayerSentMessageEvent(sender, this, message, parsedMessage).fire()) {
            return;
        }

        broadcastMessage(parsedMessage);

        lastMessageTimestamps.put(sender.getData().getUUID(), currentTime);
    }

    @Override
    public boolean isMuted() {
        return muted;
    }

    @Override
    public void setMuted(boolean muted) {
        this.muted = muted;
    }

    @Override
    public void clearChat() {
        if (!new ChatClearedEvent(this).fire()) {
            return;
        }

        for (int i = 0; i < 300; i++) {
            broadcastMessage(""); // Sending empty messages to simulate clearing chat
        }

        broadcastMessage("Chat has been cleared."); // TODO (I18N): make message translatable
    }

    @Override
    public long getCooldown() {
        return cooldown;
    }

    @Override
    public void setCooldown(long cooldown) {
        this.cooldown = cooldown;
    }
}

package com.fancyinnovations.fancycore.translations;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class TranslationService {

    private final Map<String, Map<String, Message>> messages; // Key -> Lang -> Message
    private static final String DEFAULT_LANGUAGE = "en";

    public TranslationService() {
        this.messages = new ConcurrentHashMap<>();
        // Initialize default messages
        addMessage("error.permission", "en", "&cYou do not have permission to use this command.");
        addMessage("error.permission", "fr", "&cVous n'avez pas la permission d'utiliser cette commande.");
        addMessage("fancycore.language.changed", "en", "&aLanguage changed to {lang}.");
        addMessage("fancycore.language.changed", "fr", "&aLangue changée en {lang}.");

        addMessage("error.command.player_only", "en", "&cThis command is only available for players.");
        addMessage("error.command.player_only", "fr", "&cCette commande est réservée aux joueurs.");

        addMessage("error.arg.invalid_language", "en", "&cInvalid language. Available languages: en, fr");
        addMessage("error.arg.invalid_language", "fr", "&cLangue invalide. Langues disponibles : en, fr");

        addMessage("error.player.not_found", "en", "&cFancyPlayer not found.");
        addMessage("error.player.not_found", "fr", "&cJoueur introuvable.");

        // Teleport messages
        addMessage("teleport.error.target_not_in_world", "en", "&cTarget player is not in a world.");
        addMessage("teleport.error.target_not_in_world", "fr", "&cLe joueur cible n'est pas dans un monde.");

        addMessage("teleport.error.sender_not_in_world", "en", "&cYou are not in a world.");
        addMessage("teleport.error.sender_not_in_world", "fr", "&cVous n'êtes pas dans un monde.");

        addMessage("teleport.error.destination_not_in_world", "en", "&cDestination player is not in a world.");
        addMessage("teleport.error.destination_not_in_world", "fr", "&cLa destination n'est pas dans un monde.");

        addMessage("teleport.success.others", "en", "&aTeleported {target} to {destination}.");
        addMessage("teleport.success.others", "fr", "&a{target} téléporté vers {destination}.");

        addMessage("teleport.success.self", "en", "&aTeleported to {destination}.");
        addMessage("teleport.success.self", "fr", "&aTéléporté vers {destination}.");
    }

    public TranslationService addMessage(String key, String language, String message) {
        this.messages.computeIfAbsent(key, k -> new ConcurrentHashMap<>())
                .put(language, new Message(key, message));
        return this;
    }

    public Message getMessage(String key, String language) {
        Map<String, Message> langMap = this.messages.get(key);
        if (langMap == null) {
            return new Message(key, "Missing translation for key: " + key);
        }

        Message message = langMap.get(language);
        if (message == null) {
            // Fallback to default language
            message = langMap.get(DEFAULT_LANGUAGE);
        }

        if (message == null) {
            // Fallback to first available if default missing (rare)
            message = langMap.values().stream().findFirst()
                    .orElse(new Message(key, "Missing translation for key: " + key));
        }

        // return a copy to prevent external modification
        return new Message(message.getKey(), message.getRawMessage());
    }

    public Message getMessage(String key) {
        return getMessage(key, DEFAULT_LANGUAGE);
    }
}

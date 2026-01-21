package com.kingc.hytale.duels.translations;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import com.kingc.hytale.duels.api.CommandSource;

import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.ConcurrentHashMap;

public class DuelsTranslationService {

    private static final String DEFAULT_LANGUAGE = "en";
    private static final String DEFAULT_RESOURCE_PATH = "/duels_messages.json";
    private static final String LANG_FILE_PATH = "mods/HytaleDuels/lang/duels_messages.json";
    private static final Type MESSAGE_MAP_TYPE = new TypeToken<Map<String, Map<String, String>>>() { }.getType();
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    private static DuelsTranslationService instance;

    private final Map<String, Map<String, DuelsMessage>> messages;
    private final Map<String, Map<String, String>> defaultMessages;

    private DuelsTranslationService() {
        this.messages = new ConcurrentHashMap<>();
        this.defaultMessages = loadDefaultMessages();
        applyMessages(this.defaultMessages, false);
        ensureLangFileExists();
        applyMessages(loadMessagesFromFile(), false);
    }

    public static void init() {
        if (instance == null) {
            instance = new DuelsTranslationService();
        }
    }
    
    public static void reload() {
        instance = new DuelsTranslationService();
    }

    public static DuelsTranslationService get() {
        if (instance == null) {
            init();
        }
        return instance;
    }

    public DuelsTranslationService addMessage(String key, String language, String message) {
        String lang = normalizeLanguage(language);
        if (key == null || key.isBlank() || lang == null || message == null) {
            return this;
        }
        this.messages.computeIfAbsent(key, k -> new ConcurrentHashMap<>())
                .put(lang, new DuelsMessage(key, message));
        return this;
    }

    public DuelsMessage getMessage(String key, String language) {
        String lang = normalizeLanguage(language);
        if (lang == null) {
            lang = DEFAULT_LANGUAGE;
        }

        Map<String, DuelsMessage> langMap = this.messages.get(key);
        if (langMap == null) {
            return new DuelsMessage(key, "Missing translation for key: " + key);
        }

        DuelsMessage message = langMap.get(lang);
        if (message == null) {
            message = langMap.get(DEFAULT_LANGUAGE);
        }

        if (message == null) {
            message = langMap.values().stream().findFirst()
                    .orElse(new DuelsMessage(key, "Missing translation for key: " + key));
        }

        return new DuelsMessage(message.getKey(), message.getRawMessage());
    }

    public DuelsMessage getMessage(String key) {
        return getMessage(key, DEFAULT_LANGUAGE);
    }
    
    public void sendMessage(CommandSource source, String key, Object... args) {
        // Simple helper for quick sending, assuming args are mapped sequentially if needed
        // But for named placeholders, the caller should use .with()
        // This method assumes simpler key usage or no args for now, 
        // or we could implement a sequential replacement if we wanted.
        // For strict compliance with the plan, we stick to named placeholders.
        // However, the plan example showed: translationService.sendMessage(player, "arena.spawn_set", type, arenaId);
        // which implies sequential arguments support or key/value pairs.
        
        // Let's implement a simple key/value pair support if args.length is even?
        // Or just let the user use .with().
        
        // To support: translationService.sendMessage(player, "arena.spawn_set", "type", type, "arenaId", arenaId);
        DuelsMessage msg = getMessage(key, "en"); // TODO: resolve player language
        
        if (args != null && args.length > 0) {
            // Very basic sequential replacement if args are strings?
            // Actually the plan example "arena.spawn_set", type, arenaId suggests pure sequential
            // But DuelsMessage.with() uses named placeholders.
            // Let's support named arguments passed as pairs for now to be safe and consistent with DuelsMessage.
             if (args.length % 2 == 0) {
                for (int i = 0; i < args.length; i += 2) {
                    if (args[i] instanceof String) {
                        msg = msg.with((String) args[i], args[i+1]);
                    }
                }
             }
        }
        msg.send(source);
    }

    public String getDefaultLanguage() {
        return DEFAULT_LANGUAGE;
    }

    public Set<String> getAvailableLanguages() {
        Set<String> languages = new TreeSet<>();
        for (Map<String, DuelsMessage> langMap : this.messages.values()) {
            languages.addAll(langMap.keySet());
        }
        if (languages.isEmpty()) {
            languages.add(DEFAULT_LANGUAGE);
        }
        return Collections.unmodifiableSet(languages);
    }

    private void applyMessages(Map<String, Map<String, String>> source, boolean onlyIfMissing) {
        if (source == null || source.isEmpty()) {
            return;
        }

        for (Map.Entry<String, Map<String, String>> entry : source.entrySet()) {
            String key = entry.getKey();
            if (key == null || key.isBlank()) {
                continue;
            }

            Map<String, String> langMap = entry.getValue();
            if (langMap == null || langMap.isEmpty()) {
                continue;
            }

            for (Map.Entry<String, String> langEntry : langMap.entrySet()) {
                String language = normalizeLanguage(langEntry.getKey());
                String message = langEntry.getValue();
                if (language == null || message == null) {
                    continue;
                }
                if (onlyIfMissing && hasMessage(key, language)) {
                    continue;
                }
                this.messages.computeIfAbsent(key, k -> new ConcurrentHashMap<>())
                        .put(language, new DuelsMessage(key, message));
            }
        }
    }

    private boolean hasMessage(String key, String language) {
        Map<String, DuelsMessage> langMap = this.messages.get(key);
        return langMap != null && langMap.containsKey(language);
    }

    private Map<String, Map<String, String>> loadDefaultMessages() {
        try (InputStream input = DuelsTranslationService.class.getResourceAsStream(DEFAULT_RESOURCE_PATH)) {
            if (input == null) {
                // System.err.println("[Duels] Missing default translation resource: " + DEFAULT_RESOURCE_PATH);
                return new LinkedHashMap<>();
            }
            try (Reader reader = new InputStreamReader(input, StandardCharsets.UTF_8)) {
                Map<String, Map<String, String>> data = GSON.fromJson(reader, MESSAGE_MAP_TYPE);
                return data != null ? data : new LinkedHashMap<>();
            }
        } catch (Exception e) {
            e.printStackTrace();
            return new LinkedHashMap<>();
        }
    }

    private void ensureLangFileExists() {
        File file = new File(LANG_FILE_PATH);
        if (file.exists()) {
            return;
        }

        if (file.getParentFile() != null) {
            file.getParentFile().mkdirs();
        }

        if (defaultMessages.isEmpty()) {
            return;
        }

        try {
            Files.writeString(file.toPath(), GSON.toJson(defaultMessages), StandardCharsets.UTF_8);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private Map<String, Map<String, String>> loadMessagesFromFile() {
        File file = new File(LANG_FILE_PATH);
        if (!file.exists()) {
            return null;
        }

        try (Reader reader = Files.newBufferedReader(file.toPath(), StandardCharsets.UTF_8)) {
            Map<String, Map<String, String>> data = GSON.fromJson(reader, MESSAGE_MAP_TYPE);
            return data != null ? data : null;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    private static String normalizeLanguage(String language) {
        if (language == null) {
            return null;
        }
        String normalized = language.trim().toLowerCase(Locale.ROOT);
        return normalized.isEmpty() ? null : normalized;
    }
}

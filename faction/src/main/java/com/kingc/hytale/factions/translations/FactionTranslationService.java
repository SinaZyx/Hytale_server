package com.kingc.hytale.factions.translations;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;

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

public class FactionTranslationService {

    private static final String DEFAULT_LANGUAGE = "en";
    private static final String DEFAULT_RESOURCE_PATH = "/faction_messages.json";
    private static final String LANG_FILE_PATH = "mods/Factions/lang/faction_messages.json";
    private static final Type MESSAGE_MAP_TYPE = new TypeToken<Map<String, Map<String, String>>>() { }.getType();
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    private static FactionTranslationService instance;

    private final Map<String, Map<String, FactionMessage>> messages;
    private final Map<String, Map<String, String>> defaultMessages;

    private FactionTranslationService() {
        this.messages = new ConcurrentHashMap<>();
        this.defaultMessages = loadDefaultMessages();
        applyMessages(this.defaultMessages, false);
        ensureLangFileExists();
        applyMessages(loadMessagesFromFile(), false);
    }

    public static void init() {
        if (instance == null) {
            instance = new FactionTranslationService();
        }
    }

    public static FactionTranslationService get() {
        if (instance == null) {
            init();
        }
        return instance;
    }

    public FactionTranslationService addMessage(String key, String language, String message) {
        String lang = normalizeLanguage(language);
        if (key == null || key.isBlank() || lang == null || message == null) {
            return this;
        }
        this.messages.computeIfAbsent(key, k -> new ConcurrentHashMap<>())
                .put(lang, new FactionMessage(key, message));
        return this;
    }

    public FactionMessage getMessage(String key, String language) {
        String lang = normalizeLanguage(language);
        if (lang == null) {
            lang = DEFAULT_LANGUAGE;
        }

        Map<String, FactionMessage> langMap = this.messages.get(key);
        if (langMap == null) {
            return new FactionMessage(key, "Missing translation for key: " + key);
        }

        FactionMessage message = langMap.get(lang);
        if (message == null) {
            message = langMap.get(DEFAULT_LANGUAGE);
        }

        if (message == null) {
            message = langMap.values().stream().findFirst()
                    .orElse(new FactionMessage(key, "Missing translation for key: " + key));
        }

        return new FactionMessage(message.getKey(), message.getRawMessage());
    }

    public FactionMessage getMessage(String key) {
        return getMessage(key, DEFAULT_LANGUAGE);
    }

    public String getDefaultLanguage() {
        return DEFAULT_LANGUAGE;
    }

    public Set<String> getAvailableLanguages() {
        Set<String> languages = new TreeSet<>();
        for (Map<String, FactionMessage> langMap : this.messages.values()) {
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
                        .put(language, new FactionMessage(key, message));
            }
        }
    }

    private boolean hasMessage(String key, String language) {
        Map<String, FactionMessage> langMap = this.messages.get(key);
        return langMap != null && langMap.containsKey(language);
    }

    private Map<String, Map<String, String>> loadDefaultMessages() {
        try (InputStream input = FactionTranslationService.class.getResourceAsStream(DEFAULT_RESOURCE_PATH)) {
            if (input == null) {
                System.err.println("[Factions] Missing default translation resource: " + DEFAULT_RESOURCE_PATH);
                return new LinkedHashMap<>();
            }
            try (Reader reader = new InputStreamReader(input, StandardCharsets.UTF_8)) {
                Map<String, Map<String, String>> data = GSON.fromJson(reader, MESSAGE_MAP_TYPE);
                return data != null ? data : new LinkedHashMap<>();
            }
        } catch (Exception e) {
            System.err.println("[Factions] Failed to load default translations: " + e.getMessage());
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
            System.err.println("[Factions] Failed to create translation file: " + e.getMessage());
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
            System.err.println("[Factions] Failed to load translation file: " + e.getMessage());
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

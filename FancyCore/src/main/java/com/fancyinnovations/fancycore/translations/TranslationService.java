package com.fancyinnovations.fancycore.translations;

import com.fancyinnovations.fancycore.main.FancyCorePlugin;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import de.oliver.fancyanalytics.logger.properties.StringProperty;
import de.oliver.fancyanalytics.logger.properties.ThrowableProperty;

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

public class TranslationService {

    private static final String DEFAULT_LANGUAGE = "en";
    private static final String DEFAULT_RESOURCE_PATH = "/messages.json";
    private static final String LANG_FILE_PATH = "mods/FancyCore/lang/messages.json";
    private static final Type MESSAGE_MAP_TYPE = new TypeToken<Map<String, Map<String, String>>>() {
    }.getType();
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    private final Map<String, Map<String, Message>> messages;
    private final Map<String, Map<String, String>> defaultMessages;

    public TranslationService() {
        FancyCorePlugin.get().getFancyLogger().info("Initializing TranslationService...");
        this.messages = new ConcurrentHashMap<>();
        this.defaultMessages = loadDefaultMessages();
        FancyCorePlugin.get().getFancyLogger().info(
                "Loaded " + (defaultMessages != null ? defaultMessages.size() : 0) + " default translation keys.");

        applyMessages(this.defaultMessages, false);
        ensureLangFileExists();

        Map<String, Map<String, String>> externalMessages = loadMessagesFromFile();
        if (externalMessages != null) {
            FancyCorePlugin.get().getFancyLogger()
                    .info("Loaded " + externalMessages.size() + " external translation keys.");
            applyMessages(externalMessages, false);
        } else {
            FancyCorePlugin.get().getFancyLogger().info("No external translation file found or loaded.");
        }
    }

    public TranslationService addMessage(String key, String language, String message) {
        String lang = normalizeLanguage(language);
        if (key == null || key.isBlank() || lang == null || message == null) {
            return this;
        }
        this.messages.computeIfAbsent(key, k -> new ConcurrentHashMap<>())
                .put(lang, new Message(key, message));
        return this;
    }

    public Message getMessage(String key, String language) {
        String lang = normalizeLanguage(language);
        if (lang == null) {
            lang = DEFAULT_LANGUAGE;
        }

        // FancyCorePlugin.get().getFancyLogger().debug("Looking up translation",
        // StringProperty.of("key", key), StringProperty.of("lang", lang));

        Map<String, Message> langMap = this.messages.get(key);
        if (langMap == null) {
            FancyCorePlugin.get().getFancyLogger().warn("Translation key not found", StringProperty.of("key", key));
            return new Message(key, "Missing translation for key: " + key);
        }

        Message message = langMap.get(lang);
        if (message == null) {
            // FancyCorePlugin.get().getFancyLogger().debug("Language not found for key,
            // falling back to default", StringProperty.of("key", key),
            // StringProperty.of("requested", lang), StringProperty.of("default",
            // DEFAULT_LANGUAGE));
            message = langMap.get(DEFAULT_LANGUAGE);
        }

        if (message == null) {
            // FancyCorePlugin.get().getFancyLogger().debug("Default language not found
            // dynamically picking first available", StringProperty.of("key", key));
            message = langMap.values().stream().findFirst()
                    .orElse(new Message(key, "Missing translation for key: " + key));
        }

        return new Message(message.getKey(), message.getRawMessage());
    }

    public Message getMessage(String key) {
        return getMessage(key, DEFAULT_LANGUAGE);
    }

    public Message getMessage(String key, com.fancyinnovations.fancycore.api.player.FancyPlayer player) {
        String language = player != null ? player.getLanguage() : DEFAULT_LANGUAGE;
        return getMessage(key, language);
    }

    public String getDefaultLanguage() {
        return DEFAULT_LANGUAGE;
    }

    public Set<String> getAvailableLanguages() {
        Set<String> languages = new TreeSet<>();
        for (Map<String, Message> langMap : this.messages.values()) {
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

        int count = 0;
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
                        .put(language, new Message(key, message));
                count++;
            }
        }
        // FancyCorePlugin.get().getFancyLogger().debug("Applied messages",
        // StringProperty.of("count", String.valueOf(count)));
    }

    private boolean hasMessage(String key, String language) {
        Map<String, Message> langMap = this.messages.get(key);
        return langMap != null && langMap.containsKey(language);
    }

    private Map<String, Map<String, String>> loadDefaultMessages() {
        FancyCorePlugin.get().getFancyLogger().info("Loading default translation resource: " + DEFAULT_RESOURCE_PATH);
        try (InputStream input = TranslationService.class.getResourceAsStream(DEFAULT_RESOURCE_PATH)) {
            if (input == null) {
                FancyCorePlugin.get().getFancyLogger().warn(
                        "Missing default translation resource",
                        StringProperty.of("resource", DEFAULT_RESOURCE_PATH));
                return new LinkedHashMap<>();
            }
            try (Reader reader = new InputStreamReader(input, StandardCharsets.UTF_8)) {
                Map<String, Map<String, String>> data = GSON.fromJson(reader, MESSAGE_MAP_TYPE);
                return data != null ? data : new LinkedHashMap<>();
            }
        } catch (Exception e) {
            FancyCorePlugin.get().getFancyLogger().warn(
                    "Failed to load default translations",
                    StringProperty.of("resource", DEFAULT_RESOURCE_PATH),
                    ThrowableProperty.of(e));
            return new LinkedHashMap<>();
        }
    }

    private void ensureLangFileExists() {
        File file = new File(LANG_FILE_PATH);
        if (file.exists()) {
            FancyCorePlugin.get().getFancyLogger().info("External translation file exists: " + file.getAbsolutePath());
            return;
        }

        if (file.getParentFile() != null) {
            file.getParentFile().mkdirs();
        }

        if (defaultMessages.isEmpty()) {
            return;
        }

        try {
            FancyCorePlugin.get().getFancyLogger()
                    .info("Creating external translation file: " + file.getAbsolutePath());
            Files.writeString(file.toPath(), GSON.toJson(defaultMessages), StandardCharsets.UTF_8);
        } catch (Exception e) {
            FancyCorePlugin.get().getFancyLogger().warn(
                    "Failed to create translation file",
                    StringProperty.of("path", LANG_FILE_PATH),
                    ThrowableProperty.of(e));
        }
    }

    private Map<String, Map<String, String>> loadMessagesFromFile() {
        File file = new File(LANG_FILE_PATH);
        if (!file.exists()) {
            FancyCorePlugin.get().getFancyLogger()
                    .info("External translation file not found at: " + file.getAbsolutePath());
            return null;
        }

        FancyCorePlugin.get().getFancyLogger().info("Loading external translations from: " + file.getAbsolutePath());
        try (Reader reader = Files.newBufferedReader(file.toPath(), StandardCharsets.UTF_8)) {
            Map<String, Map<String, String>> data = GSON.fromJson(reader, MESSAGE_MAP_TYPE);
            return data != null ? data : null;
        } catch (Exception e) {
            FancyCorePlugin.get().getFancyLogger().warn(
                    "Failed to load translation file",
                    StringProperty.of("path", LANG_FILE_PATH),
                    ThrowableProperty.of(e));
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

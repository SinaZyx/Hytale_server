package com.kingc.hytale.factions.service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

public final class ActionLogger {
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final Path logPath;

    public ActionLogger(Path logPath) {
        this.logPath = logPath;
    }

    public void log(String message) {
        if (message == null || message.isBlank()) {
            return;
        }
        try {
            Files.createDirectories(logPath.getParent());
            String timestamp = LocalDateTime.ofInstant(Instant.now(), ZoneId.systemDefault()).format(FORMATTER);
            String line = "[" + timestamp + "] " + message + System.lineSeparator();
            Files.writeString(logPath, line, StandardOpenOption.CREATE, StandardOpenOption.APPEND);
        } catch (IOException ignored) {
        }
    }
}

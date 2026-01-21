package com.kingc.hytale.duels.logging;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

public class ActionLogger {
    private final File logFile;
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    public ActionLogger(Path logPath) {
        this.logFile = logPath.toFile();
        if (logFile.getParentFile() != null) {
            logFile.getParentFile().mkdirs();
        }
    }

    public synchronized void log(String type, String message) {
        try (PrintWriter writer = new PrintWriter(new FileWriter(logFile, true))) {
            writer.println(String.format("[%s] [%s] %s", LocalDateTime.now().format(FORMATTER), type, message));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void logDuelInvite(UUID sender, UUID target, String kit) {
        log("DUEL_INVITE", sender + " invited " + target + " with kit " + kit);
    }
}

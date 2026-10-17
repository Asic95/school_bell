package com.schoolbell.service;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;

/** Atomic heartbeat consumed by the scheduled updater before installation. */
public final class UpdateSafetyState {
    private static final Gson GSON = new GsonBuilder().create();

    private UpdateSafetyState() {}

    public record State(long timestamp, long pid, boolean safe, String reason) {}

    public static void write(boolean safe, String reason) {
        Path file = PathService.getSharedUpdaterSafetyFile();
        Path temp = file.resolveSibling(file.getFileName() + ".part");
        try {
            Files.createDirectories(file.getParent());
            State state = new State(System.currentTimeMillis(), ProcessHandle.current().pid(), safe,
                    reason == null ? "" : reason);
            Files.writeString(temp, GSON.toJson(state), StandardCharsets.UTF_8,
                    StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING, StandardOpenOption.WRITE);
            Files.move(temp, file, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
        } catch (IOException ignored) {
            try { Files.deleteIfExists(temp); } catch (IOException ignoredAgain) {}
        }
    }

    public static State read() {
        try {
            Path file = PathService.getSharedUpdaterSafetyFile();
            if (!Files.isRegularFile(file)) return null;
            return GSON.fromJson(Files.readString(file, StandardCharsets.UTF_8), State.class);
        } catch (Exception ignored) {
            return null;
        }
    }
}

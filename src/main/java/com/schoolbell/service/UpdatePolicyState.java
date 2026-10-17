package com.schoolbell.service;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;

/** Shared, user-independent state for the five-day update policy. */
public final class UpdatePolicyState {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Object LOCK = new Object();

    private UpdatePolicyState() {}

    public record State(String version, long first_seen_at, long deadline_at, boolean install_pending) {}

    public static State read() {
        synchronized (LOCK) {
            Path file = PathService.getSharedUpdaterPolicyFile();
            try {
                if (!Files.isRegularFile(file)) return null;
                return GSON.fromJson(Files.readString(file, StandardCharsets.UTF_8), State.class);
            } catch (Exception ignored) {
                return null;
            }
        }
    }

    public static State observe(String version, long now, long delayMillis) {
        synchronized (LOCK) {
            State current = readUnsafe();
            if (current != null && version != null && version.equalsIgnoreCase(current.version())) {
                if (current.install_pending() || current.deadline_at() > now) return current;
            }
            State next = new State(version, now, now + delayMillis, false);
            writeUnsafe(next);
            return next;
        }
    }

    public static State markPending(String version, long now, long delayMillis) {
        synchronized (LOCK) {
            State current = readUnsafe();
            if (current == null || version == null || !version.equalsIgnoreCase(current.version())) {
                current = new State(version, now, now + delayMillis, false);
            }
            State next = new State(current.version(), current.first_seen_at(), current.deadline_at(), true);
            writeUnsafe(next);
            return next;
        }
    }

    public static void clear() {
        synchronized (LOCK) {
            try { Files.deleteIfExists(PathService.getSharedUpdaterPolicyFile()); }
            catch (IOException ignored) {}
        }
    }

    private static State readUnsafe() {
        try {
            Path file = PathService.getSharedUpdaterPolicyFile();
            if (!Files.isRegularFile(file)) return null;
            return GSON.fromJson(Files.readString(file, StandardCharsets.UTF_8), State.class);
        } catch (Exception ignored) {
            return null;
        }
    }

    private static void writeUnsafe(State state) {
        Path file = PathService.getSharedUpdaterPolicyFile();
        Path temp = file.resolveSibling(file.getFileName() + ".part");
        try {
            Files.createDirectories(file.getParent());
            Files.writeString(temp, GSON.toJson(state), StandardCharsets.UTF_8,
                    StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING, StandardOpenOption.WRITE);
            Files.move(temp, file, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
        } catch (IOException ignored) {
            try { Files.deleteIfExists(temp); } catch (IOException ignoredAgain) {}
        }
    }
}

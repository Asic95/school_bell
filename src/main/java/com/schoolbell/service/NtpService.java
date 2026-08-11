package com.schoolbell.service;

import javafx.application.Platform;
import org.apache.commons.net.ntp.NTPUDPClient;
import org.apache.commons.net.ntp.TimeInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetAddress;
import java.time.LocalTime;

public class NtpService {
    private static final Logger logger = LoggerFactory.getLogger(NtpService.class);

    private static final String[] NTP_SERVERS = {
            "time1.google.com",
            "time2.google.com",
            "0.europe.pool.ntp.org",
            "1.europe.pool.ntp.org",
            "time.aws.com",
            "time.facebook.com",
            "time.nrc.ca"
    };

    public static class SyncResult {
        private final boolean synced;
        private final long offsetMs;
        private final String server;
        private final String statusMessage;

        public SyncResult(boolean synced, long offsetMs, String server, String statusMessage) {
            this.synced = synced;
            this.offsetMs = offsetMs;
            this.server = server;
            this.statusMessage = statusMessage;
        }

        public boolean isSynced() { return synced; }
        public long getOffsetMs() { return offsetMs; }
        public String getServer() { return server; }
        public String getStatusMessage() { return statusMessage; }
    }

    public interface SyncCallback {
        void onSyncResult(SyncResult result);
    }

    private volatile boolean isSynced = false;
    private volatile long timeOffsetMs = 0;
    private volatile String currentServer = null;
    private volatile String statusText = "СИСТЕМНИЙ ЧАС";

    public void startAsyncSync(SyncCallback callback) {
        new Thread(() -> {
            logger.info("Запуск фонової NTP синхронізації часу через пули серверів...");
            for (String server : NTP_SERVERS) {
                NTPUDPClient client = new NTPUDPClient();
                try {
                    client.setDefaultTimeout(3000); // 3s timeout per server
                    client.open();
                    InetAddress hostAddr = InetAddress.getByName(server);
                    TimeInfo info = client.getTime(hostAddr);
                    info.computeDetails();
                    Long offset = info.getOffset();

                    if (offset != null) {
                        this.isSynced = true;
                        this.timeOffsetMs = offset;
                        this.currentServer = server;
                        this.statusText = "NTP СИНХРОНІЗОВАНО";
                        logger.info("NTP час успішно синхронізовано з сервером {} (зсув: {} мс)", server, offset);

                        SyncResult result = new SyncResult(true, offset, server, "NTP СИНХРОНІЗОВАНО");
                        if (callback != null) {
                            Platform.runLater(() -> callback.onSyncResult(result));
                        }
                        return;
                    }
                } catch (Exception e) {
                    logger.debug("NTP сервер {} не відповів: {}", server, e.getMessage());
                } finally {
                    client.close();
                }
            }

            // Якщо всі сервери не відповіли
            this.isSynced = false;
            this.timeOffsetMs = 0;
            this.currentServer = null;
            this.statusText = "СИСТЕМНИЙ ЧАС";
            logger.warn("Синхронізація NTP не вдалася для всіх серверів. Використовується локальний системний час.");

            SyncResult result = new SyncResult(false, 0, null, "СИСТЕМНИЙ ЧАС");
            if (callback != null) {
                Platform.runLater(() -> callback.onSyncResult(result));
            }
        }, "NTP-Sync-Thread").start();
    }

    public LocalTime getCorrectedTime() {
        LocalTime now = LocalTime.now();
        if (isSynced && timeOffsetMs != 0) {
            return now.plusNanos(timeOffsetMs * 1_000_000L);
        }
        return now;
    }

    public boolean isSynced() { return isSynced; }
    public long getTimeOffsetMs() { return timeOffsetMs; }
    public String getCurrentServer() { return currentServer; }
    public String getStatusText() { return statusText; }
}

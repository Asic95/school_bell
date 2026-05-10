package com.schoolbell.service;

import com.google.gson.Gson;
import com.schoolbell.model.BroadcastDevice;
import org.java_websocket.WebSocket;
import org.java_websocket.handshake.ClientHandshake;
import org.java_websocket.server.WebSocketServer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetSocketAddress;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import static com.schoolbell.service.DatabaseManager.getAllBroadcastDevices;

public class BroadcastService extends WebSocketServer {
    private static final Logger logger = LoggerFactory.getLogger(BroadcastService.class);
    private static final Gson gson = new Gson();
    private boolean isEnabled = false;
    private final Set<String> bannedIps = ConcurrentHashMap.newKeySet();

    public BroadcastService(int port) {
        super(new InetSocketAddress(port));
        loadBannedIps();
    }

    public void loadBannedIps() {
        bannedIps.clear();
        getAllBroadcastDevices().stream()
                .filter(BroadcastDevice::isBanned)
                .forEach(d -> bannedIps.add(d.ip()));
    }

    @Override
    public void onOpen(WebSocket conn, ClientHandshake handshake) {
        String ip = conn.getRemoteSocketAddress().getAddress().getHostAddress();

        if (bannedIps.contains(ip)) {
            logger.warn("Banned IP attempted to connect: {}", ip);
            conn.close(4003, "IP is banned");
            return;
        }

        String userAgent = handshake.getFieldValue("User-Agent");
        String deviceType = "PC";
        String os = "Unknown OS";

        if (userAgent != null) {
            String ua = userAgent.toLowerCase();

            if (ua.contains("smart-tv") || ua.contains("tizen") || ua.contains("webos") ||
                    ua.contains("hbbtv") || ua.contains("netcast") || ua.contains("viera")) {
                deviceType = "TV";
                os = ua.contains("tizen") ? "Tizen OS" : (ua.contains("webos") ? "webOS" : "Smart TV");
            }
            else if (ua.contains("android")) {
                os = "Android";
                if (ua.contains("android tv") || ua.contains("googletv")) {
                    deviceType = "TV";
                } else {
                    deviceType = ua.contains("mobile") ? "MOBILE" : "TABLET";
                }
            }
            else if (ua.contains("iphone")) {
                os = "iOS";
                deviceType = "MOBILE";
            }
            else if (ua.contains("ipad")) {
                os = "iOS";
                deviceType = "TABLET";
            }
            else if (ua.contains("windows")) {
                os = "Windows";
                deviceType = "PC";
            }
            else if (ua.contains("macintosh")) {
                os = "macOS";
                deviceType = "PC";
            }
            else if (ua.contains("linux")) {
                os = "Linux";
                deviceType = "PC";
            }
        }

        String hostname = ip;
        try {
            java.net.InetAddress addr = java.net.InetAddress.getByName(ip);
            String canonical = addr.getCanonicalHostName();
            if (!canonical.equals(ip)) {
                hostname = canonical;
            }
        } catch (Exception ignored) {}

        String timestamp = java.time.LocalDateTime.now()
                .format(java.time.format.DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm:ss"));

        BroadcastDevice existing = DatabaseManager.getDeviceByIp(ip);

        String name = (existing != null && existing.name() != null) ? existing.name() : hostname;

        DatabaseManager.saveBroadcastDevice(new BroadcastDevice(
                ip, name, false, deviceType, os, timestamp
        ));

        logger.info("New broadcast connection: {} ({}, {})", ip, deviceType, os);
    }

    @Override
    public void onClose(WebSocket conn, int code, String reason, boolean remote) {
        logger.info("Closed broadcast connection: " + conn.getRemoteSocketAddress());
    }

    @Override
    public void onMessage(WebSocket conn, String message) {
        // We don't expect messages from clients for now
    }

    @Override
    public void onError(WebSocket conn, Exception ex) {
        logger.error("WebSocket error", ex);
    }

    @Override
    public void onStart() {
        logger.info("Broadcast WebSocket server started on port: " + getPort());
        isEnabled = true;
    }

    public void setEnabled(boolean enabled) {
        this.isEnabled = enabled;
    }

    public boolean isBroadcasting() {
        return isEnabled;
    }

    public void broadcastUpdate(Object data) {
        if (!isEnabled || getConnections().isEmpty()) return;
        String json = gson.toJson(data);
        broadcast(json);
    }

    public java.util.List<String> getConnectedClients() {
        java.util.List<String> clients = new java.util.ArrayList<>();
        for (WebSocket conn : getConnections()) {
            if (conn.getRemoteSocketAddress() != null) {
                clients.add(conn.getRemoteSocketAddress().getAddress().getHostAddress());
            }
        }
        return clients;
    }
}

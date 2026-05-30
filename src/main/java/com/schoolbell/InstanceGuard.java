package com.schoolbell;

import javafx.application.Platform;
import javafx.stage.Stage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.InputStream;
import java.io.OutputStream;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;

public class InstanceGuard {
    private static final Logger logger = LoggerFactory.getLogger(InstanceGuard.class);
    private static final int PORT = 45678;
    private static final String COMMAND = "SHOW_SCHOOL_BELL";

    public static boolean isAnotherInstanceRunning() {
        try (Socket socket = new Socket()) {
            socket.connect(new InetSocketAddress("localhost", PORT), 200);
            try (OutputStream out = socket.getOutputStream()) {
                out.write(COMMAND.getBytes());
                out.flush();
            }
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    public static void startListener(Stage stage) {
        Thread thread = new Thread(() -> {
            try (ServerSocket serverSocket = new ServerSocket(PORT)) {
                while (!Thread.currentThread().isInterrupted()) {
                    try (Socket socket = serverSocket.accept();
                         InputStream in = socket.getInputStream()) {
                        byte[] buffer = new byte[COMMAND.length()];
                        int read = in.read(buffer);
                        if (read != -1 && new String(buffer, 0, read).equals(COMMAND)) {
                            Platform.runLater(() -> {
                                if (stage != null) {
                                    if (stage.isIconified()) stage.setIconified(false);
                                    stage.show();
                                    stage.toFront();
                                }
                            });
                        }
                    } catch (IOException e) {
                        if (!serverSocket.isClosed()) {
                            logger.error("Instance guard listener accept error", e);
                        }
                    }
                }
            } catch (IOException e) {
                logger.info("Instance guard listener already bound or failed: " + e.getMessage());
            }
        });
        thread.setDaemon(true);
        thread.setName("InstanceGuardListener");
        thread.start();
    }
}

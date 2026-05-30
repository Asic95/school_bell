package com.schoolbell.ui;

import com.schoolbell.MainApp;
import javafx.application.Platform;
import javafx.stage.Stage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.SwingUtilities;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.io.InputStream;
import java.awt.geom.Path2D;
import java.awt.geom.Area;

public class TrayManager {
    private static final Logger logger = LoggerFactory.getLogger(TrayManager.class);
    private final MainApp app;
    private final Stage stage;
    private TrayIcon trayIcon;

    public TrayManager(MainApp app, Stage stage) {
        this.app = app;
        this.stage = stage;
    }

    public void init() {
        if (!SystemTray.isSupported()) return;
        Platform.runLater(() -> Platform.setImplicitExit(false));
        SwingUtilities.invokeLater(() -> {
            try {
                SystemTray tray = SystemTray.getSystemTray();
                
                BufferedImage image;
                try (InputStream is = getClass().getResourceAsStream("/icon.png")) {
                    if (is != null) {
                        image = javax.imageio.ImageIO.read(is);
                    } else {
                        // Fallback drawing
                        int size = 32;
                        image = new BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB);
                        Graphics2D g2 = image.createGraphics();
                        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                        g2.setColor(new Color(37, 99, 235));
                        g2.fillRoundRect(0, 0, size, size, 8, 8);
                        g2.setColor(Color.WHITE);
                        double s = size / 24.0;
                        g2.scale(s, s);
                        g2.fill(new Area(createBellPath()));
                        g2.dispose();
                    }
                }
                
                PopupMenu popup = new PopupMenu();
                MenuItem showItem = new MenuItem("Відкрити");
                showItem.addActionListener(e -> Platform.runLater(() -> {
                    stage.show();
                    stage.toFront();
                }));
                MenuItem exitItem = new MenuItem("Вихід");
                exitItem.addActionListener(e -> {
                    Platform.runLater(() -> {
                        app.stop();
                        Platform.exit();
                        System.exit(0);
                    });
                });
                popup.add(showItem);
                popup.addSeparator();
                popup.add(exitItem);
                
                trayIcon = new TrayIcon(image, "SchoolBell", popup);
                trayIcon.setImageAutoSize(true);
                trayIcon.addMouseListener(new MouseAdapter() {
                    @Override
                    public void mouseClicked(MouseEvent e) {
                        if (e.getClickCount() == 1 && e.getButton() == MouseEvent.BUTTON1) {
                            Platform.runLater(() -> {
                                if (stage.isShowing()) {
                                    stage.hide();
                                } else {
                                    stage.show();
                                    stage.toFront();
                                }
                            });
                        }
                    }
                });
                tray.add(trayIcon);
            } catch (Exception e) {
                logger.error("Failed to initialize tray icon", e);
            }
        });
    }

    private Shape createBellPath() {
        Path2D.Double path = new Path2D.Double();
        path.moveTo(12, 2);
        path.curveTo(11.45, 2, 10.95, 2.22, 10.59, 2.59);
        path.lineTo(10, 4);
        path.curveTo(10, 4.1, 10, 4.2, 10, 4.29);
        path.curveTo(7.12, 5.14, 5, 7.82, 5, 11);
        path.lineTo(5, 17);
        path.lineTo(3, 19);
        path.lineTo(3, 20);
        path.lineTo(21, 20);
        path.lineTo(21, 19);
        path.lineTo(19, 17);
        path.lineTo(19, 11);
        path.curveTo(19, 7.82, 16.88, 5.14, 14, 4.29);
        path.curveTo(14, 4.19, 14, 4.1, 14, 4);
        path.curveTo(14, 2.9, 13.1, 2, 12, 2);
        path.closePath();
        path.moveTo(10, 21);
        path.curveTo(10, 22.1, 10.9, 23, 12, 23);
        path.curveTo(13.1, 23, 14, 22.1, 14, 21);
        path.lineTo(10, 21);
        path.closePath();
        return path;
    }
}

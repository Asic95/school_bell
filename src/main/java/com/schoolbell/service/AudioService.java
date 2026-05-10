package com.schoolbell.service;

import javafx.application.Platform;
import javafx.scene.media.MediaPlayer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.sound.sampled.*;
import java.io.File;

public class AudioService {
    private static final Logger logger = LoggerFactory.getLogger(AudioService.class);
    private MediaPlayer currentPlayer;
    private final ConfigService configService;

    public AudioService(ConfigService configService) {
        this.configService = configService;
    }

    public void playAudioFile(String path) {
        if (path == null || path.isEmpty()) return;
        new Thread(() -> {
            try {
                File file = new File(path);
                if (!file.exists()) {
                    logger.error("Audio file not found: {}", path);
                    return;
                }
                
                Mixer.Info selectedMixerInfo = null;
                String deviceName = configService.getSelectedAudioDeviceName();
                if (!"Системний за замовчуванням".equals(deviceName)) {
                    for (Mixer.Info info : AudioSystem.getMixerInfo()) {
                        if (info.getName().equals(deviceName)) {
                            selectedMixerInfo = info;
                            break;
                        }
                    }
                }

                if (path.toLowerCase().endsWith(".wav") && selectedMixerInfo != null) {
                    try (AudioInputStream ais = AudioSystem.getAudioInputStream(file)) {
                        Mixer mixer = AudioSystem.getMixer(selectedMixerInfo);
                        DataLine.Info info = new DataLine.Info(SourceDataLine.class, ais.getFormat());
                        try (SourceDataLine line = (SourceDataLine) mixer.getLine(info)) {
                            line.open(ais.getFormat());
                            
                            // Apply Volume (GAIN control)
                            if (line.isControlSupported(FloatControl.Type.MASTER_GAIN)) {
                                FloatControl volumeControl = (FloatControl) line.getControl(FloatControl.Type.MASTER_GAIN);
                                float min = volumeControl.getMinimum();
                                float max = volumeControl.getMaximum();
                                float value = min + (max - min) * (configService.getSystemVolume() / 100.0f);
                                volumeControl.setValue(value);
                            }
                            
                            line.start();
                            byte[] buffer = new byte[4096];
                            int read;
                            while ((read = ais.read(buffer)) != -1) {
                                line.write(buffer, 0, read);
                            }
                            line.drain();
                            logger.info("Played (WAV): {}", file.getName());
                            return;
                        }
                    } catch (Exception ex) {
                        logger.warn("Fallback to JavaFX MediaPlayer for {}: {}", file.getName(), ex.getMessage());
                    }
                }

                Platform.runLater(() -> {
                    try {
                        javafx.scene.media.Media media = new javafx.scene.media.Media(file.toURI().toString());
                        if (currentPlayer != null) currentPlayer.stop();
                        currentPlayer = new MediaPlayer(media);
                        currentPlayer.setVolume(configService.getSystemVolume() / 100.0);
                        currentPlayer.play();
                        logger.info("Playing (System): {}", file.getName());
                    } catch (Exception e) {
                        logger.error("Media player failed for {}: {}", file.getName(), e.getMessage());
                    }
                });
            } catch (Exception e) {
                logger.error("Audio playback error for {}: {}", path, e.getMessage());
            }
        }).start();
    }
    
    public void stopAll() {
        if (currentPlayer != null) {
            currentPlayer.stop();
        }
    }

    public void setVolume(double volume) {
        if (currentPlayer != null) {
            Platform.runLater(() -> currentPlayer.setVolume(volume / 100.0));
        }
    }
}

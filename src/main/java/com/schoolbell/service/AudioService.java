package com.schoolbell.service;

import javafx.application.Platform;
import javafx.scene.media.MediaPlayer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.sound.sampled.*;
import java.io.File;

public class AudioService {
    private static final Logger logger = LoggerFactory.getLogger(AudioService.class);
    private static final int FADE_DURATION_MS = 3000;
    
    private MediaPlayer currentPlayer;
    private final ConfigService configService;
    private String currentPlayingTrack = null;
    private javafx.animation.Timeline fadeTimeline;

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
                
                String deviceName = configService.getSelectedAudioDeviceName();
                Mixer.Info selectedMixerInfo = null;
                if (!"Системний за замовчуванням".equals(deviceName)) {
                    for (Mixer.Info info : AudioSystem.getMixerInfo()) {
                        if (info.getName().equals(deviceName)) {
                            selectedMixerInfo = info;
                            break;
                        }
                    }
                }

                // Use faster fade or no fade for alert/error sounds
                boolean isAlert = path.toLowerCase().contains("error") || path.toLowerCase().contains("alert");
                int fadeDuration = isAlert ? 0 : FADE_DURATION_MS;

                // IF WAV and custom device is selected, use Direct Line (Reliable, Low Latency)
                if (path.toLowerCase().endsWith(".wav") && selectedMixerInfo != null) {
                    logger.info("Playing (WAV Direct) on device: {}", deviceName);
                    playWavDirect(file, selectedMixerInfo, fadeDuration);
                    return;
                }

                // Fallback to JavaFX MediaPlayer (System Default only, supports MP3/WAV/M4A)
                final Mixer.Info mixerForLambda = selectedMixerInfo;
                Platform.runLater(() -> {
                    try {
                        javafx.scene.media.Media media = new javafx.scene.media.Media(file.toURI().toString());
                        stopCurrentPlayerImmediate(); 
                        
                        currentPlayingTrack = file.getName();
                        if (mixerForLambda != null) {
                            logger.warn("Custom device '{}' ignored for MP3/M4A. Use .wav for full support. Falling back to System Default.", deviceName);
                        } else {
                            logger.info("Playing (MediaPlayer) on System Default: {}", file.getName());
                        }

                        currentPlayer = new MediaPlayer(media);
                        double targetVolume = configService.getSystemVolume() / 100.0;

                        if (fadeDuration > 0) {
                            currentPlayer.setVolume(0.0);
                            currentPlayer.play();
                            fadeMediaPlayer(targetVolume, fadeDuration);
                        } else {
                            currentPlayer.setVolume(targetVolume);
                            currentPlayer.play();
                        }
                        
                        currentPlayer.setOnEndOfMedia(() -> {
                            currentPlayingTrack = null;
                            currentPlayer = null;
                        });
                        
                    } catch (Exception e) {
                        logger.error("Media player failed for {}: {}", file.getName(), e.getMessage());
                        currentPlayingTrack = null;
                    }
                });
            } catch (Exception e) {
                logger.error("Audio playback error for {}: {}", path, e.getMessage());
                currentPlayingTrack = null;
            }
        }).start();
    }

    private void playWavDirect(File file, Mixer.Info mixerInfo, int fadeMs) {
        try (AudioInputStream ais = AudioSystem.getAudioInputStream(file)) {
            Mixer mixer = AudioSystem.getMixer(mixerInfo);
            DataLine.Info info = new DataLine.Info(SourceDataLine.class, ais.getFormat());
            
            try (SourceDataLine line = (SourceDataLine) mixer.getLine(info)) {
                line.open(ais.getFormat());
                currentPlayingTrack = file.getName();
                
                FloatControl gainControl = null;
                if (line.isControlSupported(FloatControl.Type.MASTER_GAIN)) {
                    gainControl = (FloatControl) line.getControl(FloatControl.Type.MASTER_GAIN);
                }
                
                line.start();
                byte[] buffer = new byte[4096];
                int read;
                long totalPlayed = 0;
                float targetVol = configService.getSystemVolume() / 100.0f;
                float fadeDurationSec = fadeMs / 1000.0f;
                
                while ((read = ais.read(buffer)) != -1) {
                    if (currentPlayingTrack == null) break; 
                    
                    if (gainControl != null) {
                        if (fadeMs > 0 && fadeDurationSec > 0) {
                            float fadePoint = Math.min(1.0f, totalPlayed / (ais.getFormat().getFrameRate() * fadeDurationSec)); 
                            applyGain(gainControl, targetVol * fadePoint);
                        } else {
                            applyGain(gainControl, targetVol);
                        }
                    }
                    
                    line.write(buffer, 0, read);
                    totalPlayed += (read / ais.getFormat().getFrameSize());
                }
                line.drain();
                currentPlayingTrack = null;
            }
        } catch (Exception ex) {
            logger.error("WAV playback error: {}", ex.getMessage());
        }
    }

    private void applyGain(FloatControl gainControl, float volume) {
        float min = gainControl.getMinimum();
        float max = gainControl.getMaximum();
        // Logarithmic volume to linear gain conversion
        float dB = (float) (Math.log10(Math.max(0.0001, volume)) * 20.0);
        gainControl.setValue(Math.max(min, Math.min(max, dB)));
    }

    private void fadeMediaPlayer(double targetVolume, int durationMs) {
        if (currentPlayer == null) return;
        if (fadeTimeline != null) fadeTimeline.stop();

        fadeTimeline = new javafx.animation.Timeline(
            new javafx.animation.KeyFrame(javafx.util.Duration.ZERO, new javafx.animation.KeyValue(currentPlayer.volumeProperty(), currentPlayer.getVolume())),
            new javafx.animation.KeyFrame(javafx.util.Duration.millis(durationMs), new javafx.animation.KeyValue(currentPlayer.volumeProperty(), targetVolume))
        );
        fadeTimeline.play();
    }

    public void stopAll() {
        if (currentPlayingTrack == null) return;

        if (currentPlayer != null) {
            Platform.runLater(() -> {
                if (currentPlayer == null) return;
                if (fadeTimeline != null) fadeTimeline.stop();
                
                fadeTimeline = new javafx.animation.Timeline(
                    new javafx.animation.KeyFrame(javafx.util.Duration.millis(FADE_DURATION_MS), e -> {
                        stopCurrentPlayerImmediate();
                    }, new javafx.animation.KeyValue(currentPlayer.volumeProperty(), 0.0))
                );
                fadeTimeline.play();
            });
        } else {
            currentPlayingTrack = null; // Interrupts WAV thread
        }
    }

    private void stopCurrentPlayerImmediate() {
        if (fadeTimeline != null) fadeTimeline.stop();
        if (currentPlayer != null) {
            currentPlayer.stop();
            currentPlayer = null;
        }
        currentPlayingTrack = null;
    }

    public String getCurrentPlayingTrack() {
        return currentPlayingTrack;
    }

    public void setVolume(double volume) {
        if (currentPlayer != null) {
            Platform.runLater(() -> currentPlayer.setVolume(volume / 100.0));
        }
    }
}

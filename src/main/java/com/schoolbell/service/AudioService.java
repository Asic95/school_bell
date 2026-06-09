package com.schoolbell.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.sound.sampled.*;
import java.io.File;

/**
 * Universal PCM-based Audio Engine.
 * Replaces JavaFX MediaPlayer to provide stable playback on custom audio devices
 * with software-controlled volume and fading.
 */
public class AudioService {
    private static final Logger logger = LoggerFactory.getLogger(AudioService.class);
    private static final int FADE_DURATION_MS = 2500;
    
    private final ConfigService configService;
    private volatile String currentPlayingTrack = null;
    private volatile boolean isStopping = false;
    
    private volatile int globalSessionId = 0;
    private volatile SourceDataLine activeLine = null;

    public AudioService(ConfigService configService) {
        this.configService = configService;
    }

    /**
     * Plays a single audio source (file path or URL).
     */
    public void playAudioFile(String path) {
        playAudioFile(path, null);
    }

    public void playAudioFile(String path, String displayName) {
        if (path == null || path.isEmpty()) return;
        
        if (path.startsWith("http://") || path.startsWith("https://")) {
            playStream(path, displayName);
        } else {
            playPlaylist(java.util.List.of(new File(path)), false);
        }
    }

    private synchronized void playStream(String url, String displayName) {
        stopImmediate(); // Increment session and kill previous line
        int sessionId = ++globalSessionId;

        new Thread(() -> {
            try {
                Mixer.Info mixerInfo = getSelectedMixerInfo();
                currentPlayingTrack = displayName != null ? displayName : "Радіо: " + url;
                isStopping = false;

                logger.info("Stream [Session {}]: starting '{}' ({})", sessionId, url, currentPlayingTrack);

                try (AudioInputStream baseStream = getCleanAudioStreamFromUrl(url)) {
                    AudioFormat targetFormat = new AudioFormat(AudioFormat.Encoding.PCM_SIGNED, 44100, 16, 2, 4, 44100, false);
                    
                    AudioInputStream finalStream = null;
                    try {
                        finalStream = AudioSystem.getAudioInputStream(targetFormat, baseStream);
                    } catch (Exception e) {
                        try {
                            AudioInputStream intermediate = AudioSystem.getAudioInputStream(AudioFormat.Encoding.PCM_SIGNED, baseStream);
                            finalStream = AudioSystem.getAudioInputStream(targetFormat, intermediate);
                        } catch (Exception e2) {
                            logger.error("No PCM conversion path for stream: {}", e2.getMessage());
                        }
                    }

                    if (finalStream != null && sessionId == globalSessionId) {
                        try {
                            playInternal(finalStream, mixerInfo, currentPlayingTrack, false, sessionId);
                        } finally {
                            if (finalStream != baseStream) finalStream.close();
                        }
                    }
                } catch (Exception e) {
                    logger.error("PCM Engine failed for stream: {}", e.getMessage());
                }
            } catch (Exception e) {
                logger.error("Stream thread critical error: {}", e.getMessage());
            } finally {
                if (sessionId == globalSessionId) {
                    currentPlayingTrack = null;
                }
                isStopping = false;
                logger.info("Stream thread [Session {}] finished.", sessionId);
            }
        }, "AudioEngine-Stream-Thread-" + sessionId).start();
    }

    /**
     * Plays a list of audio files sequentially.
     */
    public synchronized void playPlaylist(java.util.List<File> files, boolean loop) {
        if (files == null || files.isEmpty()) return;

        stopImmediate();
        int sessionId = ++globalSessionId;

        new Thread(() -> {
            try {
                Mixer.Info mixerInfo = getSelectedMixerInfo();
                
                do {
                    for (File file : files) {
                        if (isStopping || sessionId != globalSessionId) break;
                        
                        if (!file.exists()) {
                            logger.error("Audio file not found: {}", file.getPath());
                            continue;
                        }

                        logger.info("Playlist [Session {}]: starting track '{}'", sessionId, file.getName());
                        currentPlayingTrack = file.getName(); 

                        try (AudioInputStream baseStream = getCleanAudioStream(file)) {
                            AudioFormat targetFormat = new AudioFormat(AudioFormat.Encoding.PCM_SIGNED, 44100, 16, 2, 4, 44100, false);
                            AudioInputStream finalStream = null;
                            
                            try {
                                finalStream = AudioSystem.getAudioInputStream(targetFormat, baseStream);
                            } catch (Exception e) {
                                try {
                                    AudioInputStream intermediate = AudioSystem.getAudioInputStream(AudioFormat.Encoding.PCM_SIGNED, baseStream);
                                    finalStream = AudioSystem.getAudioInputStream(targetFormat, intermediate);
                                } catch (Exception e2) {
                                    logger.error("No PCM conversion path for '{}': {}", file.getName(), e2.getMessage());
                                }
                            }

                            if (finalStream != null && sessionId == globalSessionId) {
                                try {
                                    boolean isAlert = file.getPath().toLowerCase().contains("error") || file.getPath().toLowerCase().contains("alert");
                                    playInternal(finalStream, mixerInfo, file.getName(), isAlert, sessionId);
                                } finally {
                                    if (finalStream != baseStream) finalStream.close();
                                }
                            }
                        } catch (Exception e) {
                            logger.error("PCM Engine failed for track '{}': {}", file.getName(), e.getMessage());
                        }

                        if (isStopping || sessionId != globalSessionId) break;
                    }
                } while (loop && !isStopping && sessionId == globalSessionId);
                
            } catch (Exception e) {
                logger.error("Playlist thread critical error: {}", e.getMessage());
            } finally {
                if (sessionId == globalSessionId) {
                    currentPlayingTrack = null;
                }
                isStopping = false;
                logger.info("Playlist thread [Session {}] finished.", sessionId);
            }
        }, "AudioEngine-Playlist-Thread-" + sessionId).start();
    }

    private void playInternal(AudioInputStream stream, Mixer.Info mixerInfo, String trackName, boolean isAlert, int sessionId) throws Exception {
        AudioFormat format = stream.getFormat();
        DataLine.Info info = new DataLine.Info(SourceDataLine.class, format);
        
        Mixer mixer = mixerInfo != null ? AudioSystem.getMixer(mixerInfo) : null;
        try (SourceDataLine line = (SourceDataLine) (mixer != null ? mixer.getLine(info) : AudioSystem.getLine(info))) {
            line.open(format);
            line.start();
            
            activeLine = line;
            currentPlayingTrack = trackName;
            isStopping = false;
            
            byte[] buffer = new byte[8192];
            int bytesRead;
            
            float targetSystemVolume = configService.getSystemVolume() / 100.0f;
            long frameCounter = 0;
            long fadeFrames = (long) (format.getFrameRate() * (FADE_DURATION_MS / 1000.0));
            
            logger.info("PCM Session {} playing: {}", sessionId, trackName);

            while ((bytesRead = stream.read(buffer)) != -1 && sessionId == globalSessionId) {
                if (isStopping && !isAlert) {
                    fadeOutAndStop(line, stream, buffer, targetSystemVolume, format);
                    return;
                }

                float volumeMultiplier = targetSystemVolume;
                if (!isAlert && frameCounter < fadeFrames) {
                    volumeMultiplier *= (float) frameCounter / fadeFrames;
                }
                
                applySoftwareVolume(buffer, bytesRead, volumeMultiplier);
                line.write(buffer, 0, bytesRead);
                frameCounter += (bytesRead / format.getFrameSize());
            }
            
            line.drain();
            line.stop();
        } finally {
            if (sessionId == globalSessionId) {
                activeLine = null;
            }
        }
    }

    private void fadeOutAndStop(SourceDataLine line, AudioInputStream stream, byte[] buffer, float targetVol, AudioFormat format) throws Exception {
        long fadeOutFrames = (long) (format.getFrameRate() * (FADE_DURATION_MS / 1000.0));
        int bytesRead;
        
        for (long f = fadeOutFrames; f > 0; f -= (buffer.length / format.getFrameSize())) {
            if ((bytesRead = stream.read(buffer)) == -1) break;
            float fadeMultiplier = (float) f / fadeOutFrames;
            applySoftwareVolume(buffer, bytesRead, targetVol * fadeMultiplier);
            line.write(buffer, 0, bytesRead);
        }
        
        line.stop();
        currentPlayingTrack = null;
    }

    /**
     * Manipulates PCM bytes to apply volume scaling.
     * Assumes 16-bit PCM Signed Little Endian.
     */
    private void applySoftwareVolume(byte[] buffer, int length, float volume) {
        if (volume >= 0.999f) return; 
        if (volume <= 0.001f) {
            for (int i = 0; i < length; i++) buffer[i] = 0;
            return;
        }

        for (int i = 0; i < length; i += 2) {
            int sample = (short) ((buffer[i] & 0xFF) | (buffer[i + 1] << 8));
            int newSample = (int) (sample * volume);
            
            // Clipping protection
            if (newSample > 32767) newSample = 32767;
            else if (newSample < -32768) newSample = -32768;
            
            buffer[i] = (byte) (newSample & 0xFF);
            buffer[i + 1] = (byte) ((newSample >> 8) & 0xFF);
        }
    }

    private Mixer.Info getSelectedMixerInfo() {
        String deviceName = configService.getSelectedAudioDeviceName();
        if (deviceName == null || "Системний за замовчуванням".equals(deviceName)) return null;
        
        for (Mixer.Info info : AudioSystem.getMixerInfo()) {
            if (info.getName().equals(deviceName)) return info;
        }
        return null;
    }

    /**
     * Initiates a graceful fade-out stop.
     */
    public void stopAll() {
        if (currentPlayingTrack != null) {
            isStopping = true;
        }
    }

    /**
     * Immediately kills playback thread and closes lines.
     */
    public synchronized void stopImmediate() {
        globalSessionId++; // Invalidate all current threads
        currentPlayingTrack = null;
        isStopping = false;
        
        if (activeLine != null) {
            try {
                activeLine.stop();
                activeLine.flush();
                activeLine.close();
            } catch (Exception ignored) {}
            activeLine = null;
        }
        
        try {
            Thread.sleep(20); 
        } catch (InterruptedException ignored) {}
    }

    public String getCurrentPlayingTrack() {
        return currentPlayingTrack;
    }

    private AudioInputStream getCleanAudioStream(File file) throws Exception {
        try {
            return AudioSystem.getAudioInputStream(file);
        } catch (Exception e) {
            logger.warn("Standard opening failed for '{}': {}. Entering advanced recovery...", file.getName(), e.getMessage());
        }

        long offset = findAudioOffset(file);
        java.io.InputStream fis = new java.io.FileInputStream(file);
        if (offset > 0) fis.skip(offset);
        java.io.BufferedInputStream bis = new java.io.BufferedInputStream(fis, 1024 * 1024);
        try {
            return AudioSystem.getAudioInputStream(bis);
        } catch (Exception e) {
            bis.close();
            throw e;
        }
    }

    private AudioInputStream getCleanAudioStreamFromUrl(String url) throws Exception {
        java.net.URL radioUrl = new java.net.URL(url);
        java.net.URLConnection conn = radioUrl.openConnection();
        conn.setRequestProperty("User-Agent", "Mozilla/5.0");
        conn.setConnectTimeout(10000);
        conn.setReadTimeout(10000);

        java.io.InputStream is = conn.getInputStream();
        java.io.BufferedInputStream bis = new java.io.BufferedInputStream(is, 1024 * 1024);
        try {
            return AudioSystem.getAudioInputStream(bis);
        } catch (Exception e) {
            bis.close();
            throw e;
        }
    }

    private long findAudioOffset(File file) {
        try (java.io.RandomAccessFile raf = new java.io.RandomAccessFile(file, "r")) {
            byte[] header = new byte[10];
            int read = raf.read(header);
            long offset = 0;
            if (read == 10 && header[0] == 'I' && header[1] == 'D' && header[2] == '3') {
                int size = ((header[6] & 0x7F) << 21) | ((header[7] & 0x7F) << 14) | ((header[8] & 0x7F) << 7) | (header[9] & 0x7F);
                offset = size + 10;
            }
            raf.seek(offset);
            long searchLimit = Math.min(raf.length(), offset + 1024 * 1024); 
            while (raf.getFilePointer() < searchLimit) {
                int b = raf.read();
                if (b == -1) break;
                if (b == 0xFF) {
                    int next = raf.read();
                    if (next != -1 && (next & 0xE0) == 0xE0) return raf.getFilePointer() - 2;
                }
            }
            return offset;
        } catch (Exception e) { return 0; }
    }
}

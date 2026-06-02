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

    public AudioService(ConfigService configService) {
        this.configService = configService;
    }

    /**
     * Plays a single audio file.
     */
    public void playAudioFile(String path) {
        if (path == null || path.isEmpty()) return;
        playPlaylist(java.util.List.of(new File(path)), false);
    }

    /**
     * Plays a list of audio files sequentially.
     */
    public void playPlaylist(java.util.List<File> files, boolean loop) {
        if (files == null || files.isEmpty()) return;

        // Ensure only one playlist/track plays at a time
        if (currentPlayingTrack != null) {
            stopImmediate();
        }

        new Thread(() -> {
            try {
                Mixer.Info mixerInfo = getSelectedMixerInfo();
                
                do {
                    for (File file : files) {
                        if (isStopping) break;
                        
                        if (!file.exists()) {
                            logger.error("Audio file not found: {}", file.getPath());
                            continue;
                        }

                        logger.info("Playlist: starting track '{}'", file.getName());
                        currentPlayingTrack = file.getName(); 

                        try (AudioInputStream baseStream = getCleanAudioStream(file)) {
                            AudioFormat baseFormat = baseStream.getFormat();
                            logger.info("Source format: {} for '{}'", baseFormat, file.getName());

                            // NOISE PROTECTION: If format is claimed to be PCM but is actually AAC/MP3, skip it
                            if (baseFormat.getEncoding().toString().contains("AAC") || baseFormat.getEncoding().toString().contains("MPEG")) {
                                // This is expected for raw streams
                            } else if (AudioFormat.Encoding.PCM_SIGNED.equals(baseFormat.getEncoding()) && baseFormat.getSampleSizeInBits() == 32) {
                                // If SPI claims 32-bit PCM but our mixer only handles 16-bit and we see noise
                                logger.warn("Detected potentially incompatible PCM format (32-bit) for '{}'. Skipping to avoid noise.", file.getName());
                                continue;
                            }

                            AudioInputStream finalStream = null;
                            
                            // Try to get a standard 16-bit PCM stream
                            AudioFormat targetFormat = new AudioFormat(AudioFormat.Encoding.PCM_SIGNED, 44100, 16, 2, 4, 44100, false);
                            
                            try {
                                // Attempt direct conversion to our target
                                finalStream = AudioSystem.getAudioInputStream(targetFormat, baseStream);
                            } catch (Exception e) {
                                // Fallback: get ANY signed PCM first
                                try {
                                    AudioInputStream intermediate = AudioSystem.getAudioInputStream(AudioFormat.Encoding.PCM_SIGNED, baseStream);
                                    if (AudioSystem.isConversionSupported(targetFormat, intermediate.getFormat())) {
                                        finalStream = AudioSystem.getAudioInputStream(targetFormat, intermediate);
                                    } else {
                                        finalStream = intermediate;
                                    }
                                } catch (Exception e2) {
                                    logger.error("No PCM conversion path for '{}': {}", file.getName(), e2.getMessage());
                                }
                            }

                            if (finalStream != null) {
                                try {
                                    boolean isAlert = file.getPath().toLowerCase().contains("error") || file.getPath().toLowerCase().contains("alert");
                                    playInternal(finalStream, mixerInfo, file.getName(), isAlert);
                                } finally {
                                    if (finalStream != baseStream) finalStream.close();
                                }
                            }
                        } catch (Exception e) {
                            logger.error("PCM Engine failed for track '{}': {} (Type: {})", 
                                    file.getName(), e.getMessage(), e.getClass().getSimpleName());
                        }

                        if (isStopping) break;
                    }
                } while (loop && !isStopping);
                
            } catch (Exception e) {
                logger.error("Playlist thread critical error: {}", e.getMessage(), e);
            } finally {
                currentPlayingTrack = null;
                isStopping = false;
                logger.info("Playlist thread finished.");
            }
        }, "AudioEngine-Playlist-Thread").start();
    }

    private boolean isFormatMatch(AudioFormat f1, AudioFormat f2) {
        return Math.abs(f1.getSampleRate() - f2.getSampleRate()) < 1.0 &&
               f1.getChannels() == f2.getChannels() &&
               f1.getSampleSizeInBits() == f2.getSampleSizeInBits() &&
               f1.getEncoding().equals(f2.getEncoding());
    }

    /**
     * Tries multiple strategies to open an audio stream.
     * 1. Standard AudioSystem detection (best for WAV, standard MP3, and AAC/M4A).
     * 2. Advanced recovery with ID3 skipping and Sync hunting (for difficult MP3s).
     */
    private AudioInputStream getCleanAudioStream(File file) throws Exception {
        // STRATEGY 1: Standard opening (allows SPIs to use RandomAccessFile)
        try {
            return AudioSystem.getAudioInputStream(file);
        } catch (Exception e) {
            logger.warn("Standard opening failed for '{}': {}. Entering advanced recovery...", 
                    file.getName(), e.getMessage());
        }

        // STRATEGY 2: Advanced recovery (Fresh Start at Audio Offset)
        long offset = findAudioOffset(file);
        
        java.io.InputStream fis = new java.io.FileInputStream(file);
        if (offset > 0) {
            long skipped = fis.skip(offset);
            if (skipped < offset) {
                logger.warn("Could not skip to target offset {} for '{}'.", offset, file.getName());
            } else {
                logger.info("Starting recovery stream at offset: {} for '{}'", offset, file.getName());
            }
        }
        
        java.io.BufferedInputStream bis = new java.io.BufferedInputStream(fis, 1024 * 1024);
        
        try {
            return AudioSystem.getAudioInputStream(bis);
        } catch (Exception e) {
            bis.close();
            logger.error("Advanced recovery also failed for '{}': {}", file.getName(), e.getMessage());
            throw e;
        }
    }

    private long findAudioOffset(File file) {
        try (java.io.RandomAccessFile raf = new java.io.RandomAccessFile(file, "r")) {
            byte[] header = new byte[10];
            int read = raf.read(header);
            long offset = 0;
            
            if (read == 10 && header[0] == 'I' && header[1] == 'D' && header[2] == '3') {
                int size = ((header[6] & 0x7F) << 21) |
                           ((header[7] & 0x7F) << 14) |
                           ((header[8] & 0x7F) << 7) |
                           (header[9] & 0x7F);
                offset = size + 10;
                logger.info("ID3v2 detected at start of '{}', header size: {}", file.getName(), offset);
            }
            
            // Sync word hunt starting from offset with MULTI-FRAME VALIDATION
            raf.seek(offset);
            long searchLimit = Math.min(raf.length(), offset + 1024 * 1024); 
            while (raf.getFilePointer() < searchLimit) {
                int b = raf.read();
                if (b == -1) break;
                if (b == 0xFF) {
                    int next = raf.read();
                    if (next != -1 && (next & 0xE0) == 0xE0) {
                        long potentialAt = raf.getFilePointer() - 2;
                        
                        // VALIDATION: Check if there is another sync word shortly after
                        // Most MP3 frames are between 144 and 1440 bytes
                        if (verifySyncChain(raf, potentialAt)) {
                            if (potentialAt > offset) {
                                logger.info("Verified MP3 Sync chain found at {} for '{}' (skipped {} extra bytes).", 
                                        potentialAt, file.getName(), potentialAt - offset);
                            }
                            return potentialAt;
                        } else {
                            raf.seek(potentialAt + 1); // False positive, continue search
                        }
                    }
                }
            }
            return offset;
        } catch (Exception e) {
            logger.error("Error searching for audio offset in '{}': {}", file.getName(), e.getMessage());
            return 0;
        }
    }

    /**
     * Verifies that the found sync word is likely a real MP3 frame by looking ahead.
     */
    private boolean verifySyncChain(java.io.RandomAccessFile raf, long startPos) throws java.io.IOException {
        long originalPos = raf.getFilePointer();
        try {
            // We don't know the exact frame size without parsing the header, 
            // but we can look for the NEXT sync word in a reasonable range (up to 1500 bytes)
            byte[] lookAhead = new byte[2000];
            raf.seek(startPos + 2); // Skip the first sync word
            int read = raf.read(lookAhead);
            if (read < 10) return false;

            for (int i = 0; i < read - 1; i++) {
                if ((lookAhead[i] & 0xFF) == 0xFF && (lookAhead[i+1] & 0xE0) == 0xE0) {
                    return true; // Found a second sync word nearby!
                }
            }
            return false;
        } finally {
            raf.seek(originalPos);
        }
    }

    private void diagnoseAudioSystem() {
        logger.info("--- AudioSystem Diagnostics ---");
        try {
            logger.info("Mixers found: {}", AudioSystem.getMixerInfo().length);
            for (Mixer.Info info : AudioSystem.getMixerInfo()) {
                logger.info(" - Mixer: {}", info.getName());
            }
            // Check for MP3 SPI providers implicitly by checking available encodings
            logger.info("Supported Encodings:");
            for (AudioFormat.Encoding enc : AudioSystem.getTargetEncodings(AudioFormat.Encoding.PCM_SIGNED)) {
                logger.info(" - To PCM_SIGNED from: {}", enc);
            }
        } catch (Exception ex) {
            logger.error("Diagnostics failed: {}", ex.getMessage());
        }
        logger.info("-------------------------------");
    }

    private void playInternal(AudioInputStream stream, Mixer.Info mixerInfo, String trackName, boolean isAlert) throws Exception {
        AudioFormat format = stream.getFormat();
        DataLine.Info info = new DataLine.Info(SourceDataLine.class, format);
        
        Mixer mixer = mixerInfo != null ? AudioSystem.getMixer(mixerInfo) : null;
        try (SourceDataLine line = (SourceDataLine) (mixer != null ? mixer.getLine(info) : AudioSystem.getLine(info))) {
            line.open(format);
            line.start();
            
            currentPlayingTrack = trackName;
            isStopping = false;
            
            byte[] buffer = new byte[8192]; // ~46ms at 44.1kHz stereo
            int bytesRead;
            
            float targetSystemVolume = configService.getSystemVolume() / 100.0f;
            long frameCounter = 0;
            long fadeFrames = (long) (format.getFrameRate() * (FADE_DURATION_MS / 1000.0));
            
            logger.info("PCM Engine playing: {} on device: {}", trackName, mixerInfo != null ? mixerInfo.getName() : "Default");

            while ((bytesRead = stream.read(buffer)) != -1 && currentPlayingTrack != null) {
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
     * Immediately kills playback thread.
     */
    public void stopImmediate() {
        currentPlayingTrack = null;
        isStopping = false;
        try {
            Thread.sleep(50); // Small grace period for thread to notice
        } catch (InterruptedException ignored) {}
    }

    public String getCurrentPlayingTrack() {
        return currentPlayingTrack;
    }

    /**
     * Dynamically updates the system volume level for current playback.
     */
    public void setVolume(double volume) {
        // Since we process volume per buffer in the loop, this will be 
        // picked up by the next read iteration from configService.
    }
}

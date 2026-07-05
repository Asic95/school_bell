package com.schoolbell.service;

import ws.schild.jave.process.ffmpeg.DefaultFFMPEGLocator;

import java.io.File;
import java.io.IOException;

public class FFmpegAudioHelper {
    private static final DefaultFFMPEGLocator locator = new DefaultFFMPEGLocator();

    /**
     * Швидко конвертує будь-який аудіо/відео файл у чистий PCM WAV.
     */
    public static File transcodeToWav(File inputFile) throws Exception {
        File tempWav = File.createTempFile("schoolbell_track_", ".wav");
        tempWav.deleteOnExit();

        // Налаштування процесу FFmpeg
        String ffmpegPath = locator.getExecutablePath();
        ProcessBuilder pb = new ProcessBuilder(
            ffmpegPath,
            "-y",                     // Перезаписати якщо файл існує
            "-i", inputFile.getAbsolutePath(),
            "-vn",                    // Вимкнути відео
            "-acodec", "pcm_s16le",   // Кодек PCM 16-bit
            "-ar", "44100",           // Частота 44.1kHz
            "-ac", "2",               // Стерео
            tempWav.getAbsolutePath()
        );
        pb.redirectError(ProcessBuilder.Redirect.DISCARD);

        Process process = pb.start();
        int exitCode = process.waitFor();
        if (exitCode != 0) {
            throw new IOException("FFmpeg transcoding failed with exit code: " + exitCode);
        }
        return tempWav;
    }

    /**
     * Створює процес для онлайн-радіо з виводом PCM в stdout.
     */
    public static Process startRadioStreamProcess(String streamUrl) throws IOException {
        String ffmpegPath = locator.getExecutablePath();
        ProcessBuilder pb = new ProcessBuilder(
            ffmpegPath,
            "-reconnect", "1",
            "-reconnect_streamed", "1",
            "-reconnect_delay_max", "5", // Автоматичне перепідключення при збоях мережі
            "-i", streamUrl,
            "-vn",
            "-f", "s16le",               // Формат виводу - raw PCM 16-bit Little Endian
            "-acodec", "pcm_s16le",
            "-ar", "44100",
            "-ac", "2",
            "-"                          // Виводити у stdout (pipe)
        );
        pb.redirectError(ProcessBuilder.Redirect.DISCARD);
        return pb.start();
    }
}

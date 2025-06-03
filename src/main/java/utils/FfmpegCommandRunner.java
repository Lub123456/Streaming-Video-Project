package utils;

import java.io.IOException;

public class FfmpegCommandRunner {

    public static void convert(String inputPath, String outputPath, String resolution) {
        // Exemple : "ffmpeg -i input -s 640x360 output"
        String scale = switch (resolution) {
            case "240p" -> "426x240";
            case "360p" -> "640x360";
            case "480p" -> "854x480";
            case "720p" -> "1280x720";
            case "1080p" -> "1920x1080";
            default -> "";
        };

        ProcessBuilder builder = new ProcessBuilder(
                "ffmpeg", "-y", "-i", inputPath, "-s", scale, outputPath
        );

        builder.inheritIO(); // pour voir les logs ffmpeg
        try {
            Process process = builder.start();
            process.waitFor();
        } catch (IOException | InterruptedException e) {
            System.err.println("FFMPEG conversion error: " + e.getMessage());
        }
    }
}

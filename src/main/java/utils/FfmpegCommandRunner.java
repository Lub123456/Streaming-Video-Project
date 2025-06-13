package utils;

import java.io.IOException;

public class FfmpegCommandRunner {

    // Converts a video file to a specified resolution using ffmpeg
    public static void convert(String inputPath, String outputPath, String resolution) {
        String scale = switch (resolution) {
            case "240p" -> "-2:240";
            case "360p" -> "-2:360";
            case "480p" -> "-2:480";
            case "720p" -> "-2:720";
            case "1080p" -> "-2:1080";
            default -> "";
        };

        ProcessBuilder builder = new ProcessBuilder(
                "ffmpeg", "-y", "-i", inputPath, "-c:v", "libx264", "-crf", "25", "-vf", "scale="+scale, outputPath
        );

        builder.inheritIO();
        try {
            Process process = builder.start();
            process.waitFor();
        } catch (IOException | InterruptedException e) {
            System.err.println("FFMPEG conversion error: " + e.getMessage());
        }
    }
}

package client;

import java.io.InputStream;
import java.net.URL;
import java.util.logging.Logger;

public class SpeedTester {

    private static final Logger logger = Logger.getLogger(SpeedTester.class.getName());

     // Performs a download speed test from a remote file (HTTP).
     // Return estimated download speed in Mbps
    public static double measureDownloadSpeed() {
        final String testFileUrl = "http://nbg1-speed.hetzner.com/100MB.bin"; // Public test file
        final int bufferSize = 1024 * 8; // 8 KB
        final int maxDownloadBytes = 5 * 1024 * 1024; // Limit to 5 MB to avoid overuse

        try {
            logger.info("Starting download speed test...");
            URL url = new URL(testFileUrl);
            InputStream in = url.openStream();
            byte[] buffer = new byte[bufferSize];

            long startTime = System.nanoTime();
            int bytesRead;
            int totalBytes = 0;

            while ((bytesRead = in.read(buffer)) != -1 && totalBytes < maxDownloadBytes) {
                totalBytes += bytesRead;
            }

            long endTime = System.nanoTime();
            double durationSeconds = (endTime - startTime) / 1_000_000_000.0;
            in.close();

            double mbits = (totalBytes * 8.0) / 1_000_000; // bits -> Mbits
            double speedMbps = Math.round(mbits / durationSeconds * 100.0) / 100.0; // Round to 2 decimal places

            logger.info("Download speed test completed: " + speedMbps + " Mbps");
            return speedMbps;

        } catch (Exception e) {
            System.err.println("Speed test error: " + e.getMessage());
            return 1.0;
        }
    }
}

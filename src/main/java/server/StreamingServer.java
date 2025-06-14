package server;

import shared.VideoFile;
import utils.FfmpegCommandRunner;
import utils.LoggerConfig;
import shared.Protocol;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.*;
import java.util.logging.Logger;

public class StreamingServer {

    private static final Logger logger = Logger.getLogger(StreamingServer.class.getName());
    private static final String VIDEO_DIR = "videos/";
    private static final List<String> FORMATS = List.of("mp4", "avi", "mkv");
    private static final List<String> RESOLUTIONS_ORDERED = List.of("240p", "360p", "480p", "720p", "1080p");
    private static final int PORT = 9090;
    private Process ffmpegProcess;

    private List<VideoFile> availableFiles = new ArrayList<>();

    private int getResolutionIndex(String resolution) {
        return RESOLUTIONS_ORDERED.indexOf(resolution);
    }

    public static void main(String[] args) {
        utils.LoggerConfig.configureSimpleLogging();
        StreamingServer server = new StreamingServer();
        server.run();
    }

    // Starts the streaming server and processes video files
    public void run() {
        logger.info("Starting Streaming Server...");

        scanAndProcessVideos();

        while (true) {
            try (ServerSocket serverSocket = new ServerSocket(PORT)) {
                logger.info("Waiting for client on port " + PORT + "...");
                Socket clientSocket = serverSocket.accept();
                logger.info("Client connected from: " + clientSocket.getInetAddress());

                handleClient(clientSocket);
                System.out.println("");

            } catch (IOException e) {
                logger.severe("Server error: " + e.getMessage());
            }
        }
    }

    // Scans the video directory, processes existing files, and generates missing resolutions
    private void scanAndProcessVideos() {
        // Detection of existing video files
        File dir = new File(VIDEO_DIR);
        Map<String, Map<String, Set<String>>> existing = new HashMap<>();
        for (File file : Objects.requireNonNull(dir.listFiles())) {
            String[] parts = file.getName().split("[-.]");
            if (parts.length >= 3) {
                String format = parts[2];
                if (!FORMATS.contains(format)) continue;

                String name = parts[0];
                String resolution = parts[1];

                existing.putIfAbsent(name, new HashMap<>());
                existing.get(name).putIfAbsent(resolution, new HashSet<>());
                existing.get(name).get(resolution).add(format);

                availableFiles.add(new VideoFile(name, format, resolution));
            }
        }

        // Generate missing resolutions for each video
        for (String name : existing.keySet()) {
            int maxResIndex = existing.get(name).keySet().stream()
                    .mapToInt(this::getResolutionIndex)
                    .max()
                    .orElse(-1);

            for (int i = 0; i <= maxResIndex; i++) {
                String res = RESOLUTIONS_ORDERED.get(i);
                for (String fmt : FORMATS) {
                    boolean alreadyExists = existing.get(name).containsKey(res)
                            && existing.get(name).get(res).contains(fmt);
                    if (!alreadyExists) {
                        Optional<VideoFile> source = availableFiles.stream()
                                .filter(f -> f.getName().equals(name)
                                        && getResolutionIndex(f.getResolution()) == maxResIndex)
                                .findFirst();

                        if (source.isPresent()) {
                            String inputFile = VIDEO_DIR + source.get().getFilename();
                            String outputFile = VIDEO_DIR + name + "-" + res + "." + fmt;
                            logger.info("Creation : " + outputFile);
                            FfmpegCommandRunner.convert(inputFile, outputFile, res);
                            availableFiles.add(new VideoFile(name, fmt, res));
                        }
                    }
                }
            }
        }

    }

    // Handles client connections and processes requests
    private void handleClient(Socket socket) {
        try (
                ObjectInputStream input = new ObjectInputStream(socket.getInputStream());
                ObjectOutputStream output = new ObjectOutputStream(socket.getOutputStream())
        ) {
            Object command = input.readObject();

            if ("play_request".equals(command)) {
                VideoFile file = (VideoFile) input.readObject();
                String protoStr = (String) input.readObject();
                Protocol protocol = Protocol.valueOf(protoStr);
                logger.info("Streaming requested: " + file.getFilename() + " via " + protocol);
                startStreaming(file, protocol);

                // Wait for the client to close the connection
                try {
                    socket.getInputStream().read();
                } catch (IOException ignored) {}

                // Stop the FFMPEG process when the client disconnects
                if (ffmpegProcess != null && ffmpegProcess.isAlive()) {
                    ffmpegProcess.destroy();
                    logger.info("FFMPEG process stopped.");
                }

            } else if (command instanceof String format) {
                double bitrateMbps = (Double) input.readObject();
                logger.info("Client requested format=" + format + ", bitrate=" + bitrateMbps + " Mbps");

                List<VideoFile> suitable = getFilesByFormatAndBitrate(format, bitrateMbps);
                output.writeObject(suitable);
                output.flush();
            }


        } catch (IOException | ClassNotFoundException e) {
            logger.severe("Client communication error: " + e.getMessage());
            // Arrêter ffmpeg en cas d'erreur/fermeture
            if (ffmpegProcess != null && ffmpegProcess.isAlive()) {
                ffmpegProcess.destroy();
                logger.info("FFMPEG process stopped (on error).");
            }
        }
    }


    private List<VideoFile> getFilesByFormatAndBitrate(String format, double bitrate) {
        Map<String, Double> resolutionBitrates = Map.of(
                "240p", 0.7,
                "360p", 1.0,
                "480p", 2.0,
                "720p", 4.0,
                "1080p", 6.0
        );

        List<VideoFile> result = new ArrayList<>();
        for (VideoFile file : availableFiles) {
            if (file.getFormat().equals(format)) {
                double required = resolutionBitrates.getOrDefault(file.getResolution(), Double.MAX_VALUE);
                if (bitrate >= required) {
                    result.add(file);
                }
            }
        }
        return result;
    }

    // Starts the FFMPEG streaming process based on the requested video file and protocol
    private void startStreaming(VideoFile file, Protocol protocol) {
        String filePath = VIDEO_DIR + file.getFilename();
        String command = switch (protocol) {
            case TCP -> "ffmpeg -re -i " + filePath + " -f mpegts tcp://localhost:8888?listen";
            case UDP -> "ffmpeg -re -i " + filePath + " -f mpegts udp://localhost:8888";
            case RTP_UDP -> "ffmpeg -re -i " + filePath +
                    " -map 0:v:0 -c:v libx264 -f rtp rtp://localhost:5004" +
                    " -map 0:a:0 -c:a aac -f rtp rtp://localhost:5006" +
                    " -sdp_file sdp/stream_rtp.sdp";
        };

        try {
            ProcessBuilder pb = new ProcessBuilder(command.split(" "));
            pb.inheritIO();
            ffmpegProcess = pb.start();
            logger.info("FFMPEG command: " + command);

            new Thread(() -> {
                try {
                    ffmpegProcess.waitFor();
                    logger.info("End of FFMPEG stream");
                } catch (InterruptedException e) {
                    logger.warning("FFMPEG interrupted: " + e.getMessage());
                }
            }).start();

        } catch (IOException e) {
            logger.severe("FFMPEG launch failed: " + e.getMessage());
        }
    }
}

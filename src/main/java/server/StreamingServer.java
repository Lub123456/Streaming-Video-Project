package server;

import shared.VideoFile;
import utils.FfmpegCommandRunner;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.*;
import java.util.logging.Logger;

public class StreamingServer {

    private static final Logger logger = Logger.getLogger(StreamingServer.class.getName());
    private static final String VIDEO_DIR = "videos/";
    private static final List<String> FORMATS = List.of("mp4", "avi", "mkv");
    private static final List<String> RESOLUTIONS = List.of("240p", "360p", "480p", "720p", "1080p");
    private static final int PORT = 9090;

    private List<VideoFile> availableFiles = new ArrayList<>();

    public static void main(String[] args) {
        StreamingServer server = new StreamingServer();
        server.run();
    }

    public void run() {
        logger.info("Starting Streaming Server...");
        // à enlever à la fin
        Scanner sc = new Scanner(System.in);
        System.out.print("Start videos processing ? (y/n) : ");
        if (sc.nextLine().equalsIgnoreCase("y")) {
            scanAndProcessVideos();
        }

        // à remettre à la fin
        //scanAndProcessVideos();

        try (ServerSocket serverSocket = new ServerSocket(PORT)) {
            logger.info("Waiting for client on port " + PORT + "...");
            Socket clientSocket = serverSocket.accept();
            logger.info("Client connected from: " + clientSocket.getInetAddress());

            handleClient(clientSocket);

        } catch (IOException e) {
            logger.severe("Server error: " + e.getMessage());
        }
    }

    private void scanAndProcessVideos() {
        File dir = new File(VIDEO_DIR);
        if (!dir.exists() || !dir.isDirectory()) {
            logger.severe("Video directory not found: " + VIDEO_DIR);
            return;
        }

        Map<String, Set<String>> foundFiles = new HashMap<>();

        for (File file : Objects.requireNonNull(dir.listFiles())) {
            String filename = file.getName();
            String[] parts = filename.split("[-.]");
            if (parts.length >= 3) {
                String name = parts[0];
                String resolution = parts[1];
                String format = parts[2];

                foundFiles.putIfAbsent(name, new HashSet<>());
                foundFiles.get(name).add(resolution + "." + format);

                availableFiles.add(new VideoFile(name, format, resolution));
            }
        }

        // Génération des fichiers manquants
        for (String name : foundFiles.keySet()) {
            for (String res : RESOLUTIONS) {
                for (String fmt : FORMATS) {
                    String fileKey = res + "." + fmt;
                    if (!foundFiles.get(name).contains(fileKey)) {
                        // Vérifier si une résolution source est disponible
                        Optional<VideoFile> source = availableFiles.stream()
                                .filter(f -> f.getName().equals(name))
                                .findFirst();

                        if (source.isPresent()) {
                            String inputFile = VIDEO_DIR + source.get().getFilename();
                            String outputFile = VIDEO_DIR + name + "-" + res + "." + fmt;

                            logger.info("Generating missing file: " + outputFile);
                            FfmpegCommandRunner.convert(inputFile, outputFile, res);
                            availableFiles.add(new VideoFile(name, fmt, res));
                        }
                    }
                }
            }
        }
    }

    private void handleClient(Socket socket) {
        try (
                ObjectInputStream input = new ObjectInputStream(socket.getInputStream());
                ObjectOutputStream output = new ObjectOutputStream(socket.getOutputStream())
        ) {
            // Exemple de protocole de communication : en attente du format + bitrate
            String format = (String) input.readObject();
            double bitrateMbps = (Double) input.readObject();

            logger.info("Client requested format=" + format + ", bitrate=" + bitrateMbps + " Mbps");

            List<VideoFile> suitable = getFilesByFormatAndBitrate(format, bitrateMbps);
            output.writeObject(suitable);
            output.flush();

        } catch (IOException | ClassNotFoundException e) {
            logger.severe("Client communication error: " + e.getMessage());
        }
    }

    private List<VideoFile> getFilesByFormatAndBitrate(String format, double bitrate) {
        Map<String, Double> resolutionBitrates = Map.of(
                "240p", 0.5,
                "360p", 1.0,
                "480p", 2.5,
                "720p", 5.0,
                "1080p", 8.0
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
}

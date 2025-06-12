package client;

import shared.VideoFile;
import shared.Protocol;

import java.io.*;
import java.net.Socket;
import java.util.List;
import java.util.logging.Logger;

public class StreamingClient {

    private static final Logger logger = Logger.getLogger(StreamingClient.class.getName());
    private static final String SERVER_ADDRESS = "localhost";
    private static final int SERVER_PORT = 9090; // Port for the server connection
    private static final int TCP_UDP_PORT = 8888; // Port for TCP/UDP streaming

    private Socket socket;
    private ObjectOutputStream output;
    private ObjectInputStream input;

    // Make the connection to the server
    public List<VideoFile> connectToServer(String format, double speedMbps) {
        try {
            socket = new Socket(SERVER_ADDRESS, SERVER_PORT);
            output = new ObjectOutputStream(socket.getOutputStream());
            input = new ObjectInputStream(socket.getInputStream());

            output.writeObject(format);
            output.writeObject(speedMbps);
            output.flush();

            // Reception of available videos
            Object response = input.readObject();
            if (response instanceof List<?> list) {
                return list.stream()
                        .filter(o -> o instanceof VideoFile)
                        .map(o -> (VideoFile) o)
                        .toList();
            }
        } catch (IOException | ClassNotFoundException e) {
            logger.severe("Communication error with server : " + e.getMessage());
        }
        return List.of();
    }

    // Request a video stream from the server
    public void requestVideoStream(VideoFile file, Protocol protocol, Runnable onPlaybackEnd) {
        try {
            socket = new Socket(SERVER_ADDRESS, SERVER_PORT);
            output = new ObjectOutputStream(socket.getOutputStream());
            input = new ObjectInputStream(socket.getInputStream());

            output.writeObject("play_request");
            output.writeObject(file);
            output.writeObject(protocol.name());
            output.flush();

            System.out.println("Waiting for stream to start...");

            // Client side command: FFMPEG reads stream and plays it
            String command = buildFfmpegClientCommand(file, protocol);
            ProcessBuilder pb = new ProcessBuilder(command.split(" "));
            pb.inheritIO();
            Process ffplayProcess = pb.start();

            new Thread(() -> {
                try {
                    ffplayProcess.waitFor();
                    System.out.println("End of FFPLAY process.");
                    onPlaybackEnd.run();
                } catch (InterruptedException e) {
                    System.err.println("Playback interrupted: " + e.getMessage());
                }
            }).start();

        } catch (IOException e) {
            System.err.println("Video playback error: " + e.getMessage());
        }
    }

    // Build the command to run FFMPEG client based on the protocol
    private String buildFfmpegClientCommand(VideoFile file, Protocol protocol) {
        return switch (protocol) {
            case TCP -> "ffplay tcp://localhost:" + TCP_UDP_PORT;
            case UDP -> "ffplay udp://localhost:" + TCP_UDP_PORT;
            case RTP_UDP -> "ffplay -protocol_whitelist file,rtp,udp -i sdp/stream_rtp.sdp";
        };
    }

    // Close the socket and streams
    public void close() {
        try {
            if (socket != null) socket.close();
        } catch (IOException ignored) {}
    }
}

package client;

import shared.VideoFile;

import java.io.*;
import java.net.Socket;
import java.util.List;
import java.util.logging.Logger;

public class StreamingClient {

    private static final Logger logger = Logger.getLogger(StreamingClient.class.getName());
    private static final String SERVER_ADDRESS = "localhost";
    private static final int SERVER_PORT = 9090;

    private Socket socket;
    private ObjectOutputStream output;
    private ObjectInputStream input;

    public List<VideoFile> connectToServer(String format, double speedMbps) {
        try {
            socket = new Socket(SERVER_ADDRESS, SERVER_PORT);
            output = new ObjectOutputStream(socket.getOutputStream());
            input = new ObjectInputStream(socket.getInputStream());

            // Envoie du format + débit
            output.writeObject(format);
            output.writeObject(speedMbps);
            output.flush();

            // Réception des vidéos disponibles
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
        return List.of(); // Retourne une liste vide si erreur
    }

    public void close() {
        try {
            if (socket != null) socket.close();
        } catch (IOException ignored) {}
    }
}

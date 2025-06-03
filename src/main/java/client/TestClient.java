package client;

import shared.VideoFile;

import java.io.*;
import java.net.Socket;
import java.util.List;

public class TestClient {
    public static void main(String[] args) {
        String serverAddress = "localhost";
        int port = 9090;

        try (
                Socket socket = new Socket(serverAddress, port);
                ObjectOutputStream output = new ObjectOutputStream(socket.getOutputStream());
                ObjectInputStream input = new ObjectInputStream(socket.getInputStream())
        ) {
            // Simuler une demande : format = mp4, bitrate = 2.1 Mbps
            output.writeObject("mp4");
            output.writeObject(2.1); // Mbps
            output.flush();

            // Lire la liste des fichiers disponibles
            Object response = input.readObject();
            if (response instanceof List<?>) {
                List<?> list = (List<?>) response;
                System.out.println("Fichiers disponibles :");
                for (Object obj : list) {
                    if (obj instanceof VideoFile) {
                        VideoFile vf = (VideoFile) obj;
                        System.out.println(" - " + vf.getFilename());
                    }
                }
            }

        } catch (IOException | ClassNotFoundException e) {
            System.err.println("Erreur client de test : " + e.getMessage());
        }
    }
}

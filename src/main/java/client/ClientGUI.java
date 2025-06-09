package client;

import shared.VideoFile;

import javax.swing.*;
import java.awt.*;
import java.util.List;
import utils.LoggerConfig;

public class ClientGUI extends JFrame {

    private JComboBox<String> formatSelector;
    private JButton fetchButton;
    private JTextArea resultArea;

    private StreamingClient client;

    public ClientGUI() {
        super("Streaming Client");
        setSize(500, 400);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new BorderLayout());

        formatSelector = new JComboBox<>(new String[]{"mp4", "avi", "mkv"});
        fetchButton = new JButton("Get files");
        resultArea = new JTextArea();
        resultArea.setEditable(false);

        JPanel topPanel = new JPanel();
        topPanel.add(new JLabel("Format:"));
        topPanel.add(formatSelector);
        topPanel.add(fetchButton);

        add(topPanel, BorderLayout.NORTH);
        add(new JScrollPane(resultArea), BorderLayout.CENTER);

        fetchButton.addActionListener(e -> fetchVideos());

        client = new StreamingClient();
        setVisible(true);
    }

    private void fetchVideos() {
        String format = (String) formatSelector.getSelectedItem();
        double speedMbps = SpeedTester.measureDownloadSpeed();
        JOptionPane.showMessageDialog(this, "Estimated speed: " + speedMbps + " Mbps");

        List<VideoFile> videos = client.connectToServer(format, speedMbps);
        resultArea.setText(""); // Clear

        if (videos.isEmpty()) {
            resultArea.append("No video available.\n");
        } else {
            resultArea.append("Videos available: \n");
            for (VideoFile vf : videos) {
                resultArea.append(" - " + vf.getFilename() + "\n");
            }
        }

        client.close();
    }

    public static void main(String[] args) {
        utils.LoggerConfig.configureSimpleLogging();
        SwingUtilities.invokeLater(ClientGUI::new);
    }
}

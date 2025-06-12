package client;

import shared.VideoFile;
import shared.Protocol;

import javax.swing.*;
import java.awt.*;
import java.util.List;
import utils.LoggerConfig;

public class ClientGUI extends JFrame {

    private JComboBox<String> formatSelector;
    private JButton fetchButton;
    private JTextArea resultArea;
    private JLabel protocolLabel;
    private JComboBox<String> protocolSelector;
    private JButton playButton;
    private JList<VideoFile> videoList;

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

        protocolSelector = new JComboBox<>(new String[]{"Auto", "TCP", "UDP", "RTP_UDP"});
        playButton = new JButton("Play video");
        protocolSelector.setVisible(false);
        playButton.setVisible(false);
        protocolLabel = new JLabel("Protocol:");
        protocolLabel.setVisible(false);

        topPanel.add(protocolLabel);
        topPanel.add(protocolSelector);
        topPanel.add(playButton);

        videoList = new JList<>();
        add(new JScrollPane(videoList), BorderLayout.CENTER);

        fetchButton.addActionListener(e -> fetchVideos());

        client = new StreamingClient();
        setVisible(true);
    }

    // Fetch videos from the server based on selected format and speed
    private void fetchVideos() {
        String format = (String) formatSelector.getSelectedItem();
        protocolLabel.setVisible(true);
        protocolSelector.setVisible(true);
        playButton.setVisible(true);
        double speedMbps = SpeedTester.measureDownloadSpeed();
        JOptionPane.showMessageDialog(this, "Estimated speed: " + speedMbps + " Mbps");

        List<VideoFile> videos = client.connectToServer(format, speedMbps);
        videoList.setListData(videos.toArray(new VideoFile[0]));
        resultArea.setText(""); // Clear

        if (videos.isEmpty()) {
            resultArea.append("No videos available.\n");
        } else {
            resultArea.append("Videos available: \n");
            for (VideoFile vf : videos) {
                resultArea.append(" - " + vf.getFilename() + "\n");
            }
        }

        playButton.addActionListener(e -> {
            VideoFile selected = videoList.getSelectedValue();
            if (selected == null) {
                JOptionPane.showMessageDialog(this, "Select a video");
                return;
            }

            String selectedProto = (String) protocolSelector.getSelectedItem();
            Protocol protocol;
            if ("Auto".equals(selectedProto)) {
                protocol = Protocol.getDefaultForResolution(selected.getResolution());
            } else {
                protocol = Protocol.valueOf(selectedProto);
            }

            client.requestVideoStream(selected, protocol, () -> {
                SwingUtilities.invokeLater(() -> dispose());
            });
        });


        client.close();
    }

    public static void main(String[] args) {
        utils.LoggerConfig.configureSimpleLogging();
        SwingUtilities.invokeLater(ClientGUI::new);
    }
}

package com.gui;

import javax.swing.*;
import com.master.P2PRecog;
import java.awt.*;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

public class GUI {
    public static void main(String[] args) throws HeadlessException, UnknownHostException, InterruptedException {
        P2PRecog p2pRecog = P2PRecog.getInstance();

        JFrame frame = new JFrame("Device: " + InetAddress.getLocalHost().getHostAddress().split("\\.")[3]);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(750, 800);
        frame.setLayout(new BorderLayout());

        JMenuBar menuBar = new JMenuBar();

        JMenu filesMenu = new JMenu("Files");
        JMenuItem connectItem = new JMenuItem("Connect");
        JMenuItem disconnectItem = new JMenuItem("Disconnect");

        JLabel statusLabel = new JLabel("Status: Disconnected", SwingConstants.CENTER);
        statusLabel.setOpaque(true);
        statusLabel.setBackground(Color.RED);
        statusLabel.setForeground(Color.WHITE);
        frame.add(statusLabel, BorderLayout.NORTH);

        connectItem.addActionListener(e -> {
            new SwingWorker<Void, Void>() {
                @Override
                protected Void doInBackground() throws Exception {
                    System.out.println("Connecting to the overlay network...");
                    p2pRecog.Connect();
                    return null;
                }
            }.execute();
        });

        connectItem.addActionListener(e -> {
            new SwingWorker<Void, Void>() {
                @Override
                protected Void doInBackground() throws Exception {
                    Thread.sleep(1000);
                    boolean isConnected = p2pRecog.isConnected();
                    if (isConnected) {
                        statusLabel.setText("Status: Connected");
                        statusLabel.setBackground(Color.GREEN);
                    } else {
                        statusLabel.setText("Status: Disconnected");
                        statusLabel.setBackground(Color.RED);
                    }

                    return null;
                }
            }.execute();
        });

        disconnectItem.addActionListener(e -> {
            System.out.println("Disconnecting from the overlay network...");
            p2pRecog.Disconnect();
            boolean isConnected = p2pRecog.isConnected();
            if (isConnected) {
                statusLabel.setText("Status: Connected");
                statusLabel.setBackground(Color.GREEN);
            } else {
                statusLabel.setText("Status: Disconnected");
                statusLabel.setBackground(Color.RED);
            }
        });

        filesMenu.add(connectItem);
        filesMenu.add(disconnectItem);
        menuBar.add(filesMenu);

        JMenu helpMenu = new JMenu("Help");
        JMenuItem aboutItem = new JMenuItem("About");
        aboutItem.addActionListener(e -> {
            JOptionPane.showMessageDialog(frame, "Developed by Eren TASKEN with the student ID 20210702054.");
        });
        helpMenu.add(aboutItem);
        menuBar.add(helpMenu);

        frame.setJMenuBar(menuBar);

        JPanel mainPanel = new JPanel();
        mainPanel.setLayout(new BoxLayout(mainPanel, BoxLayout.Y_AXIS));
        frame.add(mainPanel, BorderLayout.CENTER);

        JPanel sharedFolderPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        sharedFolderPanel.setBorder(BorderFactory.createTitledBorder("Root of the P2P shared folder"));
        JTextField sharedFolderField = new JTextField("./sharedFile",30);
        JButton sharedFolderButton = new JButton("Set");
        sharedFolderPanel.add(sharedFolderField);
        sharedFolderPanel.add(sharedFolderButton);
        mainPanel.add(sharedFolderPanel);

        sharedFolderButton.addActionListener(e -> {
            String sharedFolder = sharedFolderField.getText();
            if (sharedFolder != null && !sharedFolder.isEmpty()) {
                p2pRecog.SetSharedFolder(sharedFolder);
                JOptionPane.showMessageDialog(frame, "Shared folder set to: " + sharedFolder);
            } else {
                JOptionPane.showMessageDialog(frame, "Please enter a valid shared folder.");
            }
        });

        JPanel destinationFolderPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        destinationFolderPanel.setBorder(BorderFactory.createTitledBorder("Destination folder"));
        JTextField destinationFolderField = new JTextField("./sharedFile", 30);
        JButton destinationFolderButton = new JButton("Set");
        destinationFolderPanel.add(destinationFolderField);
        destinationFolderPanel.add(destinationFolderButton);
        mainPanel.add(destinationFolderPanel);

        destinationFolderButton.addActionListener(e -> {
            String destinationFolder = destinationFolderField.getText();
            if (destinationFolder != null && !destinationFolder.isEmpty()) {
                p2pRecog.SetDestinationFolder(destinationFolder);
                JOptionPane.showMessageDialog(frame, "Destination folder set to: " + destinationFolder);
            } else {
                JOptionPane.showMessageDialog(frame, "Please enter a valid destination folder.");
            }
        });

        JPanel settingsPanel = new JPanel();
        settingsPanel.setLayout(new BoxLayout(settingsPanel, BoxLayout.Y_AXIS));
        settingsPanel.setBorder(BorderFactory.createTitledBorder("Settings"));

        List<String> excludedFiles = new ArrayList<>();
        DefaultListModel<String> excludeListModel = new DefaultListModel<>();
        JList<String> excludeFilesList = new JList<>(excludeListModel);

        JPanel excludeFilesPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        excludeFilesPanel.add(new JLabel("Exclude files matching these masks:"));
        JTextField excludeField = new JTextField(20);
        JButton addExcludeButton = new JButton("Add");
        JButton deleteExcludeButton = new JButton("Delete");
        excludeFilesPanel.add(excludeField);
        excludeFilesPanel.add(addExcludeButton);
        excludeFilesPanel.add(deleteExcludeButton);
        settingsPanel.add(excludeFilesPanel);

        excludeFilesList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        JScrollPane excludeFilesScroller = new JScrollPane(excludeFilesList);
        settingsPanel.add(excludeFilesScroller);

        addExcludeButton.addActionListener(e -> {
            String excludePattern = excludeField.getText().trim();
            if (!excludePattern.isEmpty() && !excludedFiles.contains(excludePattern)) {
                excludedFiles.add(excludePattern);
                excludeListModel.addElement(excludePattern);
                excludeField.setText("");
            } else {
                JOptionPane.showMessageDialog(frame, "Please enter a valid and unique exclude pattern.");
            }
        });

        deleteExcludeButton.addActionListener(e -> {
            String selectedExclude = excludeFilesList.getSelectedValue();
            if (selectedExclude != null) {
                excludedFiles.remove(selectedExclude);
                excludeListModel.removeElement(selectedExclude);
            } else {
                JOptionPane.showMessageDialog(frame, "Please select an exclusion pattern to delete.");
            }
        });

        mainPanel.add(settingsPanel);

        JPanel downloadingPanel = new JPanel(new BorderLayout());
        downloadingPanel.setBorder(BorderFactory.createTitledBorder("Downloading files"));
        JTextArea downloadingArea = new JTextArea(5, 40);
        downloadingArea.setEditable(false);
        downloadingPanel.add(new JScrollPane(downloadingArea), BorderLayout.CENTER);
        mainPanel.add(downloadingPanel);

        AtomicInteger dotCount = new AtomicInteger(0);
        Timer timer = new Timer(1000, e -> {
            StringBuilder displayText = new StringBuilder();
            downloadingArea.setText("");

            P2PRecog.fileProgressMap.forEach((fileHash, fileProgress) -> {
                displayText.append(fileProgress.fileName);

                if (fileProgress.calculatePercentage().equals("100.00%")) {
                    displayText.append(" - Download complete\n");
                    return;
                }

                int dots = dotCount.get() % 6;
                for (int i = 0; i < dots; i++) {
                    displayText.append("•");
                }
                dotCount.incrementAndGet();

                String progress = String.format(" %d/%d chunks received (%s)", fileProgress.receivedChunks, fileProgress.totalChunks, fileProgress.calculatePercentage());
                displayText.append(progress).append("\n");
            });

            downloadingArea.append(displayText.toString());
        });
        timer.start();

        JPanel foundFilesPanel = new JPanel(new BorderLayout());
        foundFilesPanel.setBorder(BorderFactory.createTitledBorder("Found files"));
        DefaultListModel<String> listModel = new DefaultListModel<>();
        JList<String> foundFilesList = new JList<>(listModel);
        foundFilesList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        JScrollPane listScroller = new JScrollPane(foundFilesList);
        foundFilesPanel.add(listScroller, BorderLayout.CENTER);

        foundFilesList.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mouseClicked(java.awt.event.MouseEvent e) {
                if (e.getClickCount() == 2) { // Detect double-click
                    String selectedEntry = foundFilesList.getSelectedValue();
                    if (selectedEntry != null) {
                        String[] parts = selectedEntry.split("→");
        
                        if (parts.length == 2) {
                            String fileName = parts[0].trim();
                            String fileHash = parts[1].trim();
                            if (!fileHash.contains("•")) {
                                // Check if file matches any exclusion pattern
                                boolean isExcluded = excludedFiles.stream().anyMatch(pattern -> {
                                    String regex = pattern.replace(".", "\\.").replace("*", ".*");
                                    return fileName.matches(regex);
                                });
        
                                if (isExcluded) {
                                    JOptionPane.showMessageDialog(frame, "File is excluded from download.");
                                    return;
                                }
        
                                p2pRecog.RequestForFile(fileName, fileHash);
                                JOptionPane.showMessageDialog(frame, "Requesting file: " + fileName + " with hash: " + fileHash);
                            }
                        }
                    }
                }
            }
        });
        

        mainPanel.add(foundFilesPanel);

        JButton searchButton = new JButton("Search");
        searchButton.addActionListener(e -> {
            HashMap<String, String> foundFiles;
            try {
                foundFiles = p2pRecog.GetReachableFiles();
                listModel.clear();
                if (foundFiles != null && !foundFiles.isEmpty()) {
                    for (var entry : foundFiles.entrySet()) {
                        listModel.addElement(entry.getValue() + "→" + entry.getKey());
                    }
                } else {
                    JOptionPane.showMessageDialog(frame, "No files found.");
                }
            } catch (InterruptedException e1) {
                e1.printStackTrace();
            }
        });

        mainPanel.add(searchButton);

        frame.setVisible(true);
    }
}

package com.gui;

import javax.swing.*;

import com.master.P2PRecog;

import java.awt.*;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.HashMap;
import java.util.concurrent.atomic.AtomicInteger;

public class GUI {

    

    public static void main(String[] args) throws HeadlessException, UnknownHostException, InterruptedException {
        P2PRecog p2pRecog = P2PRecog.getInstance();
        
        // Create the main frame
        JFrame frame = new JFrame("Device : " + InetAddress.getLocalHost().getHostAddress().split("\\.")[3]);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(600, 700);
        frame.setLayout(new BorderLayout());

        // Create the menu bar
        JMenuBar menuBar = new JMenuBar();

        // Create "Files" menu
        JMenu filesMenu = new JMenu("Files");
        JMenuItem connectItem = new JMenuItem("Connect");
        JMenuItem disconnectItem = new JMenuItem("Disconnect");

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

        disconnectItem.addActionListener(e -> {
            
            System.out.println("Disconnecting from the overlay network...");

            p2pRecog.Disconnect();
        });

        filesMenu.add(connectItem);
        filesMenu.add(disconnectItem);
        menuBar.add(filesMenu);

        JMenu helpMenu = new JMenu("Help");
        menuBar.add(helpMenu);

        frame.setJMenuBar(menuBar);

        JPanel mainPanel = new JPanel();
        mainPanel.setLayout(new BoxLayout(mainPanel, BoxLayout.Y_AXIS));
        frame.add(mainPanel, BorderLayout.CENTER);

        JPanel sharedFolderPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        sharedFolderPanel.setBorder(BorderFactory.createTitledBorder("Root of the P2P shared folder"));
        JTextField sharedFolderField = new JTextField(30);
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
        JTextField destinationFolderField = new JTextField(30);
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

        JCheckBox checkNewFilesBox = new JCheckBox("Check new files only in the root");
        settingsPanel.add(checkNewFilesBox);

        JPanel excludeFilesPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        excludeFilesPanel.add(new JLabel("Exclude files matching these masks:"));
        JTextField excludeField = new JTextField(20);
        JButton addExcludeButton = new JButton("Add");
        JButton deleteExcludeButton = new JButton("Del");
        excludeFilesPanel.add(excludeField);
        excludeFilesPanel.add(addExcludeButton);
        excludeFilesPanel.add(deleteExcludeButton);
        settingsPanel.add(excludeFilesPanel);

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
            downloadingArea.setText(""); // Clear the area before appending new text

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

                String progress = String.format(" %d/%d chunks received (%s)", 
                                                fileProgress.receivedChunks, 
                                                fileProgress.totalChunks,
                                                fileProgress.calculatePercentage());
                displayText.append(progress);

                displayText.append("\n");
            });

            downloadingArea.append(displayText.toString());
        });
        timer.start();


        JPanel foundFilesPanel = new JPanel(new BorderLayout());
        foundFilesPanel.setBorder(BorderFactory.createTitledBorder("Found files"));
        DefaultListModel<String> listModel = new DefaultListModel<>();
        JList<String> foundFilesList = new JList<>(listModel);
        foundFilesList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        foundFilesList.setLayoutOrientation(JList.VERTICAL);
        foundFilesList.setVisibleRowCount(-1);
        JScrollPane listScroller = new JScrollPane(foundFilesList);
        foundFilesPanel.add(listScroller, BorderLayout.CENTER);

        foundFilesList.addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting() && foundFilesList.getSelectedValue() != null) {
                String selectedEntry = foundFilesList.getSelectedValue();
                selectedEntry = selectedEntry.trim();
                String[] parts = selectedEntry.split("→");
                
                if (parts.length == 2) {
                    if (parts[1].contains("•")){ 
                        parts[1] = parts[1].substring(0, parts[1].indexOf("•"));
                    }

                    String fileName = parts[0]; // Remove "File: " prefix
                    String fileHash = parts[1];

                    p2pRecog.RequestForFile(fileName, fileHash);

                    JOptionPane.showMessageDialog(frame, "Requesting file: " + fileName + " with hash: " + fileHash);
                }
            }
        });
        mainPanel.add(foundFilesPanel);


        JButton searchButton = new JButton("Search");
        searchButton.addActionListener(e -> {
            HashMap<String, String> foundFiles;
            try {
                foundFiles = p2pRecog.GetReachableFiles();
                listModel.clear(); // Clear existing entries
                if (foundFiles != null && !foundFiles.isEmpty()) {
                    for (var entry : foundFiles.entrySet()) {
                        listModel.addElement(entry.getValue()  + "→" + entry.getKey());
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
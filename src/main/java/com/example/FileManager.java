package com.example;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class FileManager {
    private final Map<String, ArrayList<String>> fileHashMap = new ConcurrentHashMap<>();

    private String device;

    public FileManager() {
        syncFileMap();
    }

    public void setDevice(String device) {
        this.device = device;
    }

    public static String calculateSHA256(byte[] fileBytes) throws NoSuchAlgorithmException {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        digest.update(fileBytes);
        byte[] hashBytes = digest.digest();

        StringBuilder hexString = new StringBuilder();
        for (byte b : hashBytes) {
            hexString.append(String.format("%02x", b));
        }

        return hexString.toString();
    }

    public void syncFileMap() {
    
    
        String directoryPath = "./sharedFiles";
        Path dirPath = Path.of(directoryPath).toAbsolutePath().normalize();
        File directory = dirPath.toFile();

        if (!directory.exists() || !directory.isDirectory()) {
            System.err.println("Invalid directory: " + dirPath);
            return;
        }

        File[] files = directory.listFiles();

        if (files == null || files.length == 0) {
            System.out.println("No files found in directory: " + dirPath);
            return;
        }

        fileHashMap.clear();
    
        for (File file : files) {
            if (file.isFile()) {
                try {
                    byte[] fileBytes = Files.readAllBytes(file.toPath());
                    fileHashMap.putIfAbsent(calculateSHA256(fileBytes), new ArrayList<>(Arrays.asList(file.getName(), String.valueOf(file.length()))));
                } catch (IOException | NoSuchAlgorithmException e) {
                    System.err.println("Error processing file: " + file.getName());
                    e.printStackTrace();
                }
            }
        }

    }

    public List<DirectoryFile> getFiles() {
        syncFileMap();

        List<DirectoryFile> directoryFiles = new ArrayList<>();
        for (Map.Entry<String, ArrayList<String>> entry : fileHashMap.entrySet()) {
            // Assuming the first element in the ArrayList is the file name, and the second is the file length
            String fileName = entry.getValue().get(0);
            String fileLength = entry.getValue().get(1);
            String fileHash = entry.getKey();
            directoryFiles.add(new DirectoryFile(this.device, fileName, fileHash, fileLength));
        }
        return directoryFiles;
    }
    

    public byte[] returnFile(String fileName, String fileHash) {
        String directoryPath = "./sharedFiles";
        Path dirPath = Path.of(directoryPath);
        File directory = dirPath.toFile();

        if (!directory.exists() || !directory.isDirectory()) {
            System.err.println("Invalid directory: " + dirPath);
            return null;
        }

        File[] files = directory.listFiles();

        if (files == null || files.length == 0) {
            System.out.println("No files found in directory: " + dirPath);
            return null;
        }

        for (File file : files) {
            if (file.isFile()) {
                try {
                    byte[] fileBytes = Files.readAllBytes(file.toPath());

                    if (fileHash.equals(calculateSHA256(fileBytes)) && (fileName == null || fileName.equals(file.getName()))) {
                        return Files.readAllBytes(file.toPath());
                    }

                } catch (IOException | NoSuchAlgorithmException e) {
                    System.err.println("Error returning file: " + file.getName());
                    e.printStackTrace();
                }
            }
        }
        return null;
    }


    public boolean saveFile(NetworkFile file) {
        String directoryPath = "./sharedFiles";
        Path dirPath = Path.of(directoryPath);
        File directory = dirPath.toFile();

        if (!directory.exists() || !directory.isDirectory()) {
            System.err.println("Invalid directory: " + dirPath);
            return false;
        }

        try {
            byte[] fileContent = file.getFile();  // You need to pass file content to the DirectoryFile

            String calculatedHash = calculateSHA256(fileContent);

            // Create a new file in the shared directory
            Path filePath = dirPath.resolve(file.getFileName());
            Files.write(filePath, fileContent);

            // Update the file map (optional)
            fileHashMap.putIfAbsent(calculatedHash, new ArrayList<>(Arrays.asList(file.getFileName(), String.valueOf(fileContent.length))));

            System.out.println("File saved successfully: " + file.getFileName());
            return true;

        } catch (IOException | NoSuchAlgorithmException e) {
            System.err.println("Error saving file: " + file.getFileName());
            e.printStackTrace();
            return false;
        }
    }
}

package com.example;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class FileManager {
    private final Map<String, String> fileHashMap = new ConcurrentHashMap<>();

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

        for (File file : files) {
            if (file.isFile()) {
                try {
                    byte[] fileBytes = Files.readAllBytes(file.toPath());
                    fileHashMap.putIfAbsent(calculateSHA256(fileBytes), file.getName());
                } catch (IOException | NoSuchAlgorithmException e) {
                    System.err.println("Error processing file: " + file.getName());
                    e.printStackTrace();
                }
            }
        }

        Iterator<Map.Entry<String, String>> iterator = fileHashMap.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<String, String> entry = iterator.next();
            File file = new File(directoryPath + "/" + entry.getValue());
            if (!file.exists()) {
                iterator.remove();
            }
        }
    }

    public List<DirectoryFile> getFiles() {

        List<DirectoryFile> directoryFiles = new ArrayList<DirectoryFile>();
        for (Map.Entry<String, String> entry : fileHashMap.entrySet()) {
            directoryFiles.add(new DirectoryFile(this.device, entry.getValue(), entry.getKey()));
        }

        return directoryFiles;
    }

    public String returnFile(String fileName, String fileHash) {
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
                        return Files.readString(file.toPath());
                    }

                } catch (IOException | NoSuchAlgorithmException e) {
                    System.err.println("Error returning file: " + file.getName());
                    e.printStackTrace();
                }
            }
        }
        return null;
    }
}

package com.master;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import com.models.DirectoryFile;
import com.models.NetworkFile;

public class FileManager {
    private final Map<String, ArrayList<String>> fileHashMap = new ConcurrentHashMap<>();

    private String device;

    public static int CHUNK_SIZE = 256 * 1000; // 256 KB

    private static String filePath = "./sharedFiles";
    private static String destinationFilePath = "./sharedFiles";

    public FileManager(String device) {
        this.device = device;
        syncFileMap();
    }

    public void setFilePath(String filePath) {
        FileManager.filePath = filePath;
    }

    public void setDestinationFilePath(String destinationFilePath) {
        FileManager.destinationFilePath = destinationFilePath;
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
        Path dirPath = Path.of(filePath).toAbsolutePath().normalize();
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

    public int saveChunks(NetworkFile file) {
        Path dirPath = Path.of(destinationFilePath).toAbsolutePath().normalize();
        File directory = dirPath.toFile();

        if (!directory.exists() || !directory.isDirectory()) {
            directory.mkdirs();
        }

        try (RandomAccessFile rAF = new RandomAccessFile(destinationFilePath + "/" + file.getFileName(), "rw")) {
            long length = file.getDataSize();
            int chunkNo = file.getChunkNo();
            byte[] chunkData = file.getData();

            if (rAF.length() < length) {
                rAF.setLength(length);
            }

            rAF.seek((long) chunkNo * CHUNK_SIZE);
            
            rAF.write(chunkData);

            return chunkNo;

        } catch (IOException e) {
            System.err.println("Error writing chunk to file: " + e.getMessage());
        }

        return -1;
    }

    public List<DirectoryFile> getFiles() {
        syncFileMap();

        List<DirectoryFile> directoryFiles = new ArrayList<>();
        for (Map.Entry<String, ArrayList<String>> entry : fileHashMap.entrySet()) {
            String fileName = entry.getValue().get(0);
            String fileLength = entry.getValue().get(1);
            String fileHash = entry.getKey();
            directoryFiles.add(new DirectoryFile(this.device, fileName, fileHash, fileLength));
        }
        return directoryFiles;
    }

    public byte[] getChunk(String fileName, String fileHash, int chunkIndex) {
        Path dirPath = Path.of(filePath);
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
                        int start = chunkIndex * CHUNK_SIZE;
                        int end = Math.min(start + CHUNK_SIZE, fileBytes.length);
    
                        if (start < fileBytes.length) {
                            return Arrays.copyOfRange(fileBytes, start, end);
                        } else {
                            System.err.println("Chunk index out of range for file: " + fileName);
                            return null;
                        }
                    }
    
                } catch (IOException | NoSuchAlgorithmException e) {
                    System.err.println("Error reading chunk from file: " + file.getName());
                    e.printStackTrace();
                }
            }
        }
        return null;
    }
}

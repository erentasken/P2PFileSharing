package com.example;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonProcessingException;

class DirectoryFile {
    private String fileName;
    private String fileHash;
    private String device;

    @JsonCreator
    public DirectoryFile(
        @JsonProperty("device") String device,
        @JsonProperty("fileName") String fileName, 
        @JsonProperty("fileHash") String fileHash
    ) {
        this.fileName = fileName;
        this.fileHash = fileHash;
        this.device = device;
    }

    public String getFileName() {
        return fileName;
    }

    public String getFileHash() {
        return fileHash;
    }

    public String getDevice() {
        return device;
    }

    @Override
    public String toString() {
        return "DirectoryFile{" +
                "fileName='" + fileName + '\'' +
                ", fileHash='" + fileHash + '\'' +
                ", device='" + device + '\'' +
                '}';
    }
}

class DirectoryNotification {
    private int ttl;
    private List<String> ipAncestors;
    private List<DirectoryFile> directory;

    public DirectoryNotification(int ttl, List<String> ipAncestors, List<DirectoryFile> directory) {
        this.ttl = ttl;
        this.ipAncestors = ipAncestors;
        this.directory = directory;
    }

    public static DirectoryNotification parseJson(String jsonString) {
        ObjectMapper objectMapper = new ObjectMapper();

        try {
            JsonNode rootNode = objectMapper.readTree(jsonString);
            int ttl = rootNode.get("ttl").asInt();

            List<String> ipAncestors = objectMapper.readValue(
                rootNode.get("ipAncestors").toString(),
                objectMapper.getTypeFactory().constructCollectionType(List.class, String.class)
            );

            List<DirectoryFile> directoryFiles = objectMapper.readValue(
                rootNode.get("directory").toString(),
                objectMapper.getTypeFactory().constructCollectionType(List.class, DirectoryFile.class)
            );

            return new DirectoryNotification(ttl, ipAncestors, directoryFiles);

        } catch (JsonProcessingException e) {
            System.err.println("Error processing JSON: " + e.getMessage());
        } catch (IOException e) {
            System.err.println("Error reading JSON: " + e.getMessage());
        }

        return null;
    }

    public int getTtl() {
        return ttl;
    }

    public void decreaseTtl() {
        if (ttl > 0) {
            this.ttl--;
        } else {
            System.err.println("TTL is already zero. Cannot decrease further.");
        }
    }

    public boolean isTtlValid() {
        return this.ttl > 0;
    }

    public List<String> getIpAncestors() {
        return ipAncestors;
    }

    public void addIpAncestor(String ip) {
        this.ipAncestors.add(ip);
    }

    public List<DirectoryFile> getDirectory() {
        return directory;
    }

    public void setDirectory(List<DirectoryFile> directory) {
        this.directory = directory;
    }

    public void addSharedFiles(String device, FileManager fileManager) {
        fileManager.syncFileMap();

        for (Map.Entry<String, String> entry : fileManager.getFiles().entrySet()) {
            DirectoryFile file = new DirectoryFile(device, entry.getValue(), entry.getKey());
            this.directory.add(file);
        }
    }

    public void processNotification(String hostIP, FileManager fileManager) {
        addIpAncestor(hostIP);

        decreaseTtl();

        addSharedFiles(hostIP.split("\\.")[3],fileManager);
    }

    public String toJson() {
        ObjectMapper objectMapper = new ObjectMapper();
        try {
            return objectMapper.writeValueAsString(this);
        } catch (JsonProcessingException e) {
            System.err.println("Error converting object to JSON: " + e.getMessage());
            return null;
        }
    }

    @Override
    public String toString() {
        return "DirectoryNotification{" +
                "ttl=" + ttl +
                ", ipAncestors=" + ipAncestors +
                ", directory=" + directory +
                '}';
    }
}

public class FileManager {

    private final Map<String, String> fileHashMap = new ConcurrentHashMap<>();

    public FileManager() {
        syncFileMap();
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

    public HashMap<String, String> getFiles() {
        return new HashMap<>(fileHashMap);
    }

    public String buildNotificationJson(int ttl, List<String> ipAncestors) {
        StringBuilder jsonBuilder = new StringBuilder();
    
        jsonBuilder.append("{\n");
        jsonBuilder.append("  \"ttl\": ").append(ttl).append(",\n");
        jsonBuilder.append("  \"ipAncestors\": [\n");
    
        for (int i = 0; i < ipAncestors.size(); i++) {
            jsonBuilder.append("    \"").append(ipAncestors.get(i)).append("\"");
            if (i < ipAncestors.size() - 1) {
                jsonBuilder.append(",");
            }
            jsonBuilder.append("\n");
        }
    
        jsonBuilder.append("  ],\n");
        jsonBuilder.append("  \"directory\": [\n");
    
        Iterator<Map.Entry<String, String>> fileIterator = getFiles().entrySet().iterator();
        while (fileIterator.hasNext()) {
            Map.Entry<String, String> entry = fileIterator.next();
    
            jsonBuilder.append("    {\"fileName\": \"").append(entry.getValue())
                .append("\", \"fileHash\": \"").append(entry.getKey())
                .append("\", \"device\": \"").append(ipAncestors.get(0).split("\\.")[3]).append("\"}");
    
            if (fileIterator.hasNext()) {
                jsonBuilder.append(",");
            }
    
            jsonBuilder.append("\n");
        }
    
        jsonBuilder.append("  ]\n");
        jsonBuilder.append("}\n");
    
        return jsonBuilder.toString();
    }

    public String returnFile(String fileName, String fileHash) {
        String directoryPath = "/home/eren/Desktop/Okul/471/ProjectDir/Cildircam/src/src/main/java/com/example/sharedFiles";  // Correct the path to your folder
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

    // public static void main(String[] args) {
    //     String jsonString = "{\n" +
    //         "  \"ttl\": 3,\n" +
    //         "  \"ipAncestors\": [\n" +
    //         "    \"192.168.1.1\", \"192.168.1.2\"\n" +
    //         "  ],\n" +
    //         "  \"directory\": [\n" +
    //         "    {\"device\": \"Device1\", \"fileName\": \"file1.txt\", \"fileHash\": \"abc123\"},\n" +
    //         "    {\"device\": \"Device2\", \"fileName\": \"file2.txt\", \"fileHash\": \"def456\"}\n" +
    //         "  ]\n" +
    //         "}";

    //     DirectoryNotification notification = DirectoryNotification.parseJson(jsonString);

    //     if (notification != null) {
    //         System.out.println("Parsed DirectoryNotification:");
    //         System.out.println(notification);

    //         notification.decreaseTtl();
    //         notification.addIpAncestor("192.168.1.3");
    //         notification.getDirectory().forEach(file -> System.out.println(file.get));

    //         String outputJson = notification.toJson();
    //         System.out.println("Updated JSON:");
    //         System.out.println(outputJson);
    //     } else {
    //         System.err.println("Failed to parse the JSON string.");
    //     }
    // }
}
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

class DirectoryFile {
    private String fileName;
    private String fileHash;

    // Add @JsonCreator to specify the constructor to use for deserialization
    @JsonCreator
    public DirectoryFile(
        @JsonProperty("fileName") String fileName, 
        @JsonProperty("fileHash") String fileHash
    ) {
        this.fileName = fileName;
        this.fileHash = fileHash;
    }

    public String getFileName() {
        return fileName;
    }

    public String getFileHash() {
        return fileHash;
    }
}

class DirectoryNotification {
    private int ttl;
    private String sourceIP;
    private int nodeNo;
    private int networkNo;
    private List<DirectoryFile> directory;

    public DirectoryNotification(int ttl, String sourceIP, int nodeNo, int networkNo, List<DirectoryFile> directory) {
        this.ttl = ttl;
        this.sourceIP = sourceIP;
        this.nodeNo = nodeNo;
        this.networkNo = networkNo;
        this.directory = directory;
    }

    public DirectoryNotification parseJson(String jsonString) {
        ObjectMapper objectMapper = new ObjectMapper();
        
        try {
            // Parse the JSON string into a JsonNode
            JsonNode rootNode = objectMapper.readTree(jsonString);
            
            // Extract fields from the root node
            int ttl = rootNode.get("ttl").asInt();
            String sourceIP = rootNode.get("sourceIP").asText();
            int nodeNo = rootNode.get("nodeNo").asInt();
            int networkNo = rootNode.get("networkNo").asInt();
            
            // Parse the directory list
            List<DirectoryFile> directoryFiles = objectMapper.readValue(
                rootNode.get("directory").toString(),
                objectMapper.getTypeFactory().constructCollectionType(List.class, DirectoryFile.class)
            );
            
            // Create and return a DirectoryNotification object
            return new DirectoryNotification(ttl, sourceIP, nodeNo, networkNo, directoryFiles);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    // Getters and setters
    public int getTtl() {
        return ttl;
    }

    public void decreaseTtl() {
        this.ttl--;
    }

    public boolean checkTtl() {
        return this.ttl > 0;
    }

    public String getSourceIP() {
        return sourceIP;
    }

    public void setSourceIP(String sourceIP) {
        this.sourceIP = sourceIP;
    }

    public int getNodeNo() {
        return nodeNo;
    }

    public void setNodeNo(int nodeNo) {
        this.nodeNo = nodeNo;
    }

    public int getNetworkNo() {
        return networkNo;
    }

    public void setNetworkNo(int networkNo) {
        this.networkNo = networkNo;
    }

    public List<DirectoryFile> getDirectory() {
        return directory;
    }

    public void setDirectory(List<DirectoryFile> directory) {
        this.directory = directory;
    }
}

public class FileManager {

    private final Map<String, String> fileHashMap = new ConcurrentHashMap<>();

    public FileManager() {
        syncFileMap();
    }

    // Calculate SHA-256 hash for the file content
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

    // Synchronize the file map by checking the files in the directory and their hashes
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

        // Add file hashes to map
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

        // Remove missing files from the map
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

    public String buildNotificationJson(int ttl, String sourceIP) {
    StringBuilder jsonBuilder = new StringBuilder();
    
    // Extract nodeNo (last octet) and networkNo (third octet) from sourceIP
    String[] ipParts = sourceIP.split("\\.");
    int nodeNo = Integer.parseInt(ipParts[3]); // last octet is nodeNo
    int networkNo = Integer.parseInt(ipParts[2]); // third octet is networkNo
    
    jsonBuilder.append("{\n");
    jsonBuilder.append("  \"ttl\": ").append(ttl).append(",\n");
    jsonBuilder.append("  \"sourceIP\": \"").append(sourceIP).append("\",\n");
    jsonBuilder.append("  \"nodeNo\": ").append(nodeNo).append(",\n");
    jsonBuilder.append("  \"networkNo\": ").append(networkNo).append(",\n");
    jsonBuilder.append("  \"directory\": [\n");

    Iterator<Map.Entry<String, String>> fileIterator = getFiles().entrySet().iterator();
    while (fileIterator.hasNext()) {
        Map.Entry<String, String> entry = fileIterator.next();
        jsonBuilder.append("    {\"fileName\": \"").append(entry.getValue())
            .append("\", \"fileHash\": \"").append(entry.getKey()).append("\"}");
        
        // If this is not the last file, add a comma
        if (fileIterator.hasNext()) {
            jsonBuilder.append(",");
        }
        
        jsonBuilder.append("\n");
    }

    jsonBuilder.append("  ]\n");
    jsonBuilder.append("}\n");

    return jsonBuilder.toString();
}


    public static void main(String[] args) {
        FileManager fileManager = new FileManager();

        String notificationJson = fileManager.buildNotificationJson(3, "10.10.1.30");

        DirectoryNotification directoryNotification = new DirectoryNotification(0, "", 0, 0, null);
        directoryNotification = directoryNotification.parseJson(notificationJson);

        if (directoryNotification != null) {
            System.out.println("TTL: " + directoryNotification.getTtl());
            System.out.println("Source IP: " + directoryNotification.getSourceIP());
            System.out.println("Node No: " + directoryNotification.getNodeNo());
            System.out.println("Network No: " + directoryNotification.getNetworkNo());

            // Print directory files
            for (DirectoryFile file : directoryNotification.getDirectory()) {
                System.out.println("File: " + file.getFileName() + ", Hash: " + file.getFileHash());
            }
        }


    }
}

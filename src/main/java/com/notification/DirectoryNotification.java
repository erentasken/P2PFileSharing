package com.notification;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.models.DirectoryFile;
import com.example.FileManager;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.core.JsonProcessingException;

import java.util.ArrayList;
import java.util.List;

public class DirectoryNotification {
    private int ttl;
    private List<String> ipAncestors;
    private List<DirectoryFile> directory;

    @JsonIgnore
    private boolean ttlValid;

    public DirectoryNotification(List<DirectoryFile> directory) {
        this.directory = directory;
        this.ttl = 3;
        this.ipAncestors = new ArrayList<>();
    }

    private DirectoryNotification(int ttl, List<String> ipAncestors, List<DirectoryFile> directory) {
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
        } catch (Exception e) {
            return null;
        }
    }

    public int getTtl() {
        return ttl;
    }

    public int decreaseTtl() {
        if (ttl > 0) {
            this.ttl--;
            return this.ttl;
        } else {
            return -1;
        }
    }

    public List<DirectoryFile> getDirectory() {
        return directory;
    }

    public void addSharedFiles(FileManager fileManager) {
        fileManager.syncFileMap();
        for (DirectoryFile entry : fileManager.getFiles()) {
            this.directory.add(entry);
        }
    }

    public List<String> getIpAncestors() {
        return ipAncestors;
    }

    public void addIpAncestor(String ip) {
        this.ipAncestors.add(ip);
    }

    public String toJson() {
        ObjectMapper objectMapper = new ObjectMapper();
        try {
            return objectMapper.writeValueAsString(this);
        } catch (JsonProcessingException e) {
            e.printStackTrace();  // Log the full stack trace for easier debugging
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

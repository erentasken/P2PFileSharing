package com.example;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.Collections;
import java.util.List;

public class FileRequestNotification {
    private int ttl;
    private boolean have;
    private DirectoryFile file;
    private List<String> ipAncestors;

    @JsonIgnore
    private boolean ttlValid;

    // Constructor
    public FileRequestNotification(int ttl, boolean have, List<String> ipAncestors, DirectoryFile file) {
        this.ttl = ttl;
        this.have = have;
        this.file = file;
        this.ipAncestors = ipAncestors;
    }

    // Static method to parse JSON string into object
    public static FileRequestNotification parseJson(String jsonString) {
        ObjectMapper objectMapper = new ObjectMapper();
        try {
            JsonNode rootNode = objectMapper.readTree(jsonString);
            int ttl = rootNode.get("ttl").asInt();
            boolean have = rootNode.get("have").asBoolean();
            List<String> ipAncestors = objectMapper.readValue(
                rootNode.get("ipAncestors").toString(),
                objectMapper.getTypeFactory().constructCollectionType(List.class, String.class)
            );

            DirectoryFile file = objectMapper.readValue(
                rootNode.get("file").toString(),
                DirectoryFile.class
            );

            return new FileRequestNotification(ttl, have, ipAncestors, file);
        } catch (Exception e) {
            return null;
        }
    }

    // Method to decrease TTL and return the result
    public int decreaseTtl() {
        if (ttl > 0) {
            this.ttl--;
            return this.ttl;
        } else {
            return -1;
        }
    }

    // Getters and Setters
    public int getTtl() {
        return ttl; // Added getter for ttl
    }

    public boolean isHave() {
        return have;
    }

    public void setHave(boolean have) {
        this.have = have;
    }

    public DirectoryFile getFile() {
        return file;
    }

    public List<String> getIpAncestors() {
        return ipAncestors;
    }

    public void addIpAncestor(String ip) {
        ipAncestors.add(ip);
    }

    // Convert object to JSON string
    public String toJson() {
        ObjectMapper objectMapper = new ObjectMapper();
        try {
            // Ensure ttl field is included
            return objectMapper.writeValueAsString(this);
        } catch (JsonProcessingException e) {
            e.printStackTrace();  // Log the full stack trace for easier debugging
            return null;
        }
    }

    @Override
    public String toString() {
        return "FileRequestNotification{" +
                "ttl=" + ttl +  // Ensure ttl is printed in toString
                ", have=" + have +
                ", file=" + file +
                ", ipAncestors=" + ipAncestors +
                '}';
    }

    public static void main(String[] args) {
        try {
            DirectoryFile directoryFile = new DirectoryFile("device123", "exampleFile.txt", "hashValue123", "1024");

            FileRequestNotification notification = new FileRequestNotification(10, true, List.of("192.168.1.1"), directoryFile);

            String json = notification.toJson();
            System.out.println(json);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}

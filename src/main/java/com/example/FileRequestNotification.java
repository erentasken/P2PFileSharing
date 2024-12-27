package com.example;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class FileRequestNotification implements Serializable {
    private static final int CHUNK_SIZE = 256 * 10; // 256 KB

    @JsonProperty("ttl")
    private int ttl;

    @JsonProperty("networkFile")
    private NetworkFile file;

    @JsonProperty("ipAncestors")
    private List<String> ipAncestors = new ArrayList<>();

    @JsonProperty("receivedChunks")
    private List<Integer> receivedChunks;

    private static final ObjectMapper objectMapper = new ObjectMapper();

    @JsonIgnore
    private List<Integer> unreceivedChunks;

    @JsonProperty("directoryFile")
    private DirectoryFile directoryFile;

    @JsonCreator
    public FileRequestNotification(
            @JsonProperty("ttl") int ttl,
            @JsonProperty("ipAncestors") List<String> ipAncestors,
            @JsonProperty("networkFile") NetworkFile file,
            @JsonProperty("receivedChunks") List<Integer> receivedChunks
    ) {
        this.ttl = ttl;
        this.ipAncestors = ipAncestors != null ? ipAncestors : new ArrayList<>();
        this.file = file;
        this.receivedChunks = receivedChunks != null ? receivedChunks : new ArrayList<>();
        this.unreceivedChunks = unreceivedChunks != null ? unreceivedChunks : new ArrayList<>();

    }

    public static FileRequestNotification parseJson(String jsonString) {
        try {
            return objectMapper.readValue(jsonString, FileRequestNotification.class);
        } catch (Exception e) {
            return null;
        }
    }

    public List<Integer> getUnreceivedChunks() {
        long fileSize = Long.parseLong(file.getFileSize());
        int totalChunks = (int) Math.ceil((double) fileSize / CHUNK_SIZE);
        List<Integer> unreceivedChunks = new ArrayList<>();
        for (int i = 0; i < totalChunks; i++) {
            if (!receivedChunks.contains(i)) {
                unreceivedChunks.add(i);
            }
        }
        return Collections.unmodifiableList(unreceivedChunks);
    }

    public int decreaseTtl() {
        if (ttl > 0) {
            this.ttl--;
            return this.ttl;
        } else {
            return -1;
        }
    }

    public void addReceivedChunk(int chunkNo) {
        receivedChunks.add(chunkNo);
    }

    public DirectoryFile getDirectoryFile() {
        return file.getDirectoryFile();
    }

    public List<String> getIpAncestors() {
        return ipAncestors;
    }

    public void addIpAncestor(String ip) {
        ipAncestors.add(ip);
    }

    public String toJson() {
        try {
            return objectMapper.writeValueAsString(this);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    @Override
    public String toString() {
        return toJson();
    }

    // public static void main(String[] args) {
    //     DirectoryFile requestedFile = new DirectoryFile("10", "hello1.txt", "72004025f7fdedd51ac6ac478a8c85b89bf01e2df8cc204a6ff64e1421265a18", "11");
    //     NetworkFile networkFile = new NetworkFile(new ArrayList<>(), null, requestedFile, -1);

    //     FileRequestNotification fileRequestNotificationSent = new FileRequestNotification(
    //         3, new ArrayList<>(), networkFile, new ArrayList<Integer>()
    //     );

    //     String jsonString = fileRequestNotificationSent.toJson();
    //     System.out.println(jsonString);

    //     try  {
    //         FileRequestNotification fileRequestNotificationReceived = FileRequestNotification.parseJson(jsonString);
    //     }catch (Exception e) {
    //         e.printStackTrace();
    //     }
    // }
}
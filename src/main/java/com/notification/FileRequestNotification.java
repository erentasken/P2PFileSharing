package com.notification;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.master.FileManager;
import com.models.DirectoryFile;
import com.models.NetworkFile;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class FileRequestNotification implements Serializable {
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
        int totalChunks = (int) Math.ceil((double) fileSize / FileManager.CHUNK_SIZE);
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
}
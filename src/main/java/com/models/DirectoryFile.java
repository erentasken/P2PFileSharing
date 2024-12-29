package com.models;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.annotation.JsonInclude;

import java.io.Serializable;
import java.util.Objects;

@JsonInclude(JsonInclude.Include.NON_EMPTY)
public class DirectoryFile implements Serializable {
    @JsonProperty("fileName")
    private String fileName;

    @JsonProperty("fileHash")
    private String fileHash;

    @JsonProperty("device")
    private String device;

    @JsonProperty("fileSize")
    private String fileSize;

    private static final ObjectMapper objectMapper = new ObjectMapper();

    @JsonCreator
    public DirectoryFile(
            @JsonProperty("device") String device,
            @JsonProperty("fileName") String fileName,
            @JsonProperty("fileHash") String fileHash,
            @JsonProperty("fileSize") String fileSize
    ) {
        this.fileName = fileName != null ? fileName : "";
        this.fileHash = fileHash != null ? fileHash : "";
        this.device = device != null ? device : "";
        this.fileSize = fileSize != null ? fileSize : "";
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        DirectoryFile that = (DirectoryFile) obj;
        return Objects.equals(fileName, that.fileName) &&
               Objects.equals(fileHash, that.fileHash) &&
               Objects.equals(device, that.device);
    }

    @JsonIgnore
    public String getFileName() {
        return fileName;
    }

    @JsonIgnore
    public String getFileHash() {
        return fileHash;
    }

    @JsonIgnore
    public String getDevice() {
        return device;
    }

    @JsonIgnore
    public String getFileSize() {
        return fileSize;
    }

    @Override
    public String toString() {
        return toJson();
    }

    public String toJson() { 
        try {
            return objectMapper.writeValueAsString(this);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }
}
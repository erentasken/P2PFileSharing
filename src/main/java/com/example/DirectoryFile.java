package com.example;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class DirectoryFile {
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

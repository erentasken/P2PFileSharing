package com.example;

import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class DirectoryFile {
    private String fileName;
    private String fileHash;
    private String device;
    private String fileSize;

    @JsonCreator
    public DirectoryFile(
        @JsonProperty("device") String device,
        @JsonProperty("fileName") String fileName, 
        @JsonProperty("fileHash") String fileHash,
        @JsonProperty("fileSize") String fileSize
    ) {
        this.fileName = fileName;
        this.fileHash = fileHash;
        this.device = device;
        this.fileSize = fileSize;
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

    public String getFileName() {
        return fileName;
    }

    public String getFileHash() {
        return fileHash;
    }

    public String getDevice() {
        return device;
    }

    public String getFileSize() {
        return fileSize;
    }

    @Override
    public String toString() {
        return "DirectoryFile{" +
                "fileName='" + fileName + '\'' +
                ", fileHash='" + fileHash + '\'' +
                ", device='" + device + '\'' +
                ", fileSize='" + fileSize + '\'' +
                '}';
    }
}

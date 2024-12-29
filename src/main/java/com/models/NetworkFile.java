package com.models;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.io.Serializable;
import java.util.Collections;
import java.util.List;

public class NetworkFile implements Serializable {

    @JsonProperty("netIpAncestors")
    private List<String> ipAncestors;

    @JsonProperty("data")
    private byte[] data;

    @JsonProperty("directoryFile")
    private DirectoryFile file;

    @JsonProperty("chunkNo")
    private int chunkNo;

    @JsonCreator
    public NetworkFile(
            @JsonProperty("ipAncestorsNetwork") List<String> ipAncestors,
            @JsonProperty("data") byte[] data,
            @JsonProperty("directoryFile") DirectoryFile file,
            @JsonProperty("chunkNo") int chunkNo
    ) {
        this.ipAncestors = ipAncestors != null ? ipAncestors : Collections.emptyList();
        this.data = data != null ? data : new byte[0];
        this.file = file != null ? file : new DirectoryFile("", "", "", "");
        this.chunkNo = chunkNo  != -1 ? chunkNo : -1;
    }

    @JsonIgnore
    public byte[] getData() {
        return this.data;
    }

    @JsonIgnore
    public String getFileName() {
        return this.file.getFileName();
    }

    @JsonIgnore
    public String getFileHash() {
        return this.file.getFileHash();
    }

    @JsonIgnore
    public long getDataSize() {
        return this.data != null ? this.data.length : 0L;
    }
    
    @JsonIgnore
    public int getChunkNo() {
        return this.chunkNo;
    }

    @JsonIgnore
    public String getFileSize() {
        return this.file.getFileSize();
    }

    @JsonIgnore
    public DirectoryFile getDirectoryFile() {
        return this.file;
    }

    public String popIp() {
        if (!ipAncestors.isEmpty()) {
            String poppedIp = ipAncestors.get(ipAncestors.size() - 1);
            ipAncestors.remove(ipAncestors.size() - 1);
            String deviceNo = poppedIp.split("\\.")[3];
            ipAncestors.removeIf(ip -> ip.split("\\.")[3].equals(deviceNo));
            return poppedIp;
        }
        return "";
    }

    @Override
    public String toString() {
        return "NetworkFile{" +
                "ipAncestorsNetwork=" + ipAncestors +
                ", data=" + data +
                ", directoryFile=" + file +
                ", chunkNo=" + chunkNo +
                '}';
    }
}

package com.example;

import java.io.Serializable;
import java.util.List;

public class NetworkFile implements Serializable { 
    private List<String> ipAncestors;
    private byte[] file;
    private String fileName;

    public NetworkFile(List<String> ipAncestors, byte[] file, String fileName) { 
        this.ipAncestors = ipAncestors;
        this.file = file;
        this.fileName = fileName;
    }

    public byte[] getFile() { 
        return this.file;
    }

    public String getFileName() { 
        return this.fileName;
    }

    public String popIp() {
        if (ipAncestors.size() != 0) { 
            // Pop the last IP
            String poppedIp = ipAncestors.get(ipAncestors.size() - 1);
            ipAncestors.remove(ipAncestors.size() - 1);
    
            String deviceNO = poppedIp.split("\\.")[3];
    
            ipAncestors.removeIf(ip -> ip.split("\\.")[3].equals(deviceNO));
    
            return poppedIp;
        } else {
            return "";
        }
    }
    

}

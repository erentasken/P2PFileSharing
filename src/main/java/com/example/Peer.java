package com.example;

import java.util.*;

public class Peer {

    private List<DirectoryNotification> sourceList;
    FileManager fileManager;

    public Peer(FileManager fileManager) {
        this.sourceList = new ArrayList<>();
        this.fileManager = fileManager;
    }


    public void addSource(DirectoryNotification notification) {
        sourceList.add(notification);
    }

    public void printSources() {
        System.out.println("\nCurrent Sources:");
        sourceList.forEach((source)-> {
            System.out.println("Ancestor: " + source.getIpAncestors());
            System.out.println("Files: \n");
            source.getDirectory().forEach((file) -> {
                System.out.println("Dev : " + file.getDevice() + " | " + file.getFileName() + " : " + file.getFileHash());
            });
        });
    }


}

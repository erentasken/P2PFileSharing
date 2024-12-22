package com.example;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class Peer {
    private final Map<String, Set<String>> peerGraph = new ConcurrentHashMap<>();
    private String lastPeer = null;
    FileManager fileManager= new FileManager();

    public synchronized void addPeer(String senderAddress) {
        peerGraph.putIfAbsent(senderAddress, new HashSet<>());

        if (lastPeer != null && !lastPeer.equals(senderAddress)) {
            peerGraph.get(lastPeer).add(senderAddress);
            peerGraph.get(senderAddress).add(lastPeer);
            // System.out.println("Connected " + lastPeer + " -> " + senderAddress);
        }

        lastPeer = senderAddress;
    }

    public String getFile(String fileName, String fileHash) { 
        return fileManager.returnFile(fileName, fileHash);
    }

    public HashMap<String, String> getFiles() { 
        return fileManager.getFiles();
    }

    public void syncFileDirectory() { 
        fileManager.syncFileMap();
    }

    // Print the current peer graph
    public synchronized void printGraph() {
        System.out.println("\nCurrent Peer Graph:");
        peerGraph.forEach((peer, connections) -> System.out.println(peer + " -> " + connections));
    }

    // Optional: Get all peers for testing or extension
    public synchronized Map<String, Set<String>> getPeerGraph() {
        return new HashMap<>(peerGraph);
    }
}

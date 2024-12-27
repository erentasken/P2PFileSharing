package com.example;

import java.util.*;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class Peer {

    private List<DirectoryNotification> sourceList;
    FileManager fileManager;
    private final Lock lock = new ReentrantLock();  // Mutex lock to synchronize access to sourceList

    public Peer(FileManager fileManager) {
        this.sourceList = new ArrayList<>();
        this.fileManager = fileManager;

        ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);

        scheduler.scheduleAtFixedRate(() -> {
            lock.lock();
            try {
                sourceList.clear();
            } finally {
                lock.unlock();
            }
        }, 0, 7, TimeUnit.SECONDS);
    }

    public void addSource(DirectoryNotification notification) {
        lock.lock();

        boolean exists = false;

        try {
            for (DirectoryNotification dirNotif : sourceList) {
                if(dirNotif.getIpAncestors().equals(notification.getIpAncestors())) {
                    exists = true;
                }
            }

            if(!exists){
                sourceList.add(notification);
            } 
    

        }catch(Exception e) { 
        }
         finally {
            lock.unlock();
        }
    }

    public void requestForFile(DirectoryFile requestedFile){ 
        for(DirectoryNotification dirNotif : sourceList) {
            if(dirNotif.getDirectory().contains(requestedFile)) {
                System.out.println("found the file ");
            } 
        }
    }

    public void printSources() {
        lock.lock();
        try {

            System.out.println("---------------------------------------------------------------------------");

            System.out.println("lengtht of source : " + sourceList.size());

            sourceList.forEach(source -> {
                var ancestors = source.getIpAncestors();

                System.out.println("\nCurrent Sources:\n");
                System.out.println("Ancestor: " + ancestors);
                System.out.println("Files:");
                source.getDirectory().forEach(file -> {
                    System.out.println("Dev : " + file.getDevice() + " | " + "File size: " + file.getFileSize() + " | " + file.getFileName() + " : " + file.getFileHash());
                });
            });

            System.out.println("---------------------------------------------------------------------------");
        } finally {
            lock.unlock();
        }
    }
}

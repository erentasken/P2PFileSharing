package com.example;

import java.net.*;
import java.util.*;

public class P2PRecog {
    private static final int PORT = 9876;

    private static final Peer peerManager = new Peer();

    private static FileManager fileManager = new FileManager();

    public static void main(String[] args) throws UnknownHostException, InterruptedException {
        Thread listenerThread = new Thread(() -> listenBroadcast());
        listenerThread.start();
    
        Thread sendBroadcastThread = new Thread(
            () -> {

                try {
                    String notificString = fileManager.buildNotificationJson(3, InetAddress.getLocalHost().getHostAddress());

                    
                    while (true) {
                        sendBroadcast(notificString);
                        Thread.sleep(5000);
                    }



                } catch (UnknownHostException e) {
                    e.printStackTrace();
                } catch (InterruptedException e) {
                                    e.printStackTrace();
                    }
            });
        sendBroadcastThread.start();
        
        // while (true) {
        //     String localHostAddress = InetAddress.getLocalHost().getHostAddress();
        //     if (localHostAddress.equals("10.23.5.50") || localHostAddress.equals("10.23.6.50")) {
        //         peerManager.printGraph();
        //     }
        // }
    }
    
    public static void listenBroadcast() {
        try (DatagramSocket socket = new DatagramSocket(PORT)) {
            socket.setBroadcast(true);
    
            while (true) {
                byte[] buffer = new byte[1024];
                DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
    
                // Receive broadcast
                socket.receive(packet);
                String message = new String(packet.getData(), 0, packet.getLength());
                String senderAddress = packet.getAddress().getHostAddress();
    
                if (!senderAddress.equals(InetAddress.getLocalHost().getHostAddress())) {
                    String localHostAddress = InetAddress.getLocalHost().getHostAddress();

                    if (localHostAddress.equals("10.23.5.50") || localHostAddress.equals("10.23.6.50")) {
                        DirectoryNotification notification = new DirectoryNotification(3, InetAddress.getLocalHost().getHostAddress(), 1, 1, new ArrayList<>());
                        notification = notification.parseJson(message);
    
                        System.out.println("Received broadcast from " + senderAddress);
                        System.out.println("TTL: " + notification.getTtl());
                        System.out.println("Source IP: " + notification.getSourceIP());
                        System.out.println("Node No: " + notification.getNodeNo());
                        System.out.println("Network No: " + notification.getNetworkNo());
                        System.out.println("Directory:");
                        notification.getDirectory().forEach(file -> System.out.println("  " + file.getFileName() + " (" + file.getFileHash() + ")"));
    
                    }

                    

                    peerManager.addPeer(senderAddress); // Add sender to peer manager
                    
                   
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    
    public static void sendBroadcast(String message) {
        try {
            byte[] buffer = message.getBytes();
            try (DatagramSocket socket = new DatagramSocket()) {
                socket.setBroadcast(true);
    
                peerManager.syncFileDirectory();
    
                Enumeration<NetworkInterface> networkInterfaces = NetworkInterface.getNetworkInterfaces();
                while (networkInterfaces.hasMoreElements()) {
                    NetworkInterface networkInterface = networkInterfaces.nextElement();
    
                    if (networkInterface.isLoopback() || !networkInterface.isUp()) {
                        continue;
                    }
    
                    for (InterfaceAddress interfaceAddress : networkInterface.getInterfaceAddresses()) {
                        InetAddress broadcastAddress = interfaceAddress.getBroadcast();
    
                        if (broadcastAddress != null) {
                            DatagramPacket packet = new DatagramPacket(buffer, buffer.length, broadcastAddress, PORT);
                            socket.send(packet);
                        }
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
   
}


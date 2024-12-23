package com.example;

import java.net.*;
import java.util.*;

public class P2PRecog {
    private static final int PORT = 9876;

    private static FileManager fileManager = new FileManager();

    private static Peer peerManager = new Peer(fileManager);

    private static String hostIP;

    public static void main(String[] args) throws UnknownHostException, InterruptedException {

        hostIP = InetAddress.getLocalHost().getHostAddress();

        System.out.println("IP : " + hostIP);

        Thread listenerThread = new Thread(() -> listenBroadcast());
        listenerThread.start();
        Thread sendBroadcastThread = new Thread(
            () -> {
                try {
                    String notificString = fileManager.buildNotificationJson(3, Arrays.asList(hostIP));

                    while (true) {
                        if (hostIP.equals("10.23.2.20")) { 
                            sendBroadcast(notificString);
                            Thread.sleep(5000);
                        }
                    }
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            });
        sendBroadcastThread.start();
        
        while (true) {
            String localHostAddress = hostIP;
            if (localHostAddress.equals("10.23.5.50")) {
                peerManager.printSources();
                Thread.sleep(5000);
            }
        }
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
    
                if (!senderAddress.equals(hostIP)) {

                    DirectoryNotification notification = new DirectoryNotification(0, new ArrayList<>(), new ArrayList<>());
                    notification = new DirectoryNotification(3, new ArrayList<>(), new ArrayList<>());
                    notification = notification.parseJson(message);

                    notification.processNotification(senderAddress, fileManager);
                    
                    peerManager.addSource(notification);

                    if (notification.getTtl() > 0) {
                        sendBroadcast(notification.toJson());
                    }

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


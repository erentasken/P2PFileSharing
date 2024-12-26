package com.example;

import java.net.*;
import java.util.*;

public class P2PRecog {
    private static final int PORT = 9876;

    private static FileManager fileManager = new FileManager();

    private static Peer peerManager = new Peer(fileManager);

    private static String hostIP;

    private static DirectoryNotification notificationReceive;
    private static DirectoryNotification notificationSend;

    public static void main(String[] args) throws UnknownHostException, InterruptedException {

        hostIP = InetAddress.getLocalHost().getHostAddress();

        System.out.println("Host IP: " + hostIP);

        fileManager.setDevice(hostIP.split("\\.")[3]);

        Thread listenerThread = new Thread(() -> listenBroadcast());
        listenerThread.start();

        //Test
        Thread sendBroadcastThread = new Thread(
            () -> {
                try {
                    while (true) {
                        if (hostIP.equals("10.23.2.20") || hostIP.equals("10.23.1.10")) { 

                             
                            notificationSend = new DirectoryNotification(3, new ArrayList<>(), fileManager.getFiles());

                            sendBroadcast(notificationSend);

                            Thread.sleep(5000);
                        }
                    }
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            });
        sendBroadcastThread.start();
        //

    }
    
    public static void listenBroadcast() {
        try (DatagramSocket socket = new DatagramSocket(PORT)) {
            socket.setBroadcast(true);
    
            while (true) {
                byte[] buffer = new byte[1024];
                DatagramPacket packet = new DatagramPacket(buffer, buffer.length);

                socket.receive(packet);
                String message = new String(packet.getData(), 0, packet.getLength());
                String senderAddress = packet.getAddress().getHostAddress();
    
                if (!senderAddress.split("\\.")[3].equals(hostIP.split("\\.")[3])) {
                    notificationReceive = new DirectoryNotification(3, new ArrayList<>(), new ArrayList<>());

                    notificationReceive = DirectoryNotification.parseJson(message);

                    if (notificationReceive == null) {
                        continue;
                    }

                    notificationReceive.processNotification(senderAddress, fileManager);

                    peerManager.addSource(notificationReceive);
    
                    //Test 
                    if (hostIP.equals("10.23.5.50")) {
                        peerManager.printSources();
                    }
                    //

                    if (notificationReceive.getTtl() > 0) {
                        sendBroadcast(notificationReceive);
                    }

                }

            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void sendBroadcast(DirectoryNotification notification) {
        try {
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

                            notification.addIpAncestor(interfaceAddress.getAddress().getHostAddress());

                            String message = notification.toJson();

                            byte[] buffer = message.getBytes();

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


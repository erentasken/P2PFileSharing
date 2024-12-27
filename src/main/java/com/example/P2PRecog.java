package com.example;

import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.InterfaceAddress;
import java.net.NetworkInterface;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Enumeration;

public class P2PRecog {
    private static final int PORT = 9876;

    private static FileManager fileManager = new FileManager();

    private static Peer peerManager = new Peer(fileManager);

    private static String hostIP;

    private static DirectoryNotification notificationReceive;
    private static DirectoryNotification notificationSend;

    private static FileRequestNotification fileRequestNotificationSent;
    private static FileRequestNotification fileRequestNotificationReceive;

    public static void main(String[] args) throws UnknownHostException, InterruptedException {

        hostIP = InetAddress.getLocalHost().getHostAddress();

        System.out.println("Host IP: " + hostIP);

        fileManager.setDevice(hostIP.split("\\.")[3]);

        Thread listenerThread = new Thread(() -> listenBroadcast());
        listenerThread.start();

        Thread tcpListenerThread = new Thread(()-> tcpConnectionRecevier());
        tcpListenerThread.start();

        //Test
        Thread sendBroadcastThread = new Thread(
            () -> {
                // while (true) {
                    
                //     if (hostIP.equals("10.23.1.10")) { 
                //         notificationSend = new DirectoryNotification(3, new ArrayList<>(), fileManager.getFiles());
                //         sendBroadcast(notificationSend); 
                //     }
                    




                //     if (hostIP.equals("10.23.3.40")) { 
                //         DirectoryFile requestedFile = new DirectoryFile("10", "hello1.txt", "72004025f7fdedd51ac6ac478a8c85b89bf01e2df8cc204a6ff64e1421265a18", "11");
                //         fileRequestNotificationSent = new FileRequestNotification(3, true, new ArrayList<>(), requestedFile);
                //         sendBroadcast(fileRequestNotificationSent);
                //     }

                //     try {
                //         Thread.sleep(5000);
                //     } catch (InterruptedException e) {
                //         e.printStackTrace();
                //     }

                // }



                    
                if (hostIP.equals("10.23.1.10")) { 
                    notificationSend = new DirectoryNotification(3, new ArrayList<>(), fileManager.getFiles());
                    sendBroadcast(notificationSend); 
                }
                




                if (hostIP.equals("10.23.3.40")) { 
                    // Handle the file request, handle the chunk received, send unreceived chunks information
                    DirectoryFile requestedFile = new DirectoryFile("10", "hello1.txt", "72004025f7fdedd51ac6ac478a8c85b89bf01e2df8cc204a6ff64e1421265a18", "11");
                    fileRequestNotificationSent = new FileRequestNotification(3, true, new ArrayList<>(), requestedFile);
                    sendBroadcast(fileRequestNotificationSent);
                }

                try {
                    Thread.sleep(5000);
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
                   
                    FileRequestNotificationHandling(message, senderAddress);

                    DirectoryNotificationHandling(message, senderAddress);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void FileRequestNotificationHandling(String message, String senderAddress) {
        try { 
            fileRequestNotificationReceive = FileRequestNotification.parseJson(message);
        } catch (Exception e) {
            return;
        }

        if (fileRequestNotificationReceive == null) {
            return;
        }

        if (fileRequestNotificationReceive.decreaseTtl() == -1) { 
            return;
        }

        // Initialize file sending chain.
        if (fileManager.getFiles().contains(fileRequestNotificationReceive.getFile())){
            // System.out.println("There is file found in : " + hostIP + " requested file : " + fileRequestNotificationReceive.getFile() );
            // System.out.println("Ancestors: ");
            // System.out.println(fileRequestNotificationReceive.getIpAncestors());


            //Create network file 
            NetworkFile networkFile = new NetworkFile(fileRequestNotificationReceive.getIpAncestors(), fileManager.returnFile(fileRequestNotificationReceive.getFile().getFileName(), fileRequestNotificationReceive.getFile().getFileHash()), fileRequestNotificationReceive.getFile().getFileName());
            
            //Take the next ip for sending message
            String nextIp = networkFile.popIp();

            //Create tcp on that ip, send the network file
            tcpConnectionSend(nextIp, networkFile);

            // System.out.println("initialization of tcp connection to " + nextIp);
        };

        sendBroadcast(fileRequestNotificationReceive);      
    }

    // file sending chain listener, and protect the chain, if it is last stop take the file into local, MUST BE OPEN ALWAYS.....
    public static void tcpConnectionRecevier(){ 
        try (ServerSocket serverSocket = new ServerSocket(9876)) { // Listening on port 9876

            while (true) {
                // System.out.println("Waiting for incoming file requests...");
    
                try (Socket socket = serverSocket.accept();
                     InputStream inputStream = socket.getInputStream();
                     ObjectInputStream objectInputStream = new ObjectInputStream(inputStream)) {
        

                    // System.out.println("connection established");
                        // Read the incoming DirectoryFile object
                    NetworkFile file = (NetworkFile) objectInputStream.readObject();

                    // System.out.println("received file  : " + file.getFileName());

                    String nextIp = file.popIp();

                    // System.out.println("connection initialization with the : " + nextIp);


                    if (nextIp == "") { // means you are the receiver. 
                        fileManager.saveFile(file);
                    }else { // means you should continue the chain. 
                        tcpConnectionSend(nextIp, file);

                        // System.out.println("send tcp connection to " + nextIp);
                    }
    
                } catch (IOException | ClassNotFoundException e) {
                    // System.err.println("Error receiving file request: " + e.getMessage());
                }    
            }

            
        } catch (IOException e) {
            System.err.println("Error starting server socket: " + e.getMessage());
        }
    }




    public static void tcpConnectionSend(String ip, NetworkFile file) {
        try (Socket socket = new Socket(ip, 9876); // Connect to the ancestor at port 9876
             OutputStream outputStream = socket.getOutputStream();
             ObjectOutputStream objectOutputStream = new ObjectOutputStream(outputStream)) {
    
            // Send the DirectoryFile object
            objectOutputStream.writeObject(file);
            objectOutputStream.flush();
    
        } catch (IOException e) {
        }
    }





    public static void DirectoryNotificationHandling(String message, String senderAddress) {
        try { 
            notificationReceive = DirectoryNotification.parseJson(message);
        } catch (Exception e) {
            return;
        }

        if (notificationReceive == null) {
            return;
        }

        if (notificationReceive.decreaseTtl() == -1) {
            return;
        }

        // Check that, needs to detect the existing and nonexisting files in sharedFiles 
        notificationReceive.processNotification(senderAddress, fileManager);

        sendBroadcast(notificationReceive);
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
        
        peerManager.addSource(notification);
    }

    public static void sendBroadcast(FileRequestNotification notification) {
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


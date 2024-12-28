package com.example;

import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.io.RandomAccessFile;
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
import java.util.List;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;


public class P2PRecog {
    private static final int PORT = 9876;

    private static FileManager fileManager;

    private static Peer peerManager = new Peer(fileManager);

    private static String hostIP;

    private static DirectoryNotification notificationReceive;
    private static DirectoryNotification notificationSend;
    private static FileRequestNotification fileRequestNotificationSent;
    private static FileRequestNotification fileRequestNotificationReceive;

    private static ConcurrentHashMap<String, ArrayList<Integer>> InProcessFileTracer = new ConcurrentHashMap<>();

    private static DatagramSocket sharedSocket;


    public static void main(String[] args) throws UnknownHostException, InterruptedException {

        try {
            sharedSocket = new DatagramSocket(PORT);
        } catch (IOException e) {
            System.err.println("Error initializing sharedSocket: " + e.getMessage());
            e.printStackTrace();
            return; // Exit the method if socket initialization fails
        }
        

        hostIP = InetAddress.getLocalHost().getHostAddress();

        System.out.println("Host IP: " + hostIP);

        fileManager = new FileManager(hostIP.split("\\.")[3]);

        Thread listenBroadcast = new Thread(() -> listenBroadcast());
        listenBroadcast.start();

        Thread tcpListenerThread = new Thread(()-> tcpConnectionRecevier());
        tcpListenerThread.start();

        //Test
        Thread sendBroadcastThread = new Thread(
            () -> {
                notificationSend = new DirectoryNotification(3, new ArrayList<>(), fileManager.getFiles());
                sendBroadcast(notificationSend); 

                //Test
                if (hostIP.equals("10.23.1.10")) {
                    try {
                        Thread.sleep(5000);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    RequestForFile("fileToSend", "39de4e81171e5754cfe38e39bb415af31a98d0d703c087078925abb8541341af", null);
                }

                while (true ) { 
                    notificationSend = new DirectoryNotification(3, new ArrayList<>(), fileManager.getFiles());
                    sendBroadcast(notificationSend);
                    try {
                        Thread.sleep(5000);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }


            });
        sendBroadcastThread.start();
        //

        // while(true) { 

        // }


        // Test
        // while ( true ) { 
        //     if (hostIP.equals("10.23.1.10")) { 
        //         peerManager.printSources();
        //         Thread.sleep(5000);
        //     }
        // }
    }

    public static void RequestForFile(String fileName, String fileHash, ArrayList<Integer> receivedChunks) {
        if (receivedChunks == null) {
            receivedChunks = new ArrayList<>();
        }

        final ArrayList<Integer> finalReceivedChunks = receivedChunks;

        ArrayList<DirectoryFile> requestedFileList = peerManager.getRequestedFile("fileToSend", "39de4e81171e5754cfe38e39bb415af31a98d0d703c087078925abb8541341af");
        requestedFileList.forEach((file)-> { 
            InProcessFileTracer.putIfAbsent(file.getFileHash(), new ArrayList<>());
            NetworkFile networkFile = new NetworkFile(new ArrayList<>(), null, file, -1);
            fileRequestNotificationSent = new FileRequestNotification(3, new ArrayList<>(), networkFile, finalReceivedChunks);
            sendBroadcast(fileRequestNotificationSent);
        });
    }

    public static void listenBroadcast() {
        try {
            while (true) {
                byte[] buffer = new byte[1024];
                DatagramPacket packet = new DatagramPacket(buffer, buffer.length);

                sharedSocket.receive(packet);
                String message = new String(packet.getData(), 0, packet.getLength());
                String senderAddress = packet.getAddress().getHostAddress();

                if (!senderAddress.split("\\.")[3].equals(hostIP.split("\\.")[3])) {
                    // Process notifications
                    DirectoryNotificationHandling(message, senderAddress);
                    FileRequestNotificationHandling(message, senderAddress);
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


        DirectoryFile reqFile = fileRequestNotificationReceive.getDirectoryFile();

        if (fileManager.getFiles().contains(reqFile)){

            List<Integer> unreceivedChunks = fileRequestNotificationReceive.getUnreceivedChunks();
            if (unreceivedChunks == null || unreceivedChunks.isEmpty()) {
                System.out.println("No chunks to process");
                return; // No chunks to process
            }

            int randomIndex = new Random().nextInt(unreceivedChunks.size());
            int chunkIndex = unreceivedChunks.get(randomIndex);

            byte[] data = fileManager.getChunk(reqFile.getFileName(), reqFile.getFileHash(), chunkIndex);

            NetworkFile networkFile = new NetworkFile(fileRequestNotificationReceive.getIpAncestors(), data, reqFile, chunkIndex);
            
            String nextIp = networkFile.popIp();

            if (nextIp !="") { 
                // System.out.println("Unreceived Chunks: " + unreceivedChunks);
                tcpConnectionSend(nextIp, networkFile);
            }

        };

        sendBroadcast(fileRequestNotificationReceive);      
    }

    public static void tcpConnectionSend(String ip, NetworkFile file) {
        try (Socket socket = new Socket(ip, 9876); // Connect to the ancestor at port 9876
             OutputStream outputStream = socket.getOutputStream();
             ObjectOutputStream objectOutputStream = new ObjectOutputStream(outputStream)) {
    
            // Send the DirectoryFile object
            objectOutputStream.writeObject(file);
            objectOutputStream.flush();
    
        } catch (IOException e) {
            System.err.println("Error sending file request: " + e.getMessage());
        }
    }

    // file sending chain listener, and protect the chain, if it is last stop take the file into local, MUST BE OPEN ALWAYS.....
    public static void tcpConnectionRecevier(){ 
        try (ServerSocket serverSocket = new ServerSocket(9876)) { // Listening on port 9876

            while (true) {
                try (Socket socket = serverSocket.accept();
                     InputStream inputStream = socket.getInputStream();
                     ObjectInputStream objectInputStream = new ObjectInputStream(inputStream)) {
        

                    NetworkFile file = (NetworkFile) objectInputStream.readObject();

                    String nextIp = file.popIp();

                    boolean isChunkReceived;
                    try { 
                        isChunkReceived = InProcessFileTracer.get(file.getFileHash()).contains(file.getChunkNo());
                    } catch (NullPointerException e) {
                        isChunkReceived = false;
                    }

                    if (nextIp == "" && InProcessFileTracer.containsKey(file.getFileHash()) && !isChunkReceived) { // means you are the receiver. 

                        int chunkNo = fileManager.saveChunks(file);

                        if (chunkNo == -1) {
                            System.err.println("Error saving chunk to file");
                            continue;
                        }

                        InProcessFileTracer.get(file.getFileHash()).add(chunkNo);

                        if (isChunkReceived) {
                            System.out.println("chunk already taken " + " : " + file.getChunkNo());
                        }

                        System.out.println("Chunk received: " + file.getChunkNo() + " from " + socket.getInetAddress().getHostAddress());

                        ArrayList<Integer> receivedChunks = InProcessFileTracer.get(file.getFileHash());
                        if (fileRequestNotificationSent.getUnreceivedChunks().isEmpty()) {
                            InProcessFileTracer.remove(file.getFileHash());
                        }else {
                            RequestForFile(file.getFileName(), file.getFileHash(), receivedChunks);
                        }
                    }else if (nextIp != ""){ // means you should continue the chain.
                        tcpConnectionSend(nextIp, file);
                    }
    
                } catch (IOException | ClassNotFoundException e) {
                    System.err.println("Error receiving file request: " + e.getMessage());
                }    
            }

            
        } catch (IOException e) {
            System.err.println("Error starting server socket: " + e.getMessage());
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

        notificationReceive.addSharedFiles(fileManager);

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


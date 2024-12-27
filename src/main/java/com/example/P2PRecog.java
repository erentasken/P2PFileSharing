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
import java.util.HashMap;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;


public class P2PRecog {
    private static final int PORT = 9876;

    private static FileManager fileManager = new FileManager();

    private static Peer peerManager = new Peer(fileManager);

    private static String hostIP;

    private static DirectoryNotification notificationReceive;
    private static DirectoryNotification notificationSend;

    private static FileRequestNotification fileRequestNotificationSent;
    private static FileRequestNotification fileRequestNotificationReceive;

    private static ConcurrentHashMap<String, ArrayList<Integer>> InProcessFileTracer = new ConcurrentHashMap<>();

    private static int CHUNK_SIZE = 256 * 10 ^ 3; // 256 KB

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
                while ( true ) { 
                    notificationSend = new DirectoryNotification(3, new ArrayList<>(), fileManager.getFiles());
                    sendBroadcast(notificationSend); 
                    
                    if (hostIP.equals("10.23.3.40")) { 
                        // Handle the file request, handle the chunk received, send unreceived chunks information
                        DirectoryFile requestedFile = new DirectoryFile("20", "hello2.txt", "f6aba28fbc6eb2fae2b7722c766409f041d62797ea49446a5b2e72746fecb7a2", "8");
                        InProcessFileTracer.putIfAbsent(requestedFile.getFileHash(), new ArrayList<>());
    
                        NetworkFile networkFile = new NetworkFile(new ArrayList<>(), null, requestedFile, -1);
    
                        fileRequestNotificationSent = new FileRequestNotification(3, new ArrayList<>(), networkFile, new ArrayList<Integer>());
                        sendBroadcast(fileRequestNotificationSent);
                    }
                }
                
            });
        sendBroadcastThread.start();
        //

        // Test


        while ( true ) { 
            if (hostIP.equals("10.23.3.40")) { 
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

        DirectoryFile reqFile = fileRequestNotificationReceive.getDirectoryFile();

        // Initialize file sending chain.
        if (fileManager.getFiles().contains(reqFile)){

            List<Integer> unreceivedChunks = fileRequestNotificationReceive.getUnreceivedChunks();

            int randomIndex = new Random().nextInt(unreceivedChunks.size());
            int chunkIndex = unreceivedChunks.get(randomIndex);

            // Retrieve the corresponding chunk data
            byte[] data = fileManager.getChunk(reqFile.getFileName(), reqFile.getFileHash(), chunkIndex, CHUNK_SIZE);

            //Create network file 
            NetworkFile networkFile = new NetworkFile(fileRequestNotificationReceive.getIpAncestors(), data, reqFile, chunkIndex);
            
            //Take the next ip for sending message
            String nextIp = networkFile.popIp();

            //Create tcp on that ip, send the network file
            tcpConnectionSend(nextIp, networkFile);

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
        }
    }

    // file sending chain listener, and protect the chain, if it is last stop take the file into local, MUST BE OPEN ALWAYS.....
    public static void tcpConnectionRecevier(){ 
        try (ServerSocket serverSocket = new ServerSocket(9876)) { // Listening on port 9876

            while (true) {
                try (Socket socket = serverSocket.accept();
                     InputStream inputStream = socket.getInputStream();
                     ObjectInputStream objectInputStream = new ObjectInputStream(inputStream)) {
        

                    // Read the incoming DirectoryFile object
                    NetworkFile file = (NetworkFile) objectInputStream.readObject();

                    String nextIp = file.popIp();

                    if (nextIp == "" && InProcessFileTracer.containsKey(file.getFileHash())) { // means you are the receiver. 
                        // fileManager.saveFile(file);
                        String filePath = "./sharedFiles" + "/" + file.getFileName();

                        RandomAccessFile rAF = new RandomAccessFile(filePath, "rw");

                        long length = file.getDataSize();

                        int chunkNo = file.getChunkNo();

                        byte[] chunkData= file.getData();

                        rAF.setLength(length);

                        rAF.seek((long) chunkNo * CHUNK_SIZE);
                        rAF.write(chunkData);

                        InProcessFileTracer.get(file.getFileHash()).add(chunkNo);
                        
                        rAF.close();

                        

                        ArrayList<Integer> receivedChunks = InProcessFileTracer.get(file.getFileHash());

                        fileRequestNotificationSent = new FileRequestNotification(3, new ArrayList<>(), file, receivedChunks);

                        if (allChunksReceived(file)) {
                            InProcessFileTracer.remove(file.getFileHash());
                        }
                        
                        socket.close();

                    }else { // means you should continue the chain. 
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
    
    private static boolean allChunksReceived(NetworkFile file) {
        String fileHash = file.getFileHash();
    
        int totalChunks = (int) Math.ceil((double) file.getDataSize() / CHUNK_SIZE);
    
        List<Integer> receivedChunks = InProcessFileTracer.get(fileHash);
    
        if (receivedChunks == null) {
            return false;
        }
    
        return receivedChunks.size() == totalChunks;
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


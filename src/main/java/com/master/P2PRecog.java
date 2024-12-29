package com.master;

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
import java.util.HashMap;
import java.util.List;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;

import com.models.DirectoryFile;
import com.models.NetworkFile;
import com.models.Peer;
import com.notification.DirectoryNotification;
import com.notification.FileRequestNotification;


public class P2PRecog {
    private final int PORT = 9876;

    private FileManager fileManager;

    private Peer peerManager = new Peer(fileManager);

    private ConcurrentHashMap<String, ArrayList<Integer>> InProcessFileTracer = new ConcurrentHashMap<>();

    private static P2PRecog instance;

    private boolean connected = false;

    public static ConcurrentHashMap<String, FileProgress> fileProgressMap = new ConcurrentHashMap<>();


    // public static void main(String[] args) throws UnknownHostException, InterruptedException {

    //     Connect(); // Connect


    //     String hostIP = InetAddress.getLocalHost().getHostAddress();

    //     System.out.println("Host IP: " + hostIP);

    //     //Test
    //     Thread.sleep(1000);
    //     if (hostIP.equals("10.23.2.30")) {
    //         RequestForFile("newFile", "297ca1f736ab105f212a94489b3c7b816ea1c8ff432f25452cb52b5034c05b33");
    //     }
    //     //Test end

    //     while(true) { 
    //         Thread.sleep(1000);
    //     }
    // }

    private P2PRecog() throws UnknownHostException, InterruptedException {
        
    }

    public static P2PRecog getInstance() throws UnknownHostException, InterruptedException {
        if (instance == null) {
            synchronized (P2PRecog.class) {
                if (instance == null) {
                    instance = new P2PRecog(); // Instantiate if it doesn't exist
                }
            }
        }
        return instance;
    }


    public ConcurrentHashMap<String, FileProgress> getFileProgress() {
        return fileProgressMap;
    }

    public HashMap<String, String> GetReachableFiles() throws InterruptedException { 
        return peerManager.getFiles();
    }

    public void SetDestinationFolder(String folderPath) { 
        fileManager.setDestinationFilePath(folderPath);
    }

    public void SetSharedFolder(String folderPath) { 
        fileManager.setFilePath(folderPath);
    }

    public void Connect() throws UnknownHostException, InterruptedException {
        if (connected) {
            return;
        }

        connected = true;

        String hostDevice = InetAddress.getLocalHost().getHostAddress().split("\\.")[3];
        fileManager = new FileManager(hostDevice);

        System.out.println("Connected Device IP: " + hostDevice);

        Thread listenBroadcast = new Thread(() -> listenBroadcast());
        listenBroadcast.start();

        Thread sendBroadcastThread = new Thread(() -> DirectoryNotificationBroadcast());
        sendBroadcastThread.start();

        Thread tcpListenerThread = new Thread(()-> tcpConnectionRecevier());
        tcpListenerThread.start();

        while (true) {
            if (!connected) {
                listenBroadcast.interrupt();
                sendBroadcastThread.interrupt();
                tcpListenerThread.interrupt();
                break;
            }

            Thread.sleep(1000);
        }
    }

    public void Disconnect() { 
        connected = false;
    }

    public void DirectoryNotificationBroadcast() {
        while (connected) {
            DirectoryNotification notificationSend = new DirectoryNotification(fileManager.getFiles());
            sendBroadcast(notificationSend);
            try {
                Thread.sleep(2500);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            notificationSend = new DirectoryNotification(fileManager.getFiles());
            sendBroadcast(notificationSend);
        }

        System.out.println("closing directory notification broadcast");
    }

    public void RequestForFile(String fileName, String fileHash, ArrayList<Integer> receivedChunks) {
        if (receivedChunks == null) {
            receivedChunks = new ArrayList<>();
        }

        final ArrayList<Integer> finalReceivedChunks = receivedChunks;

        ArrayList<DirectoryFile> requestedFileList = peerManager.getRequestedFile(fileName, fileHash);
        requestedFileList.forEach((file)-> { 
            InProcessFileTracer.putIfAbsent(file.getFileHash(), new ArrayList<>());
            NetworkFile networkFile = new NetworkFile(new ArrayList<>(), null, file, -1);
            FileRequestNotification fileRequestNotificationSent = new FileRequestNotification(3, new ArrayList<>(), networkFile, finalReceivedChunks);
            sendBroadcast(fileRequestNotificationSent);
        });
    }

    public void RequestForFile(String fileName, String fileHash) {
        ArrayList<DirectoryFile> requestedFileList = peerManager.getRequestedFile(fileName, fileHash);
        requestedFileList.forEach((file)-> { 
            InProcessFileTracer.putIfAbsent(file.getFileHash(), new ArrayList<>());
            NetworkFile networkFile = new NetworkFile(new ArrayList<>(), null, file, -1);
            FileRequestNotification fileRequestNotificationSent = new FileRequestNotification(3, new ArrayList<>(), networkFile, new ArrayList<>());
            sendBroadcast(fileRequestNotificationSent);
        });

        System.out.println("request for file : " + fileName + " hash : " + fileHash);
    }

    public void listenBroadcast() {
        DatagramSocket socket;
        try {
            socket = new DatagramSocket(PORT);
        } catch (IOException e) {
            System.err.println("Error initializing sharedSocket: " + e.getMessage());
            e.printStackTrace();
            return; // Exit the method if socket initialization fails
        }
        try {
            while (connected) {
                byte[] buffer = new byte[1024];
                DatagramPacket packet = new DatagramPacket(buffer, buffer.length);

                socket.receive(packet);
                String message = new String(packet.getData(), 0, packet.getLength());
                String senderAddress = packet.getAddress().getHostAddress();

                String hostDevice = InetAddress.getLocalHost().getHostAddress().split("\\.")[3];

                if (!senderAddress.split("\\.")[3].equals(hostDevice)) {
                    // Process notifications
                    DirectoryNotificationHandling(message, senderAddress);
                    FileRequestNotificationHandling(message, senderAddress);
                }
            }
            System.out.println("closing listen broadcast");
        } catch (Exception e) {
            e.printStackTrace();
        }

        System.out.println("closing listen broadcast");
        socket.close();
    }

    public void FileRequestNotificationHandling(String message, String senderAddress) {
        FileRequestNotification fileRequestNotificationReceive;

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

            // System.out.println("Sending, file found in device : " + hostIP.split("\\.")[3]);

            List<Integer> unreceivedChunks = fileRequestNotificationReceive.getUnreceivedChunks();
            if (unreceivedChunks == null || unreceivedChunks.isEmpty()) {
                System.out.println("No chunks to process");
                return; // No chunks to process
            }

            int randomIndex = new Random().nextInt(unreceivedChunks.size());
            int chunkIndex = unreceivedChunks.get(randomIndex);

            System.out.println("unreceived chunk number : " + unreceivedChunks.size());

            byte[] data = fileManager.getChunk(reqFile.getFileName(), reqFile.getFileHash(), chunkIndex);

            NetworkFile networkFile = new NetworkFile(fileRequestNotificationReceive.getIpAncestors(), data, reqFile, chunkIndex);
            
            String nextIp = networkFile.popIp();

            if (nextIp !="") { 
                tcpConnectionSend(nextIp, networkFile);
            }

        };

        sendBroadcast(fileRequestNotificationReceive);      
    }

    public void tcpConnectionSend(String ip, NetworkFile file) {
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

    public class FileProgress { 
        public String fileName;
        public String fileHash;
        public int totalChunks;
        public int receivedChunks;

        public FileProgress(String fileName, String fileHash, int totalChunks, int receivedChunks) {
            this.fileName = fileName;
            this.fileHash = fileHash;
            this.totalChunks = totalChunks;
            this.receivedChunks = receivedChunks;
        }

        public String calculatePercentage() {
            if (totalChunks == 0) {
                return "0%"; // Avoid division by zero
            }
            double percentage = ((double) receivedChunks / totalChunks) * 100;
            return String.format("%.2f", percentage) + "%";
        }
    
    }

    // file sending chain listener, and protect the chain, if it is last stop take the file into local, MUST BE OPEN ALWAYS.....
    public void tcpConnectionRecevier(){ 
        try (ServerSocket serverSocket = new ServerSocket(9876)) { // Listening on port 9876

            while (connected) {
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

                        // System.out.println("Chunk received for file: " + file.getFileName() + " chunkNo: " + chunkNo);

                        if (chunkNo == -1) {
                            InProcessFileTracer.remove(file.getFileHash());
                            System.err.println("Error saving chunk to file");
                            continue;
                        }

                        InProcessFileTracer.get(file.getFileHash()).add(chunkNo);

                        int expectedChunkNum = (int) Math.ceil(Double.valueOf(file.getFileSize()) / FileManager.CHUNK_SIZE);

                        // System.out.println("Expected chunk number: " + expectedChunkNum +  " file size : " + file.getFileSize() + " chunk size : " + FileManager.CHUNK_SIZE);
                        boolean isAllChunksReceived = InProcessFileTracer.get(file.getFileHash()).size() == expectedChunkNum;

                        fileProgressMap.putIfAbsent(file.getFileHash(),
                        new FileProgress(file.getFileName(), file.getFileHash(), expectedChunkNum, InProcessFileTracer.get(file.getFileHash()).size()));

                        fileProgressMap.get(file.getFileHash()).receivedChunks = InProcessFileTracer.get(file.getFileHash()).size();

                        ArrayList<Integer> receivedChunks = InProcessFileTracer.get(file.getFileHash());
                        if (isAllChunksReceived) { // means all chunks are received.
                            System.out.println("All chunks received for file: " + file.getFileName());
                            InProcessFileTracer.remove(file.getFileHash());
                        }else { // means there are still chunks to be received.
                            RequestForFile(file.getFileName(), file.getFileHash(), receivedChunks);
                        }
                    }else if (nextIp != ""){ // means you should continue the chain.
                        tcpConnectionSend(nextIp, file);
                    }
                    
                } catch (IOException | ClassNotFoundException e) {
                    System.err.println("Error receiving file request: " + e.getMessage());
                }    
            }

            System.out.println("closing tcp connection receiver");
            serverSocket.close();
            
        } catch (IOException e) {
            System.err.println("Error starting server socket: " + e.getMessage());
        }
        
        System.out.println("going somewhere");
    }

    public void DirectoryNotificationHandling(String message, String senderAddress) {
        DirectoryNotification notificationReceive;
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

    public void sendBroadcast(DirectoryNotification notification) {
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
                socket.close();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        
        peerManager.addSource(notification);
    }

    public void sendBroadcast(FileRequestNotification notification) {
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
                socket.close();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}


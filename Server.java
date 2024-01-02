

import java.io.BufferedInputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.*;
import java.util.*;

public class Server {
    private DatagramSocket socket;
    private int totalProcesses;
    private int windowSize;
    private String filename;
    private double errorProbability;
    private List<InetSocketAddress> clients;
    private Map<InetSocketAddress, Integer> lastAckReceived;
    private Map<InetSocketAddress, Integer> clientWindows = new HashMap<>();
    private Map<InetSocketAddress, Timer> clientTimeouts = new HashMap<>();
    private byte[][] packets; // Added packets variable

    public Server(int port, int totalProcesses, int windowSize, String filename, double errorProbability) throws SocketException {
        this.socket = new DatagramSocket(port);
        this.totalProcesses = totalProcesses;
        this.windowSize = windowSize;
        this.filename = filename;
        this.errorProbability = errorProbability;
        this.clients = new ArrayList<>();
        this.lastAckReceived = new HashMap<>();

        // Initialize the packets array with data from the file
        try (BufferedInputStream fileInputStream = new BufferedInputStream(new FileInputStream(filename))) {
            List<byte[]> packetList = new ArrayList<>();
            while (true) {
                byte[] data = new byte[1024];
                int bytesRead = fileInputStream.read(data);
                if (bytesRead == -1) {
                    break; // End of file
                }
                byte[] packet = new byte[bytesRead];
                System.arraycopy(data, 0, packet, 0, bytesRead);
                packetList.add(packet);
            }
            this.packets = packetList.toArray(new byte[0][]);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void start() {
        System.out.println("Server started. Waiting for clients to join...");
        // Wait for all clients to join
        while (clients.size() < totalProcesses) {
            receiveJoinRequest();
        }

        System.out.println("All clients joined. Initiating file transfer...");

        // Start file transfer
        sendFile();
    }

    private void receiveJoinRequest() {
        try {
            byte[] receiveData = new byte[1024];
            DatagramPacket receivePacket = new DatagramPacket(receiveData, receiveData.length);
            socket.receive(receivePacket);

            InetSocketAddress clientAddress = new InetSocketAddress(receivePacket.getAddress(), receivePacket.getPort());

            if (!clients.contains(clientAddress)) {
                clients.add(clientAddress);
                System.out.println("Client " + clients.size() + " joined: " + clientAddress);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    
    
    
    private void sendFile() {
        // Implement file transfer logic here using Go-Back-N protocol
        while (true){
        for (InetSocketAddress clientAddress : clients) {
            final int[] windowStart = {0};
            clientWindows.put(clientAddress, windowStart[0]);
    
            Timer timeoutTimer = new Timer();
            timeoutTimer.schedule(new TimerTask() {
                @Override
                public void run() {
                    handleTimeout(clientAddress, windowStart[0]);
                }
            }, 500); // Set the timeout duration
    
            clientTimeouts.put(clientAddress, timeoutTimer);
    
            while (windowStart[0] < packets.length) { // Corrected loop condition
                int windowEnd = Math.min(windowStart[0] + windowSize, packets.length);
    
                for (int i = windowStart[0]; i < windowEnd; i++) {
                    sendWithSimulation(clientAddress, packets[i]);
                }
    
                windowStart[0] = waitForAcks(clientAddress, windowStart[0]);
            }
    
            // Reset window and timer for the next client
            windowStart[0] = 0;
            clientWindows.put(clientAddress, clientWindows.get(clientAddress)+1);
            timeoutTimer.cancel();
            timeoutTimer.purge();
        }
        }
    }
    
    private int waitForAcks(InetSocketAddress clientAddress, int windowStart) {
        while (windowStart < packets.length) { // Corrected loop condition
            int ackNum = lastAckReceived.getOrDefault(clientAddress, -1);
    
            if (ackNum != -1 && ackNum >= windowStart) {
                // Move the window
                windowStart = ackNum + 1;
            }
    
            // Check if all packets in the window have been acknowledged
            if (windowStart == packets.length) {
                break;
            }
        }
    
        return windowStart;
    }
    
    private void handleTimeout(InetSocketAddress clientAddress, int windowStart) {
        // Retransmit packets in the window
        for (int i = windowStart; i < Math.min(windowStart + windowSize, packets.length); i++) {
            sendWithSimulation(clientAddress, packets[i]);
        }
    }

    private void sendWithSimulation(InetSocketAddress clientAddress, byte[] data) {
        if (Math.random() > errorProbability) {
            try {
                DatagramPacket packet = new DatagramPacket(data, data.length, clientAddress.getAddress(),
                        clientAddress.getPort());
                socket.send(packet);
                System.out.println("Packet sent.");
            } catch (Exception e) {
                e.printStackTrace();
            }
        } else {
            System.out.println("Packet lost. Simulating error.");
        }
    }

    public static void main(String[] args) {
        try {
            int port = Integer.parseInt(args[0]);
            int totalProcesses = Integer.parseInt(args[1]);
            int windowSize = Integer.parseInt(args[2]);
            String filename = args[3];
            double errorProbability = Double.parseDouble(args[4]);

            Server server = new Server(port, totalProcesses, windowSize, filename, errorProbability);
            server.start();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}

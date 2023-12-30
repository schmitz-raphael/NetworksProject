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

    public Server(int port, int totalProcesses, int windowSize, String filename, double errorProbability) throws SocketException {
        this.socket = new DatagramSocket(port);
        this.totalProcesses = totalProcesses;
        this.windowSize = windowSize;
        this.filename = filename;
        this.errorProbability = errorProbability;
        this.clients = new ArrayList<>();
        this.lastAckReceived = new HashMap<>();
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

            // Send acknowledgment
            sendAck(clientAddress, 0);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void sendAck(InetSocketAddress clientAddress, int sequenceNumber) {
        try {
            DatagramPacket ackPacket = new DatagramPacket(String.valueOf(sequenceNumber).getBytes(),
                    String.valueOf(sequenceNumber).length(), clientAddress.getAddress(), clientAddress.getPort());
            socket.send(ackPacket);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void sendFile() {
        // Implement file transfer logic here using Go-Back-N protocol
        // You need to handle window management, timers, retransmissions, etc.
        // Use the UDP send wrapper function with error simulation
        // Update lastAckReceived map when acknowledgments are received
    }

    private void sendWithSimulation(InetSocketAddress clientAddress, byte[] data) {
        if (Math.random() > errorProbability) {
            try {
                DatagramPacket packet = new DatagramPacket(data, data.length, clientAddress.getAddress(),
                        clientAddress.getPort());
                socket.send(packet);
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

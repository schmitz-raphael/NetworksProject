

import java.io.BufferedOutputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.*;

public class Client {
    private DatagramSocket socket;
    private InetSocketAddress serverAddress;
    private String filename;
    private double errorProbability;
    private int id;

    public Client(InetSocketAddress serverAddress, String filename, double errorProbability) throws SocketException {
        this.socket = new DatagramSocket();
        this.serverAddress = serverAddress;
        this.filename = filename;
        this.errorProbability = errorProbability;
        this.id = id;
    }

    public void join() {
        try {
            // Send join request to the server
            byte[] joinData = "JOIN".getBytes();
            DatagramPacket joinPacket = new DatagramPacket(joinData, joinData.length, serverAddress.getAddress(),
                    serverAddress.getPort());
            socket.send(joinPacket);

            // Wait for acknowledgment from the server
            byte[] ackData = new byte[1024];
            DatagramPacket ackPacket = new DatagramPacket(ackData, ackData.length);
            socket.receive(ackPacket);

            System.out.println("Joined server: " + serverAddress);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void receiveFile() {
        try (BufferedOutputStream fileOutputStream = new BufferedOutputStream(new FileOutputStream("incoming_"+filename))) {
            while (true) {
                DatagramPacket receivePacket = receivePacket();
                if (receivePacket.getLength() == 0) {
                    break; // End of file transfer
                }

    
                fileOutputStream.write(receivePacket.getData(), 0, receivePacket.getLength());
                System.out.println("Client " + id + ": Packet received.");
                // Send acknowledgment
                sendAck(receivePacket.getAddress(), receivePacket.getPort());
            }

            System.out.println("File transfer complete.");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private DatagramPacket receivePacket() {
        try {
            byte[] receiveData = new byte[1024];
            DatagramPacket receivePacket = new DatagramPacket(receiveData, receiveData.length);
            socket.receive(receivePacket);
            System.out.println(new String(receivePacket.getData(),0, receivePacket.getData().length));
            return receivePacket;
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    private void sendAck(InetAddress address, int port) {
        try {
            byte[] ackData = "ACK".getBytes();
            DatagramPacket ackPacket = new DatagramPacket(ackData, ackData.length, address, port);
            socket.send(ackPacket);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {
        try {
            if (args.length < 5) {
                System.out.println("Insufficient command-line arguments.");
                System.exit(1); // Exit the program with an error code
            }

            String id = args[0];
            int numberOfProcesses = Integer.parseInt(args[1]);
            String filename = args[2];
            double errorProbability = Double.parseDouble(args[3]);
            String protocol = args[4];
            int windowSize = Integer.parseInt(args[5]);

            InetSocketAddress serverAddress = new InetSocketAddress("localhost", 5000);

            Client client = new Client(serverAddress, filename, errorProbability);
            client.join();
            client.receiveFile();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}



import java.io.BufferedInputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.*;
import java.util.*;

public class Server {
    private DatagramSocket socket;
    private int port;
    private int ackCounter = 0;
    private ArrayList<String> fileNames = new ArrayList<>();
    private ArrayList<Integer> clients = new ArrayList<>();


    public Server() throws SocketException {
        this.port = 6666;
        this.socket = new DatagramSocket(port);
    }

    public void start() {
        try {
            System.out.println("Server started on port " + port);

            // Wait for the client to join
            waitForJoin();

            // Send the file to the client
            sendFile();

            System.out.println("File transfer complete.");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void waitForJoin() throws IOException {
        while (clients.size() < 2) {
            DatagramPacket joinPacket = receivePacket();
            String joinMessage = new String(joinPacket.getData(), 0, joinPacket.getLength());
            if (joinPacket.getLength() > 0 && joinMessage.split(" ")[0].equals("JOIN")) {
                System.out.println("Client joined: " + joinPacket.getAddress() + ":" + joinPacket.getPort());
                fileNames.add(joinMessage.split(" ")[1]);
                clients.add(joinPacket.getPort());
                sendAck(joinPacket.getAddress(), joinPacket.getPort());
                
            }
        }
    }
    
    public void sendFile() throws IOException {
        for (int i = 0; i < clients.size(); i++){
            ackCounter = 0;
            try (BufferedInputStream fileInputStream = new BufferedInputStream(new FileInputStream(fileNames.get(i)))) {
                byte[] buffer = new byte[2048];
                int bytesRead;

                while ((bytesRead = fileInputStream.read(buffer)) != -1) {
                    sendPacket(buffer, bytesRead, clients.get(i));
                    waitForAck();
                }

                // Signal end of file transfer
                byte[] endOfFileMessage = "END_OF_FILE".getBytes();
                DatagramPacket endOfFilePacket = new DatagramPacket(endOfFileMessage, endOfFileMessage.length, InetAddress.getLocalHost(), clients.get(i));
                socket.send(endOfFilePacket);
            }
        }
    }
    public void sendAck(InetAddress address, int clientPort) throws IOException {
        sendPacket("ACK".getBytes(), address, clientPort);
    }
    private void sendPacket(byte[] data, InetAddress address, int port) throws IOException {
        DatagramPacket packet = new DatagramPacket(data, data.length, address, port);
        socket.send(packet);
    }
    public void sendPacket(byte[] data, int length, int clientPort) throws IOException {
        if (Math.random() > 0.00){
            DatagramPacket packet = new DatagramPacket(data, length, InetAddress.getLocalHost(), clientPort);
            socket.send(packet);
            System.out.println(": Packet sent");
        }
        else{
            System.out.println("Packet lost");
        }
    }

    private DatagramPacket receivePacket() throws IOException {
        byte[] receiveData = new byte[2056];
        DatagramPacket receivePacket = new DatagramPacket(receiveData, receiveData.length);
        socket.receive(receivePacket);
        return receivePacket;
    }

    private void waitForAck() throws IOException {
        while (true){
            DatagramPacket ackPacket = receivePacket();
            String ackMessage = new String(ackPacket.getData(), 0, ackPacket.getLength());
            if (ackMessage.split(" ")[0].equals("ACK") && Integer.parseInt(ackMessage.split(" ")[1]) == ackCounter) {
                System.out.println("Acknowledgment " + ackCounter++ + " received" );
                break;
            }
        }
    }

    public static void main(String[] args) {
        try{
            Server server = new Server();
            server.start();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}



import java.io.BufferedInputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.*;
import java.nio.ByteBuffer;
import java.util.*;
public class Server {
    private DatagramSocket socket;
    private int port;
    private int ackCounter = 0;
    private String fileName = "";
    private ArrayList<Integer> clients = new ArrayList<>();

    private DatagramPacket lastPacketSent;

    private static int packetSize = 8192;

    public Server() throws SocketException {
        this.port = 6666;
        this.socket = new DatagramSocket(port);
    }

    public void start() {
        try {
            System.out.println("Server: Server started on port " + port);

            // Wait for the client to join
            waitForJoin();

            // Send the file to the client
            sendFile();

            System.out.println("Server: File transfer complete.");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void waitForJoin() throws IOException {
        while (clients.size() < 10) {
            DatagramPacket joinPacket = receivePacket();
            String joinMessage = new String(joinPacket.getData(), 0, joinPacket.getLength());
            if (joinPacket.getLength() > 0 && joinMessage.split(" ")[0].equals("JOIN")) {
                System.out.println("Server: Client joined: " + joinPacket.getAddress() + ":" + joinPacket.getPort());
                fileName = joinMessage.split(" ")[1];
                clients.add(joinPacket.getPort());
                sendAck(joinPacket.getAddress(), joinPacket.getPort());    
            }
        }
    }
    
    public void sendFile() throws IOException {
        ackCounter = 0;
        try (BufferedInputStream fileInputStream = new BufferedInputStream(new FileInputStream(fileName))) {
            byte[] buffer = new byte[packetSize];
            int bytesRead;

            while ((bytesRead = fileInputStream.read(buffer, 4, packetSize-4)) != -1) {
                System.out.println("ACK:" + ackCounter);
                ByteBuffer.wrap(buffer).putInt(0, ackCounter);
                for (int i = 0; i < clients.size(); i++){
                    sendPacket(buffer, bytesRead+4, clients.get(i));
                    waitForAck();
                }
                ackCounter++;
            }

            // Signal end of file transfer
            for (int i = 0; i < clients.size(); i++){
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
        if (Math.random() > 0.05){
            DatagramPacket packet = new DatagramPacket(data, length, InetAddress.getLocalHost(), clientPort);
            lastPacketSent = packet;
            socket.send(packet);
            System.out.println("Server: Packet " + ackCounter + " sent");
        }
        else{
            System.out.println("Server: Packet lost");
        }
    }

    private DatagramPacket receivePacket() throws IOException {
        byte[] receiveData = new byte[packetSize];
        DatagramPacket receivePacket = new DatagramPacket(receiveData, receiveData.length);
        socket.receive(receivePacket);
        return receivePacket;
    }

    private void waitForAck() throws IOException {
        socket.setSoTimeout(5);
        while (true){
            try{
                byte[] ackData = new byte[packetSize];
                DatagramPacket ackPacket = new DatagramPacket(ackData, ackData.length);
                socket.receive(ackPacket);
                String ackMessage = new String(ackPacket.getData(), 0, ackPacket.getLength());
                if (ackMessage.split(" ")[0].equals("ACK") && Integer.parseInt(ackMessage.split(" ")[1]) == ackCounter) {
                System.out.println("Server: Acknowledgment " + ackCounter + " received" );
                break;
                }
            }
            catch (SocketTimeoutException e){
                timeout();
                break;
            }
        }
    }
    private void timeout(){
        for (int client: clients){
            try {
                sendPacket(lastPacketSent.getData(), lastPacketSent.getLength(), client);
                waitForAck();
            } catch (IOException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
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

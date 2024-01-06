import java.io.*;
import java.net.*;
import java.nio.ByteBuffer;
import java.util.*;

public class Server {
    private DatagramSocket socket;
    private int port;
    private String fileName = "";
    private Map<Integer, InetAddress> clients = new HashMap<>();

    private int base;
    private int nextSeq;
    private int windowSize;

    private static int packetSize = 65507;

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
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /*
     * this method is used to fill up the client list
     */
    private void waitForJoin() throws IOException {
        //check the size of the client list
        while (clients.size() < 2) {
            System.out.println(clients.size());
            DatagramPacket joinPacket = receivePacket();
            String joinMessage = new String(joinPacket.getData(), 0, joinPacket.getLength());
            //check if the first arg equals join
            if (joinMessage.split(" ")[0].equals("JOIN")) {
                System.out.println("Server: Client joined: " + joinPacket.getAddress() + ":" + joinPacket.getPort());
                //extract fileName
                fileName = joinMessage.split(" ")[1];
                //add the port to the client list
                clients.put(joinPacket.getPort(),joinPacket.getAddress());
                //send ACK
                sendAck(joinPacket.getAddress(), joinPacket.getPort());
            }
        }
    }

    public void sendFile() throws IOException {
        System.out.println("Server: Starting file upload");
        double startTime = System.currentTimeMillis();
        base = 0;
        nextSeq = 0;
        windowSize = 4;
        
        byte[] nextPacket = createPacket(nextSeq);
        //as long as the nextPacket is not empty ie. while there's a package to send
        while (nextPacket != null){
            //while nextSeq is smaller than the base + windowsize
            while (nextSeq < base + windowSize){
                //loop through the client and send the packet
                for (HashMap.Entry<Integer,InetAddress> client: clients.entrySet()) {
                    System.out.println("SERVER --> " + client.getKey().toString() + " sent packet:" + nextSeq);
                    sendPacket(nextPacket, nextPacket.length, client.getValue(), client.getKey());
                    
                }
                //get the next packet
                nextPacket = createPacket(++nextSeq);
                if (nextPacket == null){
                    break;
                }
            }
            //get the acknowledgements from every client
            waitForAck();
        }
        // Signal end of file transfer by sending a package to all clients
        for (HashMap.Entry<Integer,InetAddress> client: clients.entrySet()) {
            byte[] endOfFileMessage = "END_OF_FILE".getBytes();
            DatagramPacket endOfFilePacket = new DatagramPacket(endOfFileMessage, endOfFileMessage.length, client.getValue(), client.getKey());
            socket.send(endOfFilePacket);
        }
        //print final statement + statistics
        System.out.println("Server: File transfer complete.");
        System.out.println("Elapsed time: " + (System.currentTimeMillis() - startTime) / 1000 + "s");
    }
    
    /*
     * This method is used to get the package of certain number n
     */
    public byte[] createPacket(int n) throws IOException {
        try (RandomAccessFile randomAccessFile = new RandomAccessFile(fileName, "r")) {
            byte[] buffer = new byte[packetSize - 4]; // Deduct 4 bytes for the base index in the packet
    
            long position = (long) n * (packetSize - 4);
            randomAccessFile.seek(position);
    
            int bytesRead = randomAccessFile.read(buffer);
    
            if (bytesRead == -1) {
                return null;
            } else {
                byte[] packet = new byte[bytesRead + 4];
                ByteBuffer.wrap(packet).putInt(0, n);
                System.arraycopy(buffer, 0, packet, 4, bytesRead);
                return packet;
            }
        }
    }
    
    /*
     * This method is used to send Acks
     */
    public void sendAck(InetAddress address, int clientPort) throws IOException {
        DatagramPacket packet = new DatagramPacket("ACK".getBytes(), "ACK".getBytes().length, address, clientPort);
        socket.send(packet);
    }
    /*
     * This method is used to send packets over the socket
     */
    public void sendPacket(byte[] data, int length, InetAddress clientAddress, int clientPort) throws IOException {
        if (Math.random() > 0.1) {
            DatagramPacket packet = new DatagramPacket(data, length, clientAddress, clientPort);
            socket.send(packet);
        }
    }

    private DatagramPacket receivePacket() throws IOException {
        byte[] receiveData = new byte[packetSize];
        DatagramPacket receivePacket = new DatagramPacket(receiveData, receiveData.length);
        socket.receive(receivePacket);
        return receivePacket;
    }

    /*
     * this method is waiting for acks
     */
    private void waitForAck() throws IOException {
        HashMap<Integer,InetAddress> unacknowledgedClients = new HashMap<>(clients);
        socket.setSoTimeout(50); // Increased timeout for better reliability
        while (!unacknowledgedClients.isEmpty()) {
            try {
                byte[] ackData = new byte[packetSize];
                DatagramPacket ackPacket = new DatagramPacket(ackData, ackData.length);
                socket.receive(ackPacket);
                String ackMessage = new String(ackPacket.getData(), 0, ackPacket.getLength());
                
                if (ackMessage.startsWith("ACK")) {
                    int ackSeq = Integer.parseInt(ackMessage.substring(4)); // Extract the sequence number
                    if (base <= ackSeq && ackSeq < base + windowSize) {
                        unacknowledgedClients.remove(ackPacket.getPort());
                    }
                }
                //in case of a socket timeout
            } catch (SocketTimeoutException e) {
                // send the base package to all clients that are still unacknowledged
                for (HashMap.Entry<Integer,InetAddress> client: unacknowledgedClients.entrySet()) {
                        //for (int i = base; i < nextSeq; i++){
                            byte[] packet = createPacket(base);
                            sendPacket(packet, packet.length, client.getValue(), client.getKey());
                            System.out.println("SERVER --> " + client.getValue() + ":" + client.getKey() + "RESENT packet:" + base);
                        //}
                        
                }
            }
        }
        //once all clients received the base package, increment
        base += 1;
    }
    

    
    public static void main(String[] args) {
        try {
            Server server = new Server();
            server.start();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}

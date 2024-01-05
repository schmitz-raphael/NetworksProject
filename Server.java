import java.io.BufferedInputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.*;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class Server {
    private DatagramSocket socket;
    private int port;
    private String fileName = "";
    private ArrayList<Integer> clients = new ArrayList<>();

    private int base;
    private int nextSeq;
    private int window;

    private static int packetSize = 32768;

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
        System.out.println("Server: Starting file upload");
        double startTime = System.currentTimeMillis();
        base = 0;
        nextSeq = 0;
        int windowSize = 10; // Choose an appropriate window size
        
        byte[] nextPacket = createPacket(nextSeq);
        //as long as the nextPacket is not empty ie. while there's a package to send
        while (nextPacket != null){
            //while nextSeq is smaller than the base + windowsize
            while (nextSeq < base + windowSize){
                //loop through the client and send the packet
                for (int clientPort : clients) {
                    sendPacket(nextPacket, nextPacket.length, clientPort);
                    try{
                        Thread.sleep(1);
                    }
                    catch (InterruptedException e){
                        Thread.currentThread().interrupt();
                    }
                }
                System.out.println("PACKET_" + nextSeq + " sent");
                //get the next packet
                nextPacket = createPacket(++nextSeq);
                if (nextPacket == null){
                    break;
                }
            }
            //get the acknowledgements from every client
            waitForAck();
            //iterate base after receiving all acks
        }
        // Signal end of file transfer
        for (int client : clients) {
            byte[] endOfFileMessage = "END_OF_FILE".getBytes();
            DatagramPacket endOfFilePacket = new DatagramPacket(endOfFileMessage, endOfFileMessage.length, InetAddress.getLocalHost(), client);
            socket.send(endOfFilePacket);
        }
        System.out.println("Server: File transfer complete.");
        System.out.println("Elapsed time: " + (System.currentTimeMillis() - startTime) / 1000 + "s");
    }
    
    /*
     * This method is used to get the package of certain number n
     */
    public byte[] createPacket(int n) throws IOException{
        try (BufferedInputStream fileInputStream = new BufferedInputStream(new FileInputStream(fileName))) {
            System.out.println(n);
            byte[] buffer = new byte[packetSize - 4]; // Deduct 4 bytes for the base index in the packet
            int bytesRead = 0;

    
            for (int i = 0; i < n; i++){
                bytesRead= fileInputStream.read(buffer);
            }

            bytesRead= fileInputStream.read(buffer);
            if (bytesRead == -1) return null;
            else{
                byte[] packet = new byte[bytesRead+4];
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
    public void sendPacket(byte[] data, int length, int clientPort) throws IOException {
        if (Math.random() > 0.00) {
            DatagramPacket packet = new DatagramPacket(data, length, InetAddress.getLocalHost(), clientPort);
            socket.send(packet);
        }
        else{
            System.out.println("PACKET-LOST");
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
        Set<Integer> unacknowledgedClients = new HashSet<>(clients);
        socket.setSoTimeout(1000); // Increased timeout for better reliability
        while (true){
            try {
                byte[] ackData = new byte[packetSize];
                DatagramPacket ackPacket = new DatagramPacket(ackData, ackData.length);
                socket.receive(ackPacket);
                String ackMessage = new String(ackPacket.getData(), 0, ackPacket.getLength());
                System.out.println("Server: " + ackMessage + " vs " + base);
                if (ackMessage.startsWith("ACK")) {
                    int ackSeq = Integer.parseInt(ackMessage.substring(4)); // Extract the sequence number
                    if (base == ackSeq) {
                        unacknowledgedClients.remove(ackPacket.getPort());

                    }
                    if (unacknowledgedClients.isEmpty()){
                        System.out.println("SERVER RECEIVED ACK:" + ackSeq);
                        base++;
                        break;
                    }
                }
            } catch (SocketTimeoutException e) {
                for (int client: unacknowledgedClients){
                    for (int i = base; i < base; i++){
                        byte[] packet = createPacket(i);
                        sendPacket(packet, packet.length, client);
                        System.out.println("Resent packet " + i + " to client " + client);
                    }
                }
            }
        }
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

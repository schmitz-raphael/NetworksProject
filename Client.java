import java.io.BufferedOutputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.*;
import java.nio.ByteBuffer;
public class Client extends Thread{
    private DatagramSocket socket;
    private InetSocketAddress serverAddress;
    private String filename;
    private int rcvBase = 0;
    private int id;

    private static int packetSize = 65507;
    private double probability;

    private byte[] lastPacketSent;

    public Client(InetSocketAddress serverAddress, String filename, int id, double probability) throws SocketException {
        this.socket = new DatagramSocket();
        this.serverAddress = serverAddress;
        this.filename = filename;
        this.id = id;
        this.probability = probability;
        
    }

    public void requestJoin() {
        try {
            // Send join request to the server
            byte[] data = ("JOIN " + filename).getBytes();
            DatagramPacket packet = new DatagramPacket(data, data.length, serverAddress.getAddress(), serverAddress.getPort());
            socket.send(packet);
            waitForAck();
            System.out.println(id+":Joined server: " + serverAddress);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    public void waitForAck(){
        while (true){
            DatagramPacket ackPacket;
            try {
                ackPacket = receivePacket();
                if (ackPacket.getLength() > 0 && new String(ackPacket.getData()).trim().equals("ACK")) {
                break;
                }
            } catch (IOException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }
    }
    public void receiveFile() {
        rcvBase = 0;
        lastPacketSent = "".getBytes();
        System.out.println("Server: Starting file download");
        double startTime = System.currentTimeMillis();
        try (BufferedOutputStream fileOutputStream = new BufferedOutputStream(new FileOutputStream("TestFiles/" + id + "_" + filename.split("/")[1]))) {
            while (true) {
                DatagramPacket receivePacket = receivePacket();
                
                if (receivePacket.getLength() == 0 || isEndOfFilePacket(receivePacket)) {
                    break; // End of file transfer
                }
                byte[] data = receivePacket.getData();
                int packetNumber = ByteBuffer.wrap(data).getInt(0);
                //if the packet-number aligns with the rcvBase then write the packet data into the file, send the ack and increment rcvBase
                if (packetNumber == rcvBase){
                    System.out.println("CLIENT_" + id + ": RECEIVED PACKET:" +packetNumber);
                    fileOutputStream.write(data, 4, receivePacket.getLength()-4);
                    sendAck(rcvBase++);
                }
                //if you receive a smaller packetNumber than your rcvBase ie. an ack packet has been lost --> resend all acks between the packet number and the rcvBase 
                else if (packetNumber < rcvBase){
                        sendAck(packetNumber);
                }
            }
            fileOutputStream.close();
            System.out.println("Client"+id+": File transfer complete.");
            System.out.println("Client"+id+"Elapsed time: "+ (System.currentTimeMillis()-startTime)/1000+ "s");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void sendPacket(byte[] data) throws IOException {
        lastPacketSent = data;
        if (Math.random() > probability){
            DatagramPacket packet = new DatagramPacket(data, data.length, serverAddress.getAddress(), serverAddress.getPort());
            socket.send(packet);
        }
    }

    private DatagramPacket receivePacket() throws IOException{
        byte[] receiveData = new byte[packetSize];
        DatagramPacket receivePacket = new DatagramPacket(receiveData, receiveData.length);
        socket.receive(receivePacket);
        return receivePacket;
    }

    private void sendAck(int n) throws IOException {
        String ackMessage = "ACK " + (n);
        System.out.println("CLIENT_" + id + ": sent: " + ackMessage);
        sendPacket(ackMessage.getBytes());
    }
    private boolean isEndOfFilePacket(DatagramPacket packet) {
        String packetData = new String(packet.getData(), 0, packet.getLength()).trim();
        return packetData.equals("END_OF_FILE");
    }
    @Override
    public void run(){
        requestJoin();
        receiveFile();
    }
    public static void main(String[] args) {
        try {
            String filename = "TestFiles/video.avi";
            int n = 2;
            double probability = 0.1;

            InetSocketAddress serverAddress = new InetSocketAddress("localhost", 6666);

            Client[] clients = new Client[n];

            for (int i = 0; i < n; i++){
                clients[i] = new Client(serverAddress, filename, i, probability);
                clients[i].start();
            }
            for (Client client : clients){
                client.join();
            }
            
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}


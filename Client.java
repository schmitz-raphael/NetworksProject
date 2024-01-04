import java.io.BufferedOutputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.*;
import java.nio.ByteBuffer;

public class Client extends Thread{
    private DatagramSocket socket;
    private InetSocketAddress serverAddress;
    private String filename;
    private int packetsReceived = 0;
    private int id;

    private static int packetSize = 8192;
    private static double probability = 0.00;

    public Client(InetSocketAddress serverAddress, String filename, int id) throws SocketException {
        this.socket = new DatagramSocket();
        this.serverAddress = serverAddress;
        this.filename = filename;
        this.id = id;
    }

    public void requestJoin() {
        try {
            // Send join request to the server
            sendPacket(("JOIN " + filename).getBytes());
            waitForAck();
            System.out.println(id+":Joined server: " + serverAddress);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    public void waitForAck(){
        while (true){
            DatagramPacket ackPacket = receivePacket();
            if (ackPacket.getLength() > 0 && new String(ackPacket.getData()).trim().equals("ACK")) {
                System.out.println(id+":Acknowledgment received.");
                break;
            }
        }
    }
    public void receiveFile() {
        try (BufferedOutputStream fileOutputStream = new BufferedOutputStream(new FileOutputStream(id +"_incoming_" + filename))) {
            while (true) {
                DatagramPacket receivePacket = receivePacket();

                if (receivePacket.getLength() == 0 || isEndOfFilePacket(receivePacket)) {
                    break; // End of file transfer
                }
                byte[] data = receivePacket.getData();
                System.out.println("data[0]=" +ByteBuffer.wrap(data).getInt(0));
                if (ByteBuffer.wrap(data).getInt(0)== packetsReceived){
                    fileOutputStream.write(data, 4, receivePacket.getLength()-4);
                    System.out.println(id+":Packet " + packetsReceived + " received.");
                    packetsReceived++;
                }

                // Send acknowledgment
                System.out.println(id+": Total packets: " + packetsReceived);
                sendAck();
            }
            fileOutputStream.close();
            System.out.println(id+":File transfer complete.");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void sendPacket(byte[] data) throws IOException {
        if (Math.random() > probability){
            DatagramPacket packet = new DatagramPacket(data, data.length, serverAddress.getAddress(), serverAddress.getPort());
            socket.send(packet);
        }
        else System.out.println(id+":Packet lost");
    }

    private DatagramPacket receivePacket() {
        try {
            byte[] receiveData = new byte[packetSize];
            DatagramPacket receivePacket = new DatagramPacket(receiveData, receiveData.length);
            socket.receive(receivePacket);
            return receivePacket;
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    private void sendAck() throws IOException {
        String ackMessage = "ACK " + (packetsReceived-1);
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

            String filename = args[0];
            int n = Integer.parseInt(args[1]);
            InetSocketAddress serverAddress = new InetSocketAddress("localhost", 6666);

            Client[] clients = new Client[n];

            for (int i = 0; i < n; i++){
                clients[i] = new Client(serverAddress, filename, i);
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


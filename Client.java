import java.io.BufferedOutputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.*;

public class Client {
    private DatagramSocket socket;
    private InetSocketAddress serverAddress;
    private String filename;
    private int packetsReceived = 0;
    private int id;

    public Client(InetSocketAddress serverAddress, String filename, int id) throws SocketException {
        this.socket = new DatagramSocket();
        this.serverAddress = serverAddress;
        this.filename = filename;
        this.id = id;
    }

    public void join() {
        try {
            // Send join request to the server
            sendPacket(("JOIN " + filename).getBytes());
            waitForAck();
            System.out.println("Joined server: " + serverAddress);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    public void waitForAck(){
        while (true){
            DatagramPacket ackPacket = receivePacket();
            if (ackPacket.getLength() > 0 && new String(ackPacket.getData()).trim().equals("ACK")) {
                System.out.println("Acknowledgment received.");
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

                fileOutputStream.write(receivePacket.getData(), 0, receivePacket.getLength());
                System.out.println("Packet " + packetsReceived + " received.");

                // Send acknowledgment
                sendAck();
            }
            fileOutputStream.close();
            System.out.println("File transfer complete.");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void sendPacket(byte[] data) throws IOException {
        if (Math.random() > 0.00){
            DatagramPacket packet = new DatagramPacket(data, data.length, serverAddress.getAddress(), serverAddress.getPort());
            socket.send(packet);
        }
        else System.out.println("Packet lost");
    }

    private DatagramPacket receivePacket() {
        try {
            byte[] receiveData = new byte[2056];
            DatagramPacket receivePacket = new DatagramPacket(receiveData, receiveData.length);
            socket.receive(receivePacket);
            return receivePacket;
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    private void sendAck() throws IOException {
        String ackMessage = "ACK " + (packetsReceived++);
        sendPacket(ackMessage.getBytes());
    }
    private boolean isEndOfFilePacket(DatagramPacket packet) {
        String packetData = new String(packet.getData(), 0, packet.getLength()).trim();
        return packetData.equals("END_OF_FILE");
    }
    public static void main(String[] args) {
        try {

            String filename = args[0];
            int id = Integer.parseInt(args[1]);
            InetSocketAddress serverAddress = new InetSocketAddress("localhost", 6666);

            Client client = new Client(serverAddress, filename, id);
            client.join();
            client.receiveFile();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}

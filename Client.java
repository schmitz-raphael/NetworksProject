import java.io.BufferedOutputStream;
import java.io.File;
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

    private int totalProcesses;
    private double probability;
    private int window;
    private int timeout;


    public Client(InetSocketAddress serverAddress, String filename, int id, int totalProcesses, double probability, int window, int timeout) throws SocketException {
        this.socket = new DatagramSocket();
        this.serverAddress = serverAddress;
        this.filename = filename;
        this.id = id;
        this.totalProcesses = totalProcesses;
        this.probability = probability;
        this.window = window;
        this.timeout = timeout;
        
    }

    public void requestJoin() {
        try {
            // Send join request to the server
            byte[] data = ("JOIN " + filename + " " + totalProcesses + " " + probability + " " + window + " " + timeout).getBytes();
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
        System.out.println("Server: Starting file download");
        double startTime = System.currentTimeMillis();
         
        //check if a directory for the user already exists and create a new one if that's the case
        File directory = new File(id+"/");
        if (!directory.exists()) {
            if (directory.mkdirs()) {
                System.out.println("Server: Directory " + id + " created.");
            } 
        }
        //create a bufferedoutput stream to read the packets into a new file
        try (BufferedOutputStream fileOutputStream = new BufferedOutputStream(new FileOutputStream(id + "/" + filename.split("/")[1]))) {
            while (true) {
                //receive packet
                DatagramPacket receivePacket = receivePacket();
                
                //check if the packet declares the end of file and break out of the loop if that's the case
                if (receivePacket.getLength() == 0 || isEndOfFilePacket(receivePacket)) {
                    break;
                }

                //retrieve the data and the packet number
                byte[] data = receivePacket.getData();
                int packetNumber = ByteBuffer.wrap(data).getInt(0);

                int checksum = ByteBuffer.wrap(data).getInt(4);

                if (checksum != checkSum(receivePacket)){
                    System.out.println(checksum);
                    System.err.println("Checksum failed! Stopping file download ");
                    break;
                }
                System.out.println("CLIENT_" + id + ": RECEIVED PACKET:" +packetNumber);

                //if the packet-number aligns with the rcvBase then write the packet data into the file, send the ack and increment rcvBase
                if (packetNumber == rcvBase){
                    //write the body of the data into the file
                    fileOutputStream.write(data, 8, receivePacket.getLength()-8);
                    //send an ACK and increase the base
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
    //wrapper function to send packets
    private void sendPacket(byte[] data) throws IOException {
        if (Math.random() > probability){
            DatagramPacket packet = new DatagramPacket(data, data.length, serverAddress.getAddress(), serverAddress.getPort());
            socket.send(packet);
        }
    }
    //wrapper function to receive packets
    private DatagramPacket receivePacket() throws IOException{
        byte[] receiveData = new byte[packetSize];
        DatagramPacket receivePacket = new DatagramPacket(receiveData, receiveData.length);
        socket.receive(receivePacket);
        return receivePacket;
    }
    //wrapper function to send acks
    private void sendAck(int n) throws IOException {
        String ackMessage = "ACK " + (n);
        System.out.println("CLIENT_" + id + ": sent: " + ackMessage);
        sendPacket(ackMessage.getBytes());
    }
    //function to check for EOF
    private boolean isEndOfFilePacket(DatagramPacket packet) {
        String packetData = new String(packet.getData(), 0, packet.getLength()).trim();
        return packetData.equals("END_OF_FILE");
    }
    //method to calculate checksum
    public int checkSum(DatagramPacket packet){
        return packet.getLength()-8;
    }
    //run method for the multi-threading
    @Override
    public void run(){
        requestJoin();
        receiveFile();
    }
    public static void main(String[] args) {
        try {
            String filename = args[0];
            int n = Integer.parseInt(args[1]);
            double probability = Double.parseDouble(args[2]);
            int windowSize = Integer.parseInt(args[3]);
            int timeout = Integer.parseInt(args[4]);

            InetSocketAddress serverAddress = new InetSocketAddress("localhost", 6666);

            Client[] clients = new Client[n];

            for (int i = 0; i < n; i++){
                clients[i] = new Client(serverAddress, filename, i, n,probability, windowSize, timeout);
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


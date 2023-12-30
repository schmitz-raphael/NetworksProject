import java.io.BufferedInputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.*;
import java.util.Timer;
import java.util.TimerTask;

public class Client {
    private DatagramSocket socket;
    private InetSocketAddress serverAddress;
    private String filename;
    private int windowSize;
    private double errorProbability;
    private Timer timer;
    private int base;
    private int nextSeqNum;
    private byte[][] packets;
    private boolean[] ackReceived;

    public Client(InetSocketAddress serverAddress, String filename, int windowSize, double errorProbability) throws SocketException {
        this.socket = new DatagramSocket();
        this.serverAddress = serverAddress;
        this.filename = filename;
        this.windowSize = windowSize;
        this.errorProbability = errorProbability;
        this.timer = new Timer();
        this.base = 0;
        this.nextSeqNum = 0;
        this.packets = new byte[windowSize][];
        this.ackReceived = new boolean[windowSize];

        // Initialize the packets array with data from the file
        try (BufferedInputStream fileInputStream = new BufferedInputStream(new FileInputStream(filename))) {
            for (int i = 0; i < windowSize; i++) {
                byte[] data = new byte[1024];
                int bytesRead = fileInputStream.read(data);
                if (bytesRead == -1) {
                    break; // End of file
                }
                packets[i] = new byte[bytesRead];
                System.arraycopy(data, 0, packets[i], 0, bytesRead);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
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

    public void start() {
        // Start a timer task to handle retransmissions
        timer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                handleTimeout();
            }
        }, 1000, 1000); // Adjust the delay and period as needed

        // Send the initial window of packets
        for (int i = 0; i < windowSize; i++) {
            sendPacket(i);
        }

        // Continue sending packets until all have been acknowledged
        while (base < packets.length) {
            // Wait for acknowledgments
            waitForAcks();

            // Send the next packet in the window
            sendPacket(nextSeqNum);
        }

        // Stop the timer task
        timer.cancel();
        timer.purge();

        System.out.println("File transfer complete.");
    }


    private void sendWithSimulation(byte[] data) {
        if (Math.random() > errorProbability) {
            try {
                DatagramPacket packet = new DatagramPacket(data, data.length, serverAddress.getAddress(),
                        serverAddress.getPort());
                socket.send(packet);
            } catch (Exception e) {
                e.printStackTrace();
            }
        } else {
            System.out.println("Packet lost. Simulating error.");
        }
    }
    private void handleTimeout() {
        for (int i = base; i < nextSeqNum; i++) {
            if (!ackReceived[i % windowSize]) {
                // Timeout occurred for this packet, retransmit
                sendPacket(i);
            }
        }
    }
    private void waitForAcks() {
        while (true) {
            if (ackReceived[base % windowSize]) {
                // Acknowledgment received for the base packet, move the window
                base++;
            }
            // Check if all packets in the window have been acknowledged
            if (base == nextSeqNum) {
                break;
            }
        }
    }
    private void sendPacket(int seqNum) {
        if (seqNum < packets.length) {
            sendWithSimulation(packets[seqNum]);
            nextSeqNum++;
        }
    }

    public static void main(String[] args) {
        try {
            String serverHost = args[0];
            int serverPort = Integer.parseInt(args[1]);
            String filename = args[2];
            int windowSize = Integer.parseInt(args[3]);
            double errorProbability = Double.parseDouble(args[4]);

            InetSocketAddress serverAddress = new InetSocketAddress(serverHost, serverPort);

            Client client = new Client(serverAddress, filename, windowSize, errorProbability);
            client.join();
            client.start();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    
}

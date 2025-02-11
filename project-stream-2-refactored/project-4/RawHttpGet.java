import com.savarese.rocksaw.net.RawSocket;
import java.io.ByteArrayOutputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.InetAddress;
import java.net.URL;
import java.util.Random;

public class RawHttpGet {
    public static void main(String[] args) throws Exception {
        if (args.length < 1) {
            System.err.println("Usage: rawhttpget <URL>");
            System.exit(1);
        }
        String urlString = args[0];
        URL url = new URL(urlString);
        String host = url.getHost();
        int port = (url.getPort() != -1) ? url.getPort() : 80;
        String path = url.getPath();
        if (path == null || path.isEmpty()) {
            path = "/";
        }
        String fileName = extractFileName(path);

        // Resolve the remote host
        InetAddress remoteIP = InetAddress.getByName(host);
        // For simplicity, assume localIP is chosen by some means (do not choose 127.0.0.1!)
        InetAddress localIP = InetAddress.getLocalHost();
        int localPort = chooseRandomPort();

        // Create raw sockets:
        // The sending socket must include our own IP header.
        RawSocket sendSocket = new RawSocket();
        sendSocket.open(RawSocket.PF_INET, RawSocket.getProtocolByName("raw"));
        sendSocket.setIPHeaderInclude(true);

        // The receiving socket listens for TCP packets
        RawSocket recvSocket = new RawSocket();
        recvSocket.open(RawSocket.PF_INET, RawSocket.getProtocolByName("tcp"));
        // Bind to our local IP so we only get packets destined for us.
        recvSocket.bind(localIP);

        // Initialize sequence numbers
        int seq = new Random().nextInt(100000);
        int ack = 0;

        // ----- 3-Way Handshake -----
        // 1. Send SYN
        TCPPacket synPacket = new TCPPacket(localPort, port, seq, 0, TCPPacket.FLAG_SYN);
        byte[] tcpSegment = synPacket.toByteArray(); // without checksum yet
        IPPacket ipSyn = new IPPacket(localIP, remoteIP, tcpSegment);
        ipSyn.computeChecksum();
        synPacket.computeChecksum(localIP, remoteIP);
        byte[] synPacketBytes = IPPacket.combine(ipSyn, synPacket);
        sendSocket.write(remoteIP, synPacketBytes);
        System.out.println("Sent SYN");

        // 2. Wait for SYN+ACK
        byte[] buffer = new byte[1500];
        while (true) {
            int len = recvSocket.read(buffer);
            if (len <= 0) continue;
            TCPPacket received = TCPPacket.parse(buffer);
            if (received == null) continue;
            // Check that the packet is from our remote host and that it has SYN+ACK set
            if (received.isSYN() && received.isACK() && received.getAckNumber() == seq + 1) {
                ack = received.getSequenceNumber() + 1;
                System.out.println("Received SYN+ACK");
                break;
            }
        }
        // 3. Send ACK to complete handshake
        TCPPacket ackPacket = new TCPPacket(localPort, port, seq + 1, ack, TCPPacket.FLAG_ACK);
        byte[] tcpAckSegment = ackPacket.toByteArray();
        IPPacket ipAck = new IPPacket(localIP, remoteIP, tcpAckSegment);
        ipAck.computeChecksum();
        ackPacket.computeChecksum(localIP, remoteIP);
        byte[] ackPacketBytes = IPPacket.combine(ipAck, ackPacket);
        sendSocket.write(remoteIP, ackPacketBytes);
        System.out.println("Sent ACK, handshake complete");

        // ----- Send HTTP GET Request -----
        String httpRequest = "GET " + path + " HTTP/1.0\r\nHost: " + host + "\r\n\r\n";
        byte[] httpData = httpRequest.getBytes("US-ASCII");
        TCPPacket getPacket = new TCPPacket(localPort, port, seq + 1, ack, TCPPacket.FLAG_ACK | TCPPacket.FLAG_PSH);
        getPacket.setPayload(httpData);
        byte[] tcpGetSegment = getPacket.toByteArray();
        IPPacket ipGet = new IPPacket(localIP, remoteIP, tcpGetSegment);
        ipGet.computeChecksum();
        getPacket.computeChecksum(localIP, remoteIP);
        byte[] getPacketBytes = IPPacket.combine(ipGet, getPacket);
        sendSocket.write(remoteIP, getPacketBytes);
        System.out.println("Sent HTTP GET request");

        // ----- Receive HTTP Response -----
        ByteArrayOutputStream responseStream = new ByteArrayOutputStream();
        boolean finished = false;
        long lastReceiveTime = System.currentTimeMillis();
        while (!finished) {
            int len = recvSocket.read(buffer);
            if (len > 0) {
                lastReceiveTime = System.currentTimeMillis();
                TCPPacket responsePacket = TCPPacket.parse(buffer);
                if (responsePacket == null) continue;
                byte[] payload = responsePacket.getPayload();
                if (payload != null && payload.length > 0) {
                    responseStream.write(payload);
                }
                // If FIN is seen, break and send ACK for FIN.
                if (responsePacket.isFIN()) {
                    TCPPacket finAck = new TCPPacket(localPort, port, seq + 1, responsePacket.getSequenceNumber() + 1, TCPPacket.FLAG_ACK);
                    byte[] tcpFinAck = finAck.toByteArray();
                    IPPacket ipFinAck = new IPPacket(localIP, remoteIP, tcpFinAck);
                    ipFinAck.computeChecksum();
                    finAck.computeChecksum(localIP, remoteIP);
                    sendSocket.write(remoteIP, IPPacket.combine(ipFinAck, finAck));
                    finished = true;
                }
            } else {
                // Check for timeout (3 minutes without data)
                if (System.currentTimeMillis() - lastReceiveTime > 180000) {
                    System.err.println("Connection timed out.");
                    finished = true;
                }
            }
        }
        byte[] responseBytes = responseStream.toByteArray();
        FileOutputStream fos = new FileOutputStream(fileName);
        fos.write(responseBytes);
        fos.close();
        System.out.println("Downloaded file: " + fileName);

        sendSocket.close();
        recvSocket.close();
    }

    private static String extractFileName(String path) {
        if (path.endsWith("/")) return "index.html";
        int lastSlash = path.lastIndexOf('/');
        if (lastSlash >= 0 && lastSlash < path.length() - 1)
            return path.substring(lastSlash + 1);
        return "index.html";
    }

    private static int chooseRandomPort() {
        return 1024 + new Random().nextInt(64512);
    }
}

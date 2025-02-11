import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.nio.ByteBuffer;

public class DNSServer {

    // The CDN-specific domain name to serve (e.g., cs5700cdn.example.com)
    private final String cdnDomain;
    // The port to listen on
    private final int port;

    public DNSServer(int port, String cdnDomain) {
        this.port = port;
        this.cdnDomain = cdnDomain.toLowerCase();
    }

    public void start() throws IOException {
        DatagramSocket socket = new DatagramSocket(port);
        System.out.println("DNS Server started on port " + port + " for domain " + cdnDomain);
        byte[] buffer = new byte[512]; // typical DNS packet size
        while (true) {
            DatagramPacket requestPacket = new DatagramPacket(buffer, buffer.length);
            socket.receive(requestPacket);
            byte[] requestData = new byte[requestPacket.getLength()];
            System.arraycopy(buffer, 0, requestData, 0, requestPacket.getLength());

            // Parse DNS query (very basic parser for one question)
            String queryName = parseQueryName(requestData);
            if (queryName != null && queryName.equals(cdnDomain)) {
                // For demonstration, use the client IP (from request packet) as the basis for replica mapping
                String clientIP = requestPacket.getAddress().getHostAddress();
                String replicaIP = ReplicaMapper.getBestReplica(clientIP);
                System.out.println("Mapping client " + clientIP + " to replica " + replicaIP);
                byte[] response = buildResponse(requestData, replicaIP);
                DatagramPacket responsePacket = new DatagramPacket(response, response.length,
                        requestPacket.getAddress(), requestPacket.getPort());
                socket.send(responsePacket);
            } else {
                // Optionally: send an error response or ignore
                System.out.println("Received query for unknown domain: " + queryName);
            }
        }
    }

    /**
     * Very simple parser that extracts the query name from the DNS request.
     * Note: This parser assumes a single question and no compression.
     */
    private String parseQueryName(byte[] data) {
        try {
            ByteBuffer buffer = ByteBuffer.wrap(data);
            // Skip the 12-byte header.
            buffer.position(12);
            StringBuilder sb = new StringBuilder();
            while (true) {
                int len = buffer.get() & 0xFF;
                if (len == 0) {
                    break;
                }
                byte[] label = new byte[len];
                buffer.get(label);
                if (sb.length() > 0) {
                    sb.append(".");
                }
                sb.append(new String(label, "UTF-8"));
            }
            return sb.toString().toLowerCase();
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * Builds a simple DNS response packet with a single A record answer.
     */
    private byte[] buildResponse(byte[] requestData, String ipStr) throws IOException {
        ByteBuffer requestBuffer = ByteBuffer.wrap(requestData);
        // The DNS header is the first 12 bytes.
        byte[] header = new byte[12];
        requestBuffer.get(header);
        // Create response header:
        // Set the response flag (QR = 1) and copy the ID.
        header[2] = (byte) 0x81; // 1000 0001 (flags: QR, Opcode=0, AA=0, TC=0, RD=1)
        header[3] = (byte) 0x80; // 1000 0000 (flags: RA=1, Z=0, RCODE=0)
        // Set QDCOUNT to the same value (we support one question)
        // Set ANCOUNT (number of answers) to 1.
        header[6] = 0;
        header[7] = 1;
        // NSCOUNT and ARCOUNT can be zero.
        header[8] = 0;
        header[9] = 0;
        header[10] = 0;
        header[11] = 0;

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        baos.write(header);
        // Copy the Question section (the rest of the query until we reach the QTYPE and QCLASS, which we preserve)
        // For simplicity, we assume the question section is everything after the header until a total length is reached.
        // (A robust implementation would properly parse and reassemble the question.)
        int questionLength = requestData.length - 12;
        baos.write(requestData, 12, questionLength);

        // Now append the Answer section.
        // Answer:
        // Name: a pointer to offset 12 (0xC00C)
        baos.write(0xC0);
        baos.write(0x0C);
        // Type: A (0x0001)
        baos.write(0x00);
        baos.write(0x01);
        // Class: IN (0x0001)
        baos.write(0x00);
        baos.write(0x01);
        // TTL: set to 60 seconds (0x0000003C)
        baos.write(0x00);
        baos.write(0x00);
        baos.write(0x00);
        baos.write(0x3C);
        // RDLENGTH: length of the RDATA for an A record is 4 bytes.
        baos.write(0x00);
        baos.write(0x04);
        // RDATA: the IPv4 address (convert the replicaIP string to 4 bytes)
        String[] parts = ipStr.split("\\.");
        for (String part : parts) {
            baos.write(Integer.parseInt(part));
        }
        return baos.toByteArray();
    }

    public static void main(String[] args) {
        // Expect command-line arguments: -p <port> -n <name>
        if (args.length < 4) {
            System.err.println("Usage: ./dnsserver -p <port> -n <name>");
            System.exit(1);
        }
        int port = 0;
        String name = null;
        for (int i = 0; i < args.length; i++) {
            if ("-p".equals(args[i]) && i + 1 < args.length) {
                port = Integer.parseInt(args[i + 1]);
                i++;
            } else if ("-n".equals(args[i]) && i + 1 < args.length) {
                name = args[i + 1];
                i++;
            }
        }
        if (port == 0 || name == null) {
            System.err.println("Invalid arguments.");
            System.exit(1);
        }
        DNSServer server = new DNSServer(port, name);
        try {
            server.start();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}

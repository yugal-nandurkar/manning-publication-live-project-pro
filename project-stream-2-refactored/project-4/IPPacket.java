import java.net.InetAddress;
import java.util.Random;

public class IPPacket {
    private byte[] header;   // fixed 20-byte header (no options)
    private InetAddress src;
    private InetAddress dst;
    private int totalLength;

    public IPPacket(InetAddress src, InetAddress dst, byte[] tcpSegment) {
        this.src = src;
        this.dst = dst;
        totalLength = 20 + tcpSegment.length;
        header = new byte[20];
        // Version (4) and IHL (5): 0x45
        header[0] = 0x45;
        header[1] = 0;  // TOS
        header[2] = (byte)((totalLength >> 8) & 0xFF);
        header[3] = (byte)(totalLength & 0xFF);
        // Identification (random)
        int id = new Random().nextInt(65535);
        header[4] = (byte)((id >> 8) & 0xFF);
        header[5] = (byte)(id & 0xFF);
        // Flags and Fragment Offset (set to 0)
        header[6] = 0;
        header[7] = 0;
        // TTL
        header[8] = 64;
        // Protocol: TCP (6)
        header[9] = 6;
        // Checksum (initially 0; will compute later)
        header[10] = 0;
        header[11] = 0;
        // Source IP address (4 bytes)
        byte[] srcBytes = src.getAddress();
        System.arraycopy(srcBytes, 0, header, 12, 4);
        // Destination IP address (4 bytes)
        byte[] dstBytes = dst.getAddress();
        System.arraycopy(dstBytes, 0, header, 16, 4);
    }

    public void computeChecksum() {
        // Zero the checksum bytes first.
        header[10] = 0;
        header[11] = 0;
        int checksum = PacketUtil.computeChecksum(header);
        header[10] = (byte)((checksum >> 8) & 0xFF);
        header[11] = (byte)(checksum & 0xFF);
    }

    public byte[] toByteArray() {
        return header;
    }

    // Utility to “combine” the IP header and the TCP segment.
    public static byte[] combine(IPPacket ip, TCPPacket tcp) {
        byte[] ipBytes = ip.toByteArray();
        byte[] tcpBytes = tcp.toByteArrayWithChecksum();
        byte[] packet = new byte[ipBytes.length + tcpBytes.length];
        System.arraycopy(ipBytes, 0, packet, 0, ipBytes.length);
        System.arraycopy(tcpBytes, 0, packet, ipBytes.length, tcpBytes.length);
        return packet;
    }
}

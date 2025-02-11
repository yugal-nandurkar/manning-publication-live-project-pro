import java.net.InetAddress;
import java.util.Arrays;
import java.util.Random;

public class TCPPacket {
    public static final int FLAG_FIN = 0x01;
    public static final int FLAG_SYN = 0x02;
    public static final int FLAG_RST = 0x04;
    public static final int FLAG_PSH = 0x08;
    public static final int FLAG_ACK = 0x10;
    public static final int FLAG_URG = 0x20;

    private int sourcePort;
    private int destPort;
    private int sequenceNumber;
    private int ackNumber;
    int dataOffset;  // header length in 32-bit words (usually 5)
    private int flags;
    private int windowSize;
    private int checksum;
    private int urgentPointer;
    private byte[] payload;

    public TCPPacket(int sourcePort, int destPort, int sequenceNumber, int ackNumber, int flags) {
        this.sourcePort = sourcePort;
        this.destPort = destPort;
        this.sequenceNumber = sequenceNumber;
        this.ackNumber = ackNumber;
        this.dataOffset = 5; // no options => header = 20 bytes
        this.flags = flags;
        this.windowSize = 65535;
        this.checksum = 0;
        this.urgentPointer = 0;
        this.payload = new byte[0];
    }

    public void setPayload(byte[] payload) {
        this.payload = payload;
    }

    // Constructs the TCP header (without checksum)
    public byte[] toByteArray() {
        int headerLength = dataOffset * 4;
        byte[] tcpHeader = new byte[headerLength + payload.length];
        // Source port (2 bytes)
        tcpHeader[0] = (byte)((sourcePort >> 8) & 0xFF);
        tcpHeader[1] = (byte)(sourcePort & 0xFF);
        // Destination port (2 bytes)
        tcpHeader[2] = (byte)((destPort >> 8) & 0xFF);
        tcpHeader[3] = (byte)(destPort & 0xFF);
        // Sequence number (4 bytes)
        tcpHeader[4] = (byte)((sequenceNumber >> 24) & 0xFF);
        tcpHeader[5] = (byte)((sequenceNumber >> 16) & 0xFF);
        tcpHeader[6] = (byte)((sequenceNumber >> 8) & 0xFF);
        tcpHeader[7] = (byte)(sequenceNumber & 0xFF);
        // Acknowledgment number (4 bytes)
        tcpHeader[8] = (byte)((ackNumber >> 24) & 0xFF);
        tcpHeader[9] = (byte)((ackNumber >> 16) & 0xFF);
        tcpHeader[10] = (byte)((ackNumber >> 8) & 0xFF);
        tcpHeader[11] = (byte)(ackNumber & 0xFF);
        // Data offset and reserved (1 byte)
        tcpHeader[12] = (byte)((dataOffset << 4) & 0xF0);
        // Flags (1 byte)
        tcpHeader[13] = (byte)(flags & 0x3F);
        // Window size (2 bytes)
        tcpHeader[14] = (byte)((windowSize >> 8) & 0xFF);
        tcpHeader[15] = (byte)(windowSize & 0xFF);
        // Checksum (2 bytes) - initially zero
        tcpHeader[16] = 0;
        tcpHeader[17] = 0;
        // Urgent pointer (2 bytes)
        tcpHeader[18] = (byte)((urgentPointer >> 8) & 0xFF);
        tcpHeader[19] = (byte)(urgentPointer & 0xFF);
        // Copy payload if present
        if (payload != null && payload.length > 0) {
            System.arraycopy(payload, 0, tcpHeader, headerLength, payload.length);
        }
        return tcpHeader;
    }

    // Computes the TCP checksum using the pseudo header.
    public void computeChecksum(InetAddress src, InetAddress dst) {
        byte[] tcpSegment = toByteArray();
        int tcpLength = tcpSegment.length;
        byte[] pseudoHeader = new byte[12];
        System.arraycopy(src.getAddress(), 0, pseudoHeader, 0, 4);
        System.arraycopy(dst.getAddress(), 0, pseudoHeader, 4, 4);
        pseudoHeader[8] = 0;
        pseudoHeader[9] = 6; // TCP protocol number
        pseudoHeader[10] = (byte)((tcpLength >> 8) & 0xFF);
        pseudoHeader[11] = (byte)(tcpLength & 0xFF);
        byte[] checksumData = new byte[pseudoHeader.length + tcpSegment.length];
        System.arraycopy(pseudoHeader, 0, checksumData, 0, pseudoHeader.length);
        System.arraycopy(tcpSegment, 0, checksumData, pseudoHeader.length, tcpSegment.length);
        checksum = PacketUtil.computeChecksum(checksumData);
    }

    // Returns a byte array of the TCP segment with the computed checksum included.
    public byte[] toByteArrayWithChecksum() {
        byte[] tcpSegment = toByteArray();
        tcpSegment[16] = (byte)((checksum >> 8) & 0xFF);
        tcpSegment[17] = (byte)(checksum & 0xFF);
        return tcpSegment;
    }

    // A simple (partial) parser: assumes the packet starts with an IP header,
    // then the TCP header. In a full implementation you would validate IP fields.
    public static TCPPacket parse(byte[] packetData) {
        if (packetData.length < 40) return null; // minimum IP+TCP length
        int ipHeaderLength = (packetData[0] & 0x0F) * 4;
        int tcpStart = ipHeaderLength;
        int srcPort = ((packetData[tcpStart] & 0xFF) << 8) | (packetData[tcpStart + 1] & 0xFF);
        int dstPort = ((packetData[tcpStart + 2] & 0xFF) << 8) | (packetData[tcpStart + 3] & 0xFF);
        int seqNum = ((packetData[tcpStart + 4] & 0xFF) << 24) | ((packetData[tcpStart + 5] & 0xFF) << 16)
                | ((packetData[tcpStart + 6] & 0xFF) << 8) | (packetData[tcpStart + 7] & 0xFF);
        int ackNum = ((packetData[tcpStart + 8] & 0xFF) << 24) | ((packetData[tcpStart + 9] & 0xFF) << 16)
                | ((packetData[tcpStart + 10] & 0xFF) << 8) | (packetData[tcpStart + 11] & 0xFF);
        int dataOffset = (packetData[tcpStart + 12] >> 4) & 0xF;
        int flags = packetData[tcpStart + 13] & 0x3F;
        TCPPacket tcp = new TCPPacket(srcPort, dstPort, seqNum, ackNum, flags);
        tcp.dataOffset = dataOffset;
        int headerLength = dataOffset * 4;
        int payloadLength = packetData.length - tcpStart - headerLength;
        if (payloadLength > 0) {
            byte[] payload = new byte[payloadLength];
            System.arraycopy(packetData, tcpStart + headerLength, payload, 0, payloadLength);
            tcp.setPayload(payload);
        }
        return tcp;
    }

    public boolean isSYN() {
        return (flags & FLAG_SYN) != 0;
    }

    public boolean isACK() {
        return (flags & FLAG_ACK) != 0;
    }

    public boolean isFIN() {
        return (flags & FLAG_FIN) != 0;
    }

    public int getSequenceNumber() {
        return sequenceNumber;
    }

    public int getAckNumber() {
        return ackNumber;
    }

    public byte[] getPayload() {
        return payload;
    }
}

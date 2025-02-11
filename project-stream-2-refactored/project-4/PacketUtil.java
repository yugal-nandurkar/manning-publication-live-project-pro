public class PacketUtil {
    // Computes the Internet checksum (RFC 1071)
    public static int computeChecksum(byte[] data) {
        int length = data.length;
        int i = 0;
        long sum = 0;
        while (length > 1) {
            int first = (data[i] << 8) & 0xFF00;
            int second = data[i + 1] & 0xFF;
            int word = first | second;
            sum += word;
            if ((sum & 0xFFFF0000) != 0) {
                sum = (sum & 0xFFFF) + (sum >> 16);
            }
            i += 2;
            length -= 2;
        }
        if (length > 0) {
            int last = (data[i] << 8) & 0xFF00;
            sum += last;
            if ((sum & 0xFFFF0000) != 0) {
                sum = (sum & 0xFFFF) + (sum >> 16);
            }
        }
        sum = ~sum & 0xFFFF;
        return (int) sum;
    }
}

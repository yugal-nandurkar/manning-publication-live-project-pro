import java.util.Arrays;
import java.util.List;

public class ReplicaMapper {
    // A list of replica server IP addresses (as Strings).
    // In a real deployment these could be read from configuration.
    private static final List<String> replicaIPs = Arrays.asList(
            "54.172.123.45",  // Replica 1 (e.g., US)
            "34.213.67.89",   // Replica 2 (e.g., Europe)
            "52.201.98.123"   // Replica 3 (e.g., Asia)
    );

    /**
     * Given a client IP (or any identifier) returns the best replica IP.
     * In this simple implementation, we simply compute the hash modulo the number of replicas.
     */
    public static String getBestReplica(String clientIP) {
        int hash = clientIP.hashCode();
        if (hash < 0) {
            hash = -hash;
        }
        int index = hash % replicaIPs.size();
        return replicaIPs.get(index);
    }
}

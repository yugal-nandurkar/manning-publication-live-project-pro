import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * TraceAnalyzer parses an NS-2 trace file to compute performance metrics.
 * It reads each line, groups events by flow (here defined as "fromNode-toNode-protocol"),
 * and calculates:
 *   - Average throughput (Mbps)
 *   - Packet drop count
 *   - Average latency (for packets whose send and receive times can be matched)
 *
 * Usage:
 *   java TraceAnalyzer <traceFile>
 *
 * Note: This parser assumes a trace file format where each line is whitespace-separated,
 * with fields in the following order (indices starting at 0):
 *   [0]: Event type (e.g., 's' for send, 'r' for receive, 'd' for drop)
 *   [1]: Simulation time (double)
 *   [2]: Source node
 *   [3]: Destination node
 *   [4]: Protocol (e.g., tcp, udp)
 *   [5]: Packet size (in bytes)
 *   [8]: Sequence number (optional; if missing, set to -1)
 *
 * Adjust the parsing logic if your NS-2 trace file format differs.
 */
public class TraceAnalyzer {

    /**
     * Inner class to represent one trace event.
     */
    static class PacketEvent {
        public char eventType;       // 's', 'r', or 'd'
        public double time;
        public String fromNode;
        public String toNode;
        public String protocol;
        public int packetSize;
        public int sequenceNumber;

        /**
         * Constructs a PacketEvent by parsing a trace file line.
         * @param line the trace file line.
         */
        public PacketEvent(String line) {
            // Split the line by whitespace
            String[] tokens = line.split("\\s+");
            // Basic validation: skip if not enough tokens
            if (tokens.length < 6) {
                throw new IllegalArgumentException("Insufficient tokens in line: " + line);
            }
            this.eventType = tokens[0].charAt(0);
            this.time = Double.parseDouble(tokens[1]);
            this.fromNode = tokens[2];
            this.toNode = tokens[3];
            this.protocol = tokens[4];
            this.packetSize = Integer.parseInt(tokens[5]);
            // If a sequence number is provided at token index 8, parse it; otherwise, set as -1.
            if (tokens.length > 8) {
                try {
                    this.sequenceNumber = Integer.parseInt(tokens[8]);
                } catch (NumberFormatException e) {
                    this.sequenceNumber = -1;
                }
            } else {
                this.sequenceNumber = -1;
            }
        }
    }

    /**
     * Inner class to collect and compute metrics for a single flow.
     */
    static class FlowMetrics {
        public double firstTime = Double.MAX_VALUE;
        public double lastTime = 0.0;
        public long totalBytesReceived = 0;
        public int dropCount = 0;
        // Map sequence numbers (packet ID) to send times.
        public Map<Integer, Double> sendTimes = new HashMap<>();
        public double totalLatency = 0.0;
        public int receivedCount = 0;

        public void recordSend(PacketEvent event) {
            // Update earliest send time.
            if (event.time < firstTime) {
                firstTime = event.time;
            }
            if (event.sequenceNumber != -1) {
                sendTimes.put(event.sequenceNumber, event.time);
            }
        }

        public void recordReceive(PacketEvent event) {
            if (event.time > lastTime) {
                lastTime = event.time;
            }
            totalBytesReceived += event.packetSize;
            // Compute latency if matching send time exists.
            if (event.sequenceNumber != -1 && sendTimes.containsKey(event.sequenceNumber)) {
                double sendTime = sendTimes.get(event.sequenceNumber);
                double latency = event.time - sendTime;
                totalLatency += latency;
                receivedCount++;
                // Remove the entry to avoid double counting.
                sendTimes.remove(event.sequenceNumber);
            }
        }

        public void recordDrop(PacketEvent event) {
            dropCount++;
        }

        /**
         * Computes average latency in seconds.
         * @return average latency (0 if no packets received).
         */
        public double getAverageLatency() {
            return (receivedCount > 0) ? totalLatency / receivedCount : 0.0;
        }

        /**
         * Computes average throughput in Mbps.
         * Throughput = (totalBytesReceived * 8 bits) / (duration in seconds) / 1e6.
         * @return throughput in Mbps.
         */
        public double getThroughputMbps() {
            double duration = lastTime - firstTime;
            return (duration > 0) ? (totalBytesReceived * 8) / (duration * 1e6) : 0.0;
        }
    }

    /**
     * Creates a simple flow identifier based on source node, destination node, and protocol.
     * @param event the packet event.
     * @return a string representing the flow.
     */
    public static String getFlowId(PacketEvent event) {
        return event.fromNode + "-" + event.toNode + "-" + event.protocol;
    }

    public static void main(String[] args) {
        if (args.length < 1) {
            System.err.println("Usage: java TraceAnalyzer <tracefile>");
            System.exit(1);
        }
        String traceFile = args[0];
        // Map flow IDs to their metrics.
        Map<String, FlowMetrics> flowMetricsMap = new HashMap<>();

        try (BufferedReader br = new BufferedReader(new FileReader(traceFile))) {
            String line;
            while ((line = br.readLine()) != null) {
                // Skip empty lines and comment lines (starting with "#")
                if (line.trim().isEmpty() || line.startsWith("#"))
                    continue;
                try {
                    PacketEvent event = new PacketEvent(line);
                    String flowId = getFlowId(event);
                    // Get current metrics for this flow, or create a new one.
                    FlowMetrics metrics = flowMetricsMap.getOrDefault(flowId, new FlowMetrics());
                    // Process event based on its type.
                    switch (event.eventType) {
                        case 's':  // send event
                            metrics.recordSend(event);
                            break;
                        case 'r':  // receive event
                            metrics.recordReceive(event);
                            break;
                        case 'd':  // drop event
                            metrics.recordDrop(event);
                            break;
                        default:
                            // Other event types (e.g., enqueue, dequeue) are ignored.
                            break;
                    }
                    flowMetricsMap.put(flowId, metrics);
                } catch (Exception e) {
                    System.err.println("Error parsing line: " + line);
                    e.printStackTrace();
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        // Output the computed metrics for each flow.
        System.out.println("Flow Metrics Summary:");
        System.out.println("Flow ID\t\tThroughput (Mbps)\tPacket Drops\tAverage Latency (s)");
        for (Map.Entry<String, FlowMetrics> entry : flowMetricsMap.entrySet()) {
            String flowId = entry.getKey();
            FlowMetrics metrics = entry.getValue();
            double throughput = metrics.getThroughputMbps();
            int drops = metrics.dropCount;
            double avgLatency = metrics.getAverageLatency();
            System.out.println(flowId + "\t" + String.format("%.3f", throughput) +
                    "\t\t" + drops + "\t\t" + String.format("%.6f", avgLatency));
        }
    }
}

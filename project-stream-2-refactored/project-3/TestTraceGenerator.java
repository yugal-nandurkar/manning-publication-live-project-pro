import java.io.FileWriter;
import java.io.IOException;

/**
 * TestTraceGenerator writes a sample NS-2 trace file and then calls the TraceAnalyzer.
 */
public class TestTraceGenerator {

    public static void main(String[] args) {
        String filename = "sample_trace.tr";
        try (FileWriter fw = new FileWriter(filename)) {
            // Write a comment line (to be skipped by the analyzer)
            fw.write("# Sample NS-2 trace file\n");
            // Sample events:
            // Format: [event] [time] [fromNode] [toNode] [protocol] [packetSize] ... [sequenceNumber]
            // A TCP packet from node 1 to node 4, sent at 0.1 sec, sequence number 1.
            fw.write("s 0.1 1 4 tcp 1000 0 0 1\n");
            // A UDP packet from node 2 to node 3, sent at 0.2 sec, sequence number 2.
            fw.write("s 0.2 2 3 udp 500 0 0 2\n");
            // The TCP packet is received at 0.3 sec.
            fw.write("r 0.3 1 4 tcp 1000 0 0 1\n");
            // The UDP packet is received at 0.35 sec.
            fw.write("r 0.35 2 3 udp 500 0 0 2\n");
            // A drop event for a TCP packet (sequence number 3) at 0.4 sec.
            fw.write("d 0.4 1 4 tcp 1000 0 0 3\n");
            // Another TCP send at 0.5 sec (sequence number 4)...
            fw.write("s 0.5 1 4 tcp 1000 0 0 4\n");
            // ... and its corresponding receive at 1.0 sec.
            fw.write("r 1.0 1 4 tcp 1000 0 0 4\n");
        } catch (IOException e) {
            e.printStackTrace();
        }

        // Now run the TraceAnalyzer on the generated sample trace file.
        System.out.println("Running TraceAnalyzer on " + filename + " ...");
        TraceAnalyzer.main(new String[] { filename });
    }
}

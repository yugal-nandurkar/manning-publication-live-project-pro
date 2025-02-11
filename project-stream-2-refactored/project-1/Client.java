// Import all classes in the java.io package for input/output operations (e.g., streams, readers, writers).
import java.io.*;

// Import networking classes (e.g., Socket) from the java.net package for TCP socket communications.
import java.net.*;

// Import SSL networking classes from the javax.net.ssl package for creating secure (SSL) sockets.
import javax.net.ssl.*;

// Define the public class named Client which contains the main entry point of the program.
public class Client {

    // Define a constant for the default port used when SSL is not required.
    // Non-SSL communication defaults to port 27993.
    private static final int DEFAULT_PORT = 27993;

    // Define a constant for the default SSL port.
    // When SSL is requested and the port is not overridden, use port 27994.
    private static final int DEFAULT_SSL_PORT = 27994;

    // Define a constant magic string that is used as a protocol identifier in all messages.
    private static final String MAGIC_STRING = "cs5700spring2015";

    // The main method: entry point of the Java application.
    public static void main(String[] args) {

        // Initialize the port variable with the default port (for non-SSL mode).
        int port = DEFAULT_PORT;

        // Initialize a flag indicating whether SSL should be used; default is false.
        boolean useSSL = false;

        // Declare a variable to hold the server hostname; initially null.
        String hostname = null;

        // Declare a variable to hold the NEU ID; initially null.
        String neuId = null;

        // Check if the number of command-line arguments is less than 2.
        // If there are too few arguments, print usage instructions to stderr and exit.
        if (args.length < 2) {
            System.err.println("Usage: client <-p port> <-s> [hostname] [NEU ID]");
            System.exit(1);
        }

        // Create an index variable to keep track of the current argument position.
        int index = 0;

        // Check if the current argument equals "-p", which indicates that a port number is provided.
        if (args[index].equals("-p")) {
            // Ensure that there is a port value after "-p"; if not, print an error and exit.
            if (args.length < index + 2) {
                System.err.println("Error: Port value is missing after -p");
                System.exit(1);
            }
            try {
                // Parse the string following "-p" as an integer and assign it to the port variable.
                port = Integer.parseInt(args[index + 1]);
            } catch (NumberFormatException e) {
                // If parsing fails (i.e., the port is not a valid integer), print an error and exit.
                System.err.println("Error: Invalid port number.");
                System.exit(1);
            }
            // Increment the index by 2 since two arguments ("-p" and the port number) have been processed.
            index += 2;
        }

        // Check if the current argument exists and equals "-s", indicating SSL mode.
        if (index < args.length && args[index].equals("-s")) {
            // Set the useSSL flag to true to indicate that an SSL connection is required.
            useSSL = true;
            // Move to the next argument by incrementing the index.
            index++;
            // If the port has not been overridden by the user (still the default non-SSL port),
            // then switch the port to the default SSL port.
            if (port == DEFAULT_PORT) {
                port = DEFAULT_SSL_PORT;
            }
        }

        // After processing options, there should be exactly two arguments remaining: hostname and NEU ID.
        // If not, print usage instructions and exit.
        if (args.length - index != 2) {
            System.err.println("Usage: client <-p port> <-s> [hostname] [NEU ID]");
            System.exit(1);
        }
        // Assign the hostname from the current argument.
        hostname = args[index];
        // Assign the NEU ID from the next argument.
        neuId = args[index + 1];

        // Initialize a Socket variable for network communication; start with a null value.
        Socket socket = null;
        try {
            // Check if SSL mode is requested.
            if (useSSL) {
                // Obtain the default SSL socket factory.
                SSLSocketFactory sslFactory = (SSLSocketFactory) SSLSocketFactory.getDefault();
                // Create an SSL socket to the specified hostname and port.
                socket = sslFactory.createSocket(hostname, port);
            } else {
                // If not using SSL, create a standard TCP socket to the specified hostname and port.
                socket = new Socket(hostname, port);
            }

            // Set up an input stream to read from the socket, using US-ASCII encoding.
            // BufferedReader wraps an InputStreamReader for efficient reading of text lines.
            BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream(), "US-ASCII"));

            // Set up an output stream to write to the socket, also using US-ASCII encoding.
            // BufferedWriter wraps an OutputStreamWriter to allow writing of text lines.
            BufferedWriter out = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream(), "US-ASCII"));

            // Compose the HELLO message using the magic string, the literal "HELLO", and the NEU ID.
            // The message is terminated with a newline character (\n).
            String helloMessage = MAGIC_STRING + " HELLO " + neuId + "\n";

            // Write the HELLO message to the output stream.
            out.write(helloMessage);

            // Flush the stream to ensure that the HELLO message is sent immediately.
            out.flush();

            // Enter an infinite loop to continuously process messages from the server.
            while (true) {
                // Read a line of text from the server.
                String serverLine = in.readLine();

                // If readLine returns null, it means the server closed the connection unexpectedly.
                if (serverLine == null) {
                    System.err.println("Error: Connection closed unexpectedly.");
                    System.exit(1);
                }

                // Split the received line into tokens based on space characters.
                String[] tokens = serverLine.split(" ");

                // Verify that the received message is well-formed by checking:
                //   1. There are at least 2 tokens.
                //   2. The first token equals the expected magic string.
                if (tokens.length < 2 || !tokens[0].equals(MAGIC_STRING)) {
                    System.err.println("Error: Received malformed message: " + serverLine);
                    System.exit(1);
                }

                // Check if the message is a BYE message.
                // The protocol specifies that the last token should be "BYE" for the termination message.
                if (tokens[tokens.length - 1].equals("BYE")) {
                    // For a valid BYE message, there should be exactly 3 tokens.
                    if (tokens.length != 3) {
                        System.err.println("Error: Malformed BYE message: " + serverLine);
                        System.exit(1);
                    }
                    // The secret flag (which the client must print) is the second token.
                    String secretFlag = tokens[1];
                    // Print the secret flag to the standard output.
                    System.out.println(secretFlag);
                    // Break out of the loop to end the program since the protocol has finished.
                    break;
                }
                // Otherwise, check if the message is a STATUS message.
                else if (tokens[1].equals("STATUS")) {
                    // A valid STATUS message must have exactly 5 tokens:
                    // magic string, "STATUS", operand1, operator, operand2.
                    if (tokens.length != 5) {
                        System.err.println("Error: Malformed STATUS message: " + serverLine);
                        System.exit(1);
                    }

                    // Declare variables to hold the two operands.
                    int operand1, operand2;
                    try {
                        // Parse the third token as the first integer operand.
                        operand1 = Integer.parseInt(tokens[2]);
                        // Parse the fifth token as the second integer operand.
                        operand2 = Integer.parseInt(tokens[4]);
                    } catch (NumberFormatException e) {
                        // If parsing fails, print an error indicating an invalid number and exit.
                        System.err.println("Error: Invalid number in expression: " + serverLine);
                        System.exit(1);
                        return; // This return is redundant due to exit, but included for clarity.
                    }

                    // Extract the operator from the fourth token.
                    String operator = tokens[3];

                    // Initialize the result variable that will hold the computed result of the expression.
                    int result = 0;

                    // Use a switch statement to determine which arithmetic operation to perform based on the operator.
                    switch (operator) {
                        // If the operator is "+", add operand1 and operand2.
                        case "+":
                            result = operand1 + operand2;
                            break;
                        // If the operator is "-", subtract operand2 from operand1.
                        case "-":
                            result = operand1 - operand2;
                            break;
                        // If the operator is "*", multiply operand1 by operand2.
                        case "*":
                            result = operand1 * operand2;
                            break;
                        // If the operator is "/", perform integer division (which floors the result).
                        case "/":
                            // Note: In Java, integer division automatically floors the result.
                            result = operand1 / operand2;
                            break;
                        // If the operator does not match any of the expected cases, print an error and exit.
                        default:
                            System.err.println("Error: Unknown operator " + operator);
                            System.exit(1);
                    }

                    // Compose the SOLUTION message by concatenating the magic string, the computed result, and a newline.
                    String solutionMessage = MAGIC_STRING + " " + result + "\n";

                    // Write the SOLUTION message to the output stream.
                    out.write(solutionMessage);

                    // Flush the output stream to ensure the message is sent immediately.
                    out.flush();
                } else {
                    // If the message type is not recognized (neither BYE nor STATUS), print an error and exit.
                    System.err.println("Error: Unknown message type: " + serverLine);
                    System.exit(1);
                }
            }
        } catch (IOException e) {
            // Catch any IOExceptions that occur during communication (e.g., connection issues).
            // Print the error message to stderr and exit.
            System.err.println("Error: " + e.getMessage());
            System.exit(1);
        } finally {
            // The finally block ensures that the socket is closed regardless of whether an exception occurred.
            if (socket != null) {
                try {
                    // Attempt to close the socket.
                    socket.close();
                } catch (IOException e) {
                    // If an exception occurs while closing the socket, ignore it.
                    // Typically, cleanup exceptions during close are not critical.
                }
            }
        }
    }
}

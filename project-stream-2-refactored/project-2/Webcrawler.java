// Import BufferedReader for efficient reading of text from an input stream.
import java.io.BufferedReader; // Needed to read lines from the socket's input stream
// Import BufferedWriter for efficient writing of text to an output stream.
import java.io.BufferedWriter; // Needed to write text to the socket's output stream
// Import InputStreamReader to convert byte streams (from a socket) into character streams.
import java.io.InputStreamReader; // Converts raw byte data from the socket into characters
// Import OutputStreamWriter to convert character streams to byte streams for a socket.
import java.io.OutputStreamWriter; // Converts characters to bytes to send over the socket
// Import IOException to handle input/output related exceptions.
import java.io.IOException; // Required for catching exceptions during I/O operations
// Import PrintWriter to print formatted representations of objects to an output stream.
import java.io.PrintWriter; // Provides convenience methods for printing to the socket's output
// Import Reader as a general class for reading character streams.
import java.io.Reader; // Abstract class for reading character streams (not used directly here)
// Import Socket to create TCP socket connections.
import java.net.Socket; // Used to create and manage a network socket connection
// Import Matcher for matching regular expressions against text.
import java.util.regex.Matcher; // Allows matching patterns (e.g., for flag extraction)
// Import Pattern to compile regular expressions.
import java.util.regex.Pattern; // Used to define regex patterns for matching text
// Import all classes from the java.util package (e.g., collections, maps, lists).
import java.util.*; // Provides access to collections like HashSet, LinkedList, etc.


// Main class implementing the web crawler.
public class WebCrawler {

    // Constants for the target host and port.
    private static final String HOST = "cs5700sp15.ccs.neu.edu"; // The target host for Fakebook
    private static final int PORT = 80; // The target port (HTTP standard port)

    // The login path and the starting path for Fakebook.
    private static final String LOGIN_PATH = "/accounts/login/?next=/fakebook/"; // URL path used for logging in
    private static final String START_PATH = "/fakebook/"; // Starting path for crawling Fakebook pages

    // Maximum number of secret flags to collect.
    private static final int FLAG_COUNT = 5; // We expect to find 5 secret flags during the crawl

    // Pattern to extract a secret flag from HTML.
    private static final Pattern FLAG_PATTERN = Pattern.compile(
            "<h2 class='secret_flag' style=\"color:red\">FLAG: ([A-Za-z0-9]{64})</h2>"
            // This regex matches an H2 element with class 'secret_flag' and style color:red containing
            // "FLAG: " followed by 64 alphanumeric characters (the secret flag)
    );

    // Pattern to extract href links from an anchor tag.
    private static final Pattern LINK_PATTERN = Pattern.compile(
            "<a\\s+href=\"([^\"]+)\""
            // This regex matches an anchor tag (<a ...>) and extracts the value of the href attribute.
    );

    // HTTP response representation.
    static class HttpResponse {
        int statusCode; // The HTTP status code (e.g., 200, 404)
        Map<String, String> headers; // A map of HTTP response headers (e.g., "Content-Type")
        String body; // The body of the HTTP response (e.g., the HTML content)

        // Constructor to initialize the HTTP response object with a status code, headers, and body.
        HttpResponse(int statusCode, Map<String, String> headers, String body) {
            this.statusCode = statusCode; // Set the status code
            this.headers = headers;       // Set the headers map
            this.body = body;             // Set the response body
        }
    }

    // Entry point of the program.
    public static void main(String[] args) {
        // Ensure exactly two arguments: username and password.
        if (args.length != 2) { // If the number of command-line arguments is not exactly 2...
            System.err.println("Usage: ./webcrawler [username] [password]"); // ...print usage instructions
            System.exit(1); // Terminate the program with an error code
        }
        String username = args[0]; // Retrieve the username from the first command-line argument
        String password = args[1]; // Retrieve the password from the second command-line argument

        // Perform login and get the session cookies.
        Map<String, String> cookies = login(username, password); // Attempt to log in using the provided credentials
        if (cookies == null) { // If login fails (cookies are null)...
            System.err.println("Login failed."); // ...print an error message...
            System.exit(1); // ...and terminate the program.
        }

        // Crawl Fakebook starting from START_PATH.
        Set<String> flagsFound = crawl(cookies); // Begin crawling using the session cookies and store discovered flags

        // Check that we have found the required number of flags.
        if (flagsFound.size() < FLAG_COUNT) { // If the number of flags found is less than expected...
            System.err.println("Error: Expected " + FLAG_COUNT + " flags, but found only " + flagsFound.size());
            // ...print an error message with the expected and actual number of flags...
            System.exit(1); // ...and terminate the program.
        }

        // Print exactly five lines of output (one secret flag per line).
        int printed = 0; // Initialize a counter for the number of flags printed
        for (String flag : flagsFound) { // Loop through each discovered flag
            if (printed >= FLAG_COUNT) break; // If we've printed the expected number of flags, exit the loop
            System.out.println(flag); // Print the flag to standard output
            printed++; // Increment the printed flag counter
        }
    }

    /**
     * Logs in to Fakebook using an HTTP POST.
     * Builds a POST request with the provided username and password.
     * Returns a map of cookies set by the server.
     */
    private static Map<String, String> login(String username, String password) {
        Map<String, String> cookies = new HashMap<>(); // Create a new HashMap to store cookies from the server

        // Build POST data. We include the 'next' parameter so that after login we land in Fakebook.
        String postData = "username=" + encode(username) +  // Append the encoded username
                "&password=" + encode(password) + // Append the encoded password
                "&next=/fakebook/";              // Append the 'next' parameter with the Fakebook path

        // Build the HTTP POST request string.
        StringBuilder request = new StringBuilder(); // Create a StringBuilder to construct the HTTP request
        request.append("POST " + LOGIN_PATH + " HTTP/1.1\r\n"); // Append the request line with POST, login path, and HTTP version
        request.append("Host: " + HOST + "\r\n"); // Append the Host header with the target host
        request.append("Content-Type: application/x-www-form-urlencoded\r\n"); // Append Content-Type for form data
        request.append("Content-Length: " + postData.length() + "\r\n"); // Append Content-Length header with the length of postData
        request.append("Connection: close\r\n"); // Append the Connection header to signal that the connection will be closed after the request
        // End of headers.
        request.append("\r\n"); // Append a blank line to separate headers from the body
        // Append the POST data.
        request.append(postData); // Append the form data (username, password, next) to the request

        // Send the request and get the response.
        HttpResponse response = sendHttpRequest(request.toString()); // Convert the request to a string and send it using sendHttpRequest
        if (response == null) { // If no response was received...
            return null; // ...return null indicating login failure
        }

        // Check for a successful login (assume 302 Found redirect or 200 OK).
        if (response.statusCode != 302 && response.statusCode != 200) { // If the status code is neither 302 nor 200...
            System.err.println("Login failed with status code: " + response.statusCode); // ...print an error message with the status code...
            return null; // ...and return null to indicate failure.
        }

        // Extract cookies from the Set-Cookie headers.
        for (Map.Entry<String, String> entry : response.headers.entrySet()) { // Iterate over each header in the response
            if (entry.getKey().equalsIgnoreCase("Set-Cookie")) { // Check if the header is a Set-Cookie header (case-insensitive)
                // There could be multiple cookies in one header line separated by semicolons.
                String[] cookieParts = entry.getValue().split(";"); // Split the cookie header value by ";" to separate individual cookies
                if (cookieParts.length > 0) { // If at least one cookie part exists...
                    String[] pair = cookieParts[0].split("=", 2); // Split the first cookie part into key and value (limit to 2 parts)
                    if (pair.length == 2) { // If both key and value are present...
                        cookies.put(pair[0].trim(), pair[1].trim()); // ...store the cookie in the map after trimming whitespace
                    }
                }
            }
        }

        return cookies; // Return the map of cookies extracted from the login response
    }

    /**
     * The main crawling method.
     * Uses a breadth-first search to traverse pages under Fakebook.
     * Only URLs within the target domain (or relative URLs) are crawled.
     * Returns a set of secret flags discovered.
     */
    private static Set<String> crawl(Map<String, String> cookies) {
        Set<String> visited = new HashSet<>(); // Create a set to keep track of visited URLs (to avoid loops)
        Set<String> flags = new LinkedHashSet<>(); // Create a set to store discovered secret flags (using LinkedHashSet to maintain order)
        Queue<String> frontier = new LinkedList<>(); // Create a queue to manage the frontier of URLs to crawl

        // Start at the Fakebook root.
        frontier.add(START_PATH); // Add the starting path (Fakebook homepage) to the frontier

        // Continue crawling until there are no more URLs to visit or until we have enough flags.
        while (!frontier.isEmpty() && flags.size() < FLAG_COUNT) {
            String path = frontier.poll(); // Remove and retrieve the next URL path from the frontier

            // Skip if already visited.
            if (visited.contains(path)) continue; // If the current path has already been visited, skip processing it
            visited.add(path); // Mark the current path as visited

            // Fetch the page with retry logic for 500 errors.
            HttpResponse response = getPageWithRetries(path, cookies); // Try to fetch the page at the current path using the cookies
            if (response == null) continue; // If no response is obtained, skip to the next URL

            // Handle HTTP redirection.
            if (response.statusCode == 301 || response.statusCode == 302) { // If the response indicates a redirection...
                String location = response.headers.get("Location"); // Retrieve the new location URL from the Location header
                if (location != null && isValidUrl(location)) { // If the new location is not null and is valid for crawling...
                    String normalized = normalizeUrl(location); // Normalize the URL (convert to relative if needed)
                    if (!visited.contains(normalized)) { // If the normalized URL has not been visited...
                        frontier.add(normalized); // ...add it to the frontier for future crawling
                    }
                }
                continue; // Skip further processing of the current URL since it's a redirection
            }

            // Skip pages that are not OK.
            if (response.statusCode != 200) { // If the HTTP status is not 200 (OK)...
                // For 403, 404, etc., we simply do not follow the link.
                continue; // ...skip processing this page
            }

            // Extract and store the flag if present.
            String flag = extractFlag(response.body); // Attempt to extract a secret flag from the page's body
            if (flag != null) { // If a flag is found...
                flags.add(flag); // ...add the flag to the set of discovered flags
            }

            // Extract URLs from the page.
            List<String> urls = extractUrls(response.body); // Extract all URLs (links) from the page's HTML
            for (String url : urls) { // Iterate over each extracted URL...
                String normalized = normalizeUrl(url); // Normalize the URL to get a relative path if possible
                // Only crawl URLs that belong to the same domain or are relative.
                if (isValidUrl(normalized) && !visited.contains(normalized)) { // If the URL is valid and has not been visited...
                    frontier.add(normalized); // ...add it to the frontier for future crawling
                }
            }
        }
        return flags; // Return the set of secret flags discovered during the crawl
    }

    /**
     * Attempts to fetch the page at the given path.
     * If a 500 Internal Server Error is returned, it retries until successful.
     */
    private static HttpResponse getPageWithRetries(String path, Map<String, String> cookies) {
        HttpResponse response = null; // Initialize the response to null
        int attempts = 0; // Initialize a counter for the number of attempts
        // Retry up to 5 times on 500 Internal Server Error.
        while (attempts < 5) { // Loop until 5 attempts have been made
            response = getPage(path, cookies); // Attempt to fetch the page at the given path
            if (response != null && response.statusCode != 500) { // If the response is valid and not a 500 error...
                break; // ...exit the loop
            }
            attempts++; // Increment the attempt counter
            try {
                Thread.sleep(1000); // Wait for 1 second before retrying
            } catch (InterruptedException e) { // Catch any interruption during sleep
                // Ignore interruption.
            }
        }
        return response; // Return the final response (or null if unsuccessful)
    }

    /**
     * Sends an HTTP GET request for the given path with the provided cookies.
     */
    private static HttpResponse getPage(String path, Map<String, String> cookies) {
        StringBuilder request = new StringBuilder(); // Create a StringBuilder to construct the GET request
        request.append("GET " + path + " HTTP/1.1\r\n"); // Append the request line with GET method, target path, and HTTP version
        request.append("Host: " + HOST + "\r\n"); // Append the Host header with the target host
        // Add the Cookie header if any cookies are stored.
        if (!cookies.isEmpty()) { // If the cookies map is not empty...
            request.append("Cookie: "); // Start the Cookie header line
            boolean first = true; // Flag to format multiple cookies correctly
            for (Map.Entry<String, String> cookie : cookies.entrySet()) { // Loop through each cookie in the map
                if (!first) { // If this is not the first cookie...
                    request.append("; "); // ...append a semicolon and space as a separator
                }
                request.append(cookie.getKey() + "=" + cookie.getValue()); // Append the cookie key and its value in key=value format
                first = false; // Mark that we have added at least one cookie
            }
            request.append("\r\n"); // End the Cookie header line with a newline
        }
        request.append("Connection: close\r\n"); // Append the Connection header to signal that the connection will close after the request
        request.append("\r\n"); // Append a blank line to indicate the end of headers

        return sendHttpRequest(request.toString()); // Convert the constructed request to a string, send it, and return the response
    }

    /**
     * Opens a socket connection, sends the HTTP request, and reads the response.
     * Returns an HttpResponse object.
     */
    private static HttpResponse sendHttpRequest(String request) {
        Socket socket = null; // Declare a socket variable for the connection, initially null
        try {
            socket = new Socket(HOST, PORT); // Create a new socket connection to the specified host and port
            // Write the request to the socket.
            PrintWriter out = new PrintWriter(new BufferedWriter(new OutputStreamWriter(socket.getOutputStream())), true);
            // Create a PrintWriter wrapped around a BufferedWriter and OutputStreamWriter to write the request text to the socket's output stream
            out.print(request); // Write the HTTP request string to the socket
            out.flush(); // Flush the stream to ensure the entire request is sent immediately

            // Read the response from the socket.
            BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            // Create a BufferedReader to read the response from the socket's input stream
            HttpResponse response = parseHttpResponse(in); // Parse the HTTP response using the parseHttpResponse method
            return response; // Return the parsed HttpResponse object
        } catch (IOException e) { // Catch any IOException that occurs during the HTTP request/response cycle
            System.err.println("Error during HTTP request: " + e.getMessage()); // Print an error message with the exception details
            return null; // Return null to indicate a failure in the HTTP request
        } finally {
            // Ensure the socket is closed.
            if (socket != null) { // If the socket was successfully created...
                try {
                    socket.close(); // ...attempt to close the socket to free resources
                } catch (IOException e) { // Catch any IOException that occurs while closing the socket
                    // Ignore errors on close.
                }
            }
        }
    }

    /**
     * Parses an HTTP response from the given reader.
     * It reads the status line, headers, and body.
     */
    private static HttpResponse parseHttpResponse(BufferedReader in) throws IOException {
        // Read the status line.
        String statusLine = in.readLine(); // Read the first line of the response (the status line)
        if (statusLine == null || statusLine.isEmpty()) { // If the status line is null or empty...
            return null; // ...return null indicating an invalid response
        }
        String[] statusParts = statusLine.split(" "); // Split the status line by spaces into an array
        int statusCode = 0; // Initialize the status code variable
        try {
            statusCode = Integer.parseInt(statusParts[1]); // Parse the second part of the status line as the status code (e.g., 200)
        } catch (Exception e) { // Catch any exceptions if parsing fails
            // Parsing error; return null.
            return null; // Return null if unable to parse the status code
        }

        // Read headers.
        Map<String, String> headers = new HashMap<>(); // Create a HashMap to store response headers
        String line; // Declare a variable to hold each header line
        while ((line = in.readLine()) != null && !line.isEmpty()) { // Read header lines until a blank line is encountered
            int colonIndex = line.indexOf(":"); // Find the position of the colon that separates the header name and value
            if (colonIndex != -1) { // If a colon is found...
                String headerName = line.substring(0, colonIndex).trim(); // Extract and trim the header name
                String headerValue = line.substring(colonIndex + 1).trim(); // Extract and trim the header value
                // For multiple cookies, we simply concatenate.
                if (headers.containsKey(headerName)) { // If this header already exists (e.g., multiple Set-Cookie headers)...
                    headers.put(headerName, headers.get(headerName) + ", " + headerValue); // Append the new value separated by a comma
                } else {
                    headers.put(headerName, headerValue); // Otherwise, add the header name and value to the map
                }
            }
        }

        // Read the body.
        StringBuilder bodyBuilder = new StringBuilder(); // Create a StringBuilder to accumulate the response body
        while ((line = in.readLine()) != null) { // Continue reading lines until the end of the stream
            bodyBuilder.append(line + "\n"); // Append each line and a newline character to the body
        }

        // Return a new HttpResponse object with the parsed status code, headers, and body.
        return new HttpResponse(statusCode, headers, bodyBuilder.toString());
    }

    /**
     * Extracts a secret flag from the HTML body using a regular expression.
     * Returns the flag if found, or null otherwise.
     */
    private static String extractFlag(String body) {
        Matcher matcher = FLAG_PATTERN.matcher(body); // Create a Matcher to apply the FLAG_PATTERN regex on the response body
        if (matcher.find()) { // If the regex finds a match in the body...
            return matcher.group(1); // ...return the first capturing group (the secret flag)
        }
        return null; // If no match is found, return null
    }

    /**
     * Extracts URLs from the HTML body using a regular expression.
     * Returns a list of URL strings.
     */
    private static List<String> extractUrls(String body) {
        List<String> urls = new ArrayList<>(); // Create a list to store the extracted URLs
        Matcher matcher = LINK_PATTERN.matcher(body); // Create a Matcher to apply the LINK_PATTERN regex on the response body
        while (matcher.find()) { // Loop while the regex finds matches in the body
            String url = matcher.group(1); // Get the first capturing group from the match (the URL)
            urls.add(url); // Add the extracted URL to the list
        }
        return urls; // Return the list of URLs extracted from the body
    }

    /**
     * Normalizes a URL.
     * If the URL is relative (starts with "/"), it is returned as-is.
     * If the URL is absolute and points to the target host, returns the path part.
     * Otherwise, returns null.
     */
    private static String normalizeUrl(String url) {
        if (url.startsWith("/")) { // Check if the URL starts with a "/" (i.e., it is a relative URL)
            return url; // Return the relative URL as-is
        } else if (url.startsWith("http://")) { // If the URL is absolute and starts with "http://"
            // Check if it is on our target host.
            int hostIndex = url.indexOf(HOST); // Find the index where the target host appears in the URL
            if (hostIndex != -1) { // If the target host is found in the URL...
                // Extract the part after the host.
                int pathIndex = url.indexOf("/", url.indexOf(HOST) + HOST.length()); // Find the index of the "/" after the host name
                if (pathIndex != -1) { // If a path is present after the host...
                    return url.substring(pathIndex); // Return the substring from the path onwards (making it relative)
                }
            }
        }
        // URL is not valid for crawling.
        return null; // Return null if the URL does not meet the criteria for crawling
    }

    /**
     * Checks if a given URL (after normalization) is valid for crawling.
     * In our case, it is valid if it is non-null and starts with "/fakebook/".
     */
    private static boolean isValidUrl(String url) {
        return url != null && url.startsWith("/fakebook/"); // Return true if the URL is non-null and begins with "/fakebook/", false otherwise
    }

    /**
     * Simple URL encoder for POST data (only handles basic characters).
     */
    private static String encode(String s) {
        // For simplicity, replace spaces with '+' and leave other characters unchanged.
        // In a real implementation, all unsafe characters should be properly encoded.
        return s.replace(" ", "+"); // Replace all space characters in the string with '+' and return the encoded string
    }
}

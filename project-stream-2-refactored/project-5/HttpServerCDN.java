import com.sun.net.httpserver.HttpServer;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import java.io.*;
import java.net.*;
import java.util.LinkedHashMap;
import java.util.Map;

public class HTTPServerCDN {

    // In-memory cache using an LRU strategy.
    // Key: URL path; Value: CachedContent
    private static final int CACHE_CAPACITY = 10; // maximum number of entries
    private static final Map<String, CachedContent> cache = new LinkedHashMap<String, CachedContent>(CACHE_CAPACITY, 0.75f, true) {
        @Override
        protected boolean removeEldestEntry(Map.Entry<String, CachedContent> eldest) {
            return size() > CACHE_CAPACITY;
        }
    };

    // The origin server from which to fetch content if not cached.
    private final String originServer;

    // The port on which the HTTP server listens.
    private final int port;

    public HTTPServerCDN(int port, String originServer) {
        this.port = port;
        this.originServer = originServer;
    }

    public void start() throws IOException {
        HttpServer server = HttpServer.create(new InetSocketAddress(port), 0);
        System.out.println("HTTP Server (CDN Replica) started on port " + port);
        server.createContext("/", new CDNHandler());
        server.setExecutor(null); // default executor
        server.start();
    }

    // Handler for incoming HTTP requests.
    static class CDNHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            // Only handle GET requests.
            if (!exchange.getRequestMethod().equalsIgnoreCase("GET")) {
                exchange.sendResponseHeaders(405, -1); // 405 Method Not Allowed
                return;
            }
            String path = exchange.getRequestURI().getPath();
            System.out.println("HTTP GET: " + path);
            byte[] content;
            synchronized (cache) {
                CachedContent cached = cache.get(path);
                if (cached != null) {
                    // Cache hit: update hit count and use cached content.
                    cached.hitNo++;
                    content = cached.content;
                    System.out.println("Cache hit for " + path + " (hits: " + cached.hitNo + ")");
                } else {
                    // Cache miss: fetch content from origin server.
                    System.out.println("Cache miss for " + path + ". Fetching from origin...");
                    content = fetchFromOrigin(path);
                    // Store in cache
                    cache.put(path, new CachedContent(path, content));
                }
            }
            // Respond with content.
            exchange.sendResponseHeaders(200, content.length);
            OutputStream os = exchange.getResponseBody();
            os.write(content);
            os.close();
        }

        /**
         * Fetch content from the origin server. Assumes HTTP protocol and port 80.
         */
        private byte[] fetchFromOrigin(String path) throws IOException {
            URL url = new URL("http://" + CDNConfig.ORIGIN_SERVER + path);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            try (InputStream is = conn.getInputStream()) {
                byte[] buffer = new byte[4096];
                int bytesRead;
                while ((bytesRead = is.read(buffer)) != -1) {
                    baos.write(buffer, 0, bytesRead);
                }
            }
            return baos.toByteArray();
        }
    }

    // Represents a cached content entry.
    static class CachedContent {
        String domainName; // URL path (e.g., /index.html)
        byte[] content;
        int hitNo;

        public CachedContent(String domainName, byte[] content) {
            this.domainName = domainName;
            this.content = content;
            this.hitNo = 1;
        }
    }

    // Simple configuration holder.
    public static class CDNConfig {
        public static String ORIGIN_SERVER = "origin.example.com"; // default; to be set via command-line
    }

    public static void main(String[] args) {
        // Expected arguments: -p <port> -o <origin>
        if (args.length < 4) {
            System.err.println("Usage: ./httpserver -p <port> -o <origin>");
            System.exit(1);
        }
        int port = 0;
        String origin = null;
        for (int i = 0; i < args.length; i++) {
            if ("-p".equals(args[i]) && i + 1 < args.length) {
                port = Integer.parseInt(args[i + 1]);
                i++;
            } else if ("-o".equals(args[i]) && i + 1 < args.length) {
                origin = args[i + 1];
                i++;
            }
        }
        if (port == 0 || origin == null) {
            System.err.println("Invalid arguments.");
            System.exit(1);
        }
        CDNConfig.ORIGIN_SERVER = origin;
        HTTPServerCDN httpServer = new HTTPServerCDN(port, origin);
        try {
            httpServer.start();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}

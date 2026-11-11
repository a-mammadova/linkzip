import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;

public  class Linkzip {
    // limits
    private static final int MAX_REQUEST_LINE_SIZE = 8192;
    private static final int MAX_HEADER_LINE_SIZE = 8192;
    private static final int MAX_HEADER_SIZE = 16384;
    private static final int MAX_BODY_SIZE = 8192;


    private static final int PORT = 8080;
    private static final String BASE_URL = "http://localhost:" + PORT + "/";

    private static final Map<String, String> urls = new ConcurrentHashMap<>();
    private static final String alph = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";

    public static void main(String[] args){

        // try-w-res closes sources inside () auto
        try (ServerSocket serverSocket = new ServerSocket(PORT)) {
            System.out.println("linkzip on: " + BASE_URL);

            while (true) {
                Socket clientSocket = serverSocket.accept(); // accept tcp conn
                new Thread(() -> handleClientSafely(clientSocket)).start();
            }

        } catch(IOException e) {
            System.err.println("server error, bro ->" + e.getMessage());
        }
    }

    // method for main loop
    private static void handleClientSafely(Socket socket) {
        try {
            handleClient(socket);
        } catch (IOException e) {
            System.err.println("client i/o error: " + e.getMessage());
        } catch (RuntimeException e) {
            System.err.println("unexpected client error: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private static void handleClient(Socket socket) throws IOException {
        try (socket;
             InputStream reader = socket.getInputStream();
             OutputStream out = socket.getOutputStream();
        ) {
            try {

                // read, parse incoming request
                String request = readHttpLine(reader, MAX_REQUEST_LINE_SIZE);

                System.out.println("just checking what is request ->\n" + request + "\n"); // DELETE

                if (request == null || request.isEmpty()) {
                    sendResponse(out, "400", "Bad Request", "empty request");
                    return;
                }

                String[] requestParts = request.trim().split(" ", 3);

                if (requestParts.length !=3 || requestParts[0].isEmpty() ||
                        requestParts[1].isEmpty() || requestParts[2].isEmpty()) {
                    sendResponse(out, "400", "Bad Request", "malformed request");
                    return;
                }

                String method = requestParts[0];
                String path = requestParts[1];
                String httpVersion = requestParts[2];

                if (!httpVersion.equalsIgnoreCase("http/1.1")) {
                    sendResponse(out, "505", "HTTP Version Not Supported", "");
                    return;
                }

                if (!method.equalsIgnoreCase("get") && !method.equalsIgnoreCase("post")) {
                    sendResponse(out, "405", "Method Not Allowed",
                            "unsupported method or some nonsense");
                    return;
                }

                System.out.printf("request: %s, path: %s\n", method, path); // DELETE

                // get content length
                int contentLength = 0;
                String header;

                while ((header = readHttpLine(reader, MAX_HEADER_LINE_SIZE)) != null && !header.isEmpty()) {

                    int separator = header.indexOf(":");

                    if (separator <= 0) {
                        throw new IOException("malformed http request, missing ':'");
                    }

                    String headerName = header.substring(0, separator).trim();
                    String headerValue = header.substring(separator + 1).trim();

                    if (headerName.equalsIgnoreCase("Content-Length")) {
                        contentLength = Integer.parseInt(headerValue);
                    }
                }

                // handle responses for methods
                if (method.equalsIgnoreCase("POST") && path.equalsIgnoreCase("/zip")) {
                    byte[] bodyBytes = reader.readNBytes(contentLength);

                    if (bodyBytes.length != contentLength) {
                        throw new IOException("unexpected end of line");
                    }

                    String longUrl = new String(bodyBytes, StandardCharsets.UTF_8);

                    if (longUrl.isEmpty()) {
                        sendResponse(out, "400", "Bad Request", "missing url");
                        System.out.println();
                        return;
                    }

                    String key;
                    do {
                        key = generateKey();
                    }
                    while (urls.putIfAbsent(key, longUrl) != null);
                    String shortUlr = BASE_URL + key;

                    sendResponse(out, "201", "Created", shortUlr);
                    System.out.println();
                } else if (method.equalsIgnoreCase("GET") && path.length() == 7 && path.startsWith("/")) {
                    String key = path.substring(1); // getting rid of /
                    String longUrl = urls.get(key);

                    if (longUrl != null) {
                        sendRedirect(out, longUrl);
                        System.out.println();
                    } else {
                        sendResponse(out, "404", "not found", "");
                        System.out.println();
                    }
                } else {
                    sendResponse(out, "404", "not found", "use POST /zip or GET /{key}");
                    System.out.println();
                }
            } catch (IOException | NumberFormatException e) {
                throw new RuntimeException(e);
            }
        }
    }

    private static String readHttpLine(InputStream reader, int maxBytes) throws IOException{
        ByteArrayOutputStream line = new ByteArrayOutputStream();
        int current;

        while ((current = reader.read()) != -1) {
            if (current == '\r') {
                int next = reader.read();
                if (next != '\n') {throw new IOException("where the hell is \\n");}
                return line.toString(StandardCharsets.ISO_8859_1); // iso_8859_1 is suitable for reading http/1.1 headers
            }

            if (current == '\n') { throw new IOException("where the hell is \\r");}
            line.write(current);
            if (line.size() >= maxBytes) {throw new IOException("what the hell have u written longer than 8192");}
        }

        if (line.size() == 0) {return null;}
        throw new IOException("unexpected end of line");
    }

    private static String generateKey() {
        StringBuilder key = new StringBuilder(6);
        for (int i = 0; i < 6; i++) {
            // threadlocalrandom is better for concurrent stuff
            int index = ThreadLocalRandom.current().nextInt(alph.length());
            key.append(alph.charAt(index));
        }

        return key.toString();
    }

    private static void sendResponse(OutputStream out,
                                     String statusCode,
                                     String reasonPhrase,
                                     String body)
            throws IOException {
        String contentType = "text/plain";
        byte[] bodyBytes = body.getBytes(StandardCharsets.UTF_8);
        int contentLength = bodyBytes.length;

        String responseHeaders = "HTTP/1.1 " + statusCode + " " + reasonPhrase +
                "\r\nContent-Type: " + contentType + "; charset=UTF-8" +
                "\r\nContent-Length: " + contentLength +
                "\r\nConnection: close" +
                "\r\n\r\n";

        out.write(responseHeaders.getBytes(StandardCharsets.UTF_8));
        out.write(bodyBytes);
        out.flush();
    }

    private static void sendRedirect(OutputStream out, String location) throws IOException{

        String response = "HTTP/1.1 302 Found\r\n" +
                "Location: " + location + "\r\n" +
                "Content-Length: 0\r\n" +
                "Connection: close\r\n\r\n";

        out.write(response.getBytes(StandardCharsets.UTF_8));
        out.flush();
    }
}
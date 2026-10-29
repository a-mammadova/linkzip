private static final int PORT = 8080;
private static final String BASE_URL = "http://localhost:" + PORT + "/";

private static final Map<String, String> urlMap = new HashMap<>();
private static final String alphabet = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
private static final Random random = new Random();

void main() {
    try (ServerSocket serverSocket = new ServerSocket(PORT)) {
        IO.println("linkzip started on " + BASE_URL);

        while (true) {
            // accept incoming TCP socket connection
            Socket clientSocket = serverSocket.accept();

            // handle each request separately
            new Thread(() -> handleClient(clientSocket)).start();
        }
    } catch (IOException e) {
        System.err.println("server error: " + e.getMessage());
    }
}

private static void handleClient(Socket socket) {
    try (socket;
         BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
         OutputStream out = socket.getOutputStream()
    ) {
        try {
            // parse request
            String request = in.readLine();
            if (request == null || request.isEmpty()) {
                return;
            }

            String[] parts = request.split(" ");
            String method = parts[0];
            String path = parts[1];

            // find content-length
            int contentLength = 0;
            String header;
            while (!(header = in.readLine()).isEmpty()) {
                if (header.toLowerCase().startsWith("content-length:")) {
                    contentLength = Integer.parseInt(header.substring(15).trim());
                }
            }

            // routes
            if (method.equals("POST") && path.equals("/zip")) {

                char[] bodyChars = new char[contentLength];
                in.read(bodyChars, 0, contentLength);
                String longUrl = new String(bodyChars).trim();

                if (longUrl.isEmpty()) {
                    sendResponse(out, "HTTP/1.1 400 Bad Request", "missing url");
                    return;
                }

                String key = generateShortKey();
                urlMap.put(key, longUrl);
                String shortUrl = BASE_URL + key;

                sendResponse(out, "HTTP/1.1 201 Created", shortUrl);

            } else if (method.equals("GET") && path.length() == 7 && path.startsWith("/")) {
                String key = path.substring(1); // just getting rid of the first '/'
                String longUrl = urlMap.get(key);

                if (longUrl != null) {
                    String redirectResponse = "HTTP/1.1 302 Found\r\n" +
                            "Location: " + longUrl + "\r\n" +
                            "Content-Length: 0\r\n\r\n";
                    out.write(redirectResponse.getBytes(StandardCharsets.UTF_8));
                    out.flush();
                } else {
                    sendResponse(out, "HTTP/1.1 404 Not Found", "url not found");
                }
            } else {
                sendResponse(out, "HTTP/1.1 404 Not Found", "use POST /zip or GET /{key}");
            }
        } catch (Exception e) {
            System.err.println("error handling client connection: " + e.getMessage());
        }
    } catch (IOException ignored) {
    }
}

private static void sendResponse(OutputStream out, String status, String body) throws IOException {
    String response = status + "\r\n" + "Content-Type: " + "text/plain" + "\r\n" +
            "Content-Length: " + body.length() + "\r\n" + "\r\n" + body;
    out.write(response.getBytes(StandardCharsets.UTF_8));
    out.flush();
}

private static String generateShortKey() {
    StringBuilder sb = new StringBuilder(6);
    for (int i = 0; i < 6; i++) {
        sb.append(alphabet.charAt(random.nextInt(alphabet.length())));
    }
    return sb.toString();
}

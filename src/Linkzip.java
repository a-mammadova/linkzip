private static final int PORT = 8080;
private static final String BASE_URL = "http://localhost:" + PORT + "/";

private static final Map<String, String> urls = new ConcurrentHashMap<>();
private static final String alph = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";

void main() {

    // closes socket auto
    try (ServerSocket serverSocket = new ServerSocket(PORT)) {
        System.out.println("linkzip on: " + BASE_URL);

        while (true) {
            Socket clientSocket = serverSocket.accept(); // accept tcp conn
            new Thread(() -> {
                try {
                    handleClient(clientSocket);
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }).start();
        }

    } catch(IOException e) {
        System.err.println("server error, bro ->" + e.getMessage());
    }
}

private static void handleClient(Socket socket) throws IOException {
    try (socket;
    BufferedReader reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
    OutputStream out = socket.getOutputStream();
    ) {
        try {
            String request = reader.readLine();
            if (request == null || request.isEmpty()) { return; }

            System.out.println("just checking what is request ->\n" + request + "\n"); // DELETE

            String[] requestParts = request.split(" "); // parsin request
            String method = requestParts[0];
            String path = requestParts[1];

            System.out.printf("request: %s, path: %s\n", method, path); // DELETE

            int contentLength = 0;
            String header;

            while ((header = reader.readLine()) != null && !header.isEmpty()) {
                if (header.toLowerCase().startsWith("content-length")) {
                    // 15 is where con-len finishes, we trim spaces that surround num and convert to int
                    contentLength = Integer.parseInt(header.substring(15).trim());
                }
            }

            /*
            char[] bodyChars = new char[contentLength];
            int totalRead = 0;

            while (totalRead < contentLength) {
                int count = reader.read(bodyChars, totalRead, contentLength - totalRead);

                if (count == -1) { throw new IOException("unexpected end of request body");}
                totalRead += count;
            } */

            if (method.equals("POST") && path.equals("/zip")) {
                char[] bodyChars = new char[contentLength];
                reader.read(bodyChars, 0, contentLength);
                String longUrl = new String(bodyChars).trim();

                if (longUrl.isEmpty()) {
                    sendResponse(out, "400", "bad request", "missing url");
                    return;
                }

                String key;

                do {
                    key = generateKey();
                } while (urls.putIfAbsent(key, longUrl) != null);

                String shortUlr = BASE_URL + key;

                sendResponse(out, "201", "Created", "created");
            } else if (method.equals("GET") && path.length() == 7 && path.startsWith("/")) {
                String key = path.substring(1); // getting rid of /
                String longUrl = urls.get(key);

                if (longUrl != null) {
                    String redirectResponse = "302 found\n" + "Location: " + longUrl;
                    out.write(redirectResponse.getBytes(StandardCharsets.UTF_8));
                    out.flush();
                } else {
                    sendResponse(out, "404", "not found", "");
                }
            } else {
                sendResponse(out, "404", "not found", "use POST /zip or GET /{key}");
            }
        } catch (Exception e) {
            System.err.println("some error in handleClient first catch -> " + e.getMessage());
        }
    } catch (Exception _) {

    }
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

private static void sendResponse(OutputStream out, String statusCode, String reasonPhrase, String body) throws IOException {
    String httpVersion = "HTTP/1.1 ";
    String contentType = "text/plain";
    byte[] bodyBytes = body.getBytes(StandardCharsets.UTF_8);
    int contentLength = bodyBytes.length;

    String responseHeaders = httpVersion + statusCode + " " + reasonPhrase +
            "\r\nContent-Type: " + contentType + "; charset=UTF-8" +
            "\r\nContent-Length: " + contentLength +
            "\r\nConnection: close" +
            "\r\n\r\n";

    out.write(responseHeaders.getBytes(StandardCharsets.UTF_8));
    out.write(bodyBytes);
    out.flush();
}


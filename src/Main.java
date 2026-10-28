import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;

public class Main {
    public static void main() throws IOException {

        // first, we'll create http server that listens to 8080
        HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 8080), 0);

        server.createContext("/hello", exchange -> {

            // response body
            String response = "hello motherfuckers\n";

            // convert the response from string to utf-8 bytes
            byte[] responseBytes = response.getBytes(StandardCharsets.UTF_8);


            // i = status code
            exchange.sendResponseHeaders(200, responseBytes.length);

            // send it back to the server
            try (OutputStream output = exchange.getResponseBody()) {
                output.write(responseBytes);
            }
        });

        server.createContext("/api/urls", exchange -> {

            // 1. Inspect the request method and URI.
            String method = exchange.getRequestMethod();
            String path = exchange.getRequestURI().getPath();

            // HttpServer contexts use prefix matching, so verify the exact path.
            if (!path.equals("/api/urls")) {
                sendText(exchange, 404, "Not Found");
                return;
            }

            if (!method.equals("POST")) {
                exchange.getResponseHeaders().set("Allow", "POST");
                sendText(exchange, 405, "Method Not Allowed");
                return;
            }

            String contentType =
                    exchange.getRequestHeaders().getFirst("Content-Type");

            if (contentType == null
                    || !contentType.split(";", 2)[0].trim()
                    .equalsIgnoreCase("application/x-www-form-urlencoded")) {
                sendText(exchange, 415, "Unsupported Media Type");
                return;
            }

            String requestBody;

            try (var input = exchange.getRequestBody()) {
                byte[] bodyBytes = input.readAllBytes();
                requestBody = new String(bodyBytes, StandardCharsets.UTF_8);
            }

            System.out.println("Method: " + method);
            System.out.println("Path: " + path);
            System.out.println("Content-Type: " + contentType);
            System.out.println("Raw body: " + requestBody);

            sendText(exchange, 200, "Request received successfully.");
        });

        server.start();

        System.out.println("server is running at http://127.0.0.1:8080");
    }

    private static void sendText(
            HttpExchange exchange,
            int statusCode,
            String responseBody
    ) throws IOException {

        byte[] responseBytes =
                responseBody.getBytes(StandardCharsets.UTF_8);

        exchange.getResponseHeaders().set(
                "Content-Type",
                "text/plain; charset=UTF-8"
        );

        exchange.sendResponseHeaders(statusCode, responseBytes.length);

        try (OutputStream output = exchange.getResponseBody()) {
            output.write(responseBytes);
        }
    }
}
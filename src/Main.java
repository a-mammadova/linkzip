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
            String response = "hello motherfuckers";

            // convert the response from string to utf-8 bytes
            byte[] responseBytes = response.getBytes(StandardCharsets.UTF_8);


            // i = status code
            exchange.sendResponseHeaders(200, responseBytes.length);

            // send it back to the server
            try (OutputStream output = exchange.getResponseBody()) {
                output.write(responseBytes);
            }
        });

        server.start();

        System.out.println("server is running at http://127.0.0.1:8080");
    }
}
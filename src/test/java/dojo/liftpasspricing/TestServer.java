package dojo.liftpasspricing;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

import spark.Spark;

/**
 * O Spark guarda o servidor em estado estático, então todas as classes de teste
 * usam a mesma aplicação, iniciada uma única vez.
 */
class TestServer {

    private static final HttpClient CLIENT = HttpClient.newHttpClient();
    private static boolean started;

    static synchronized void start() throws Exception {
        if (started) {
            return;
        }
        Prices.createApp();
        Spark.awaitInitialization();
        started = true;
    }

    static HttpResponse<String> get(String pathAndQuery) throws IOException, InterruptedException {
        return send(HttpRequest.newBuilder(uri(pathAndQuery)).GET().build());
    }

    static HttpResponse<String> put(String pathAndQuery) throws IOException, InterruptedException {
        return send(HttpRequest.newBuilder(uri(pathAndQuery)).PUT(HttpRequest.BodyPublishers.noBody()).build());
    }

    private static URI uri(String pathAndQuery) {
        return URI.create("http://localhost:4567" + pathAndQuery);
    }

    private static HttpResponse<String> send(HttpRequest request) throws IOException, InterruptedException {
        return CLIENT.send(request, HttpResponse.BodyHandlers.ofString());
    }
}

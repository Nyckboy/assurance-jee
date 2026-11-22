package ma.sinistrepro.gateway;

import static org.assertj.core.api.Assertions.assertThat;

import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

/**
 * The gateway must forward each request exactly once: retrying is the job of the caller
 * or of the service, and re-sending a POST could create a resource twice.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class GatewayForwardingTests {

    private static final AtomicInteger hits = new AtomicInteger();
    private static final HttpServer backend = startBackend();

    @LocalServerPort
    private int gatewayPort;

    @DynamicPropertySource
    static void route(DynamicPropertyRegistry registry) {
        registry.add("spring.cloud.gateway.server.webmvc.routes[0].id", () -> "backend");
        registry.add("spring.cloud.gateway.server.webmvc.routes[0].uri",
                () -> "http://localhost:" + backend.getAddress().getPort());
        registry.add("spring.cloud.gateway.server.webmvc.routes[0].predicates[0]", () -> "Path=/backend/**");
    }

    @BeforeEach
    void reset() {
        hits.set(0);
    }

    @AfterAll
    static void stopBackend() {
        backend.stop(0);
    }

    // A POST without a body, like POST /api/indemnities/calculate/{claimId}.
    @Test
    void bodylessPostAnsweredWith503IsForwardedOnce() throws Exception {
        int status = send(HttpRequest.newBuilder(backendUri("/backend/indemnities/calculate/12"))
                .POST(HttpRequest.BodyPublishers.noBody()));

        assertThat(status).isEqualTo(503);
        assertThat(hits).as("requests received by the service").hasValue(1);
    }

    @Test
    void getAnsweredWith503IsForwardedOnce() throws Exception {
        int status = send(HttpRequest.newBuilder(backendUri("/backend/indemnities")).GET());

        assertThat(status).isEqualTo(503);
        assertThat(hits).as("requests received by the service").hasValue(1);
    }

    private URI backendUri(String path) {
        return URI.create("http://localhost:" + gatewayPort + path);
    }

    private static int send(HttpRequest.Builder request) throws Exception {
        return HttpClient.newHttpClient()
                .send(request.build(), HttpResponse.BodyHandlers.discarding())
                .statusCode();
    }

    private static HttpServer startBackend() {
        try {
            HttpServer server = HttpServer.create(new InetSocketAddress("localhost", 0), 0);
            server.createContext("/", exchange -> {
                hits.incrementAndGet();
                exchange.sendResponseHeaders(503, -1);
                exchange.close();
            });
            server.start();
            return server;
        } catch (IOException exception) {
            throw new IllegalStateException(exception);
        }
    }
}

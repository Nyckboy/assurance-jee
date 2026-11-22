package ma.sinistrepro.indemnities.client;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import feign.FeignException;
import io.github.resilience4j.circuitbreaker.CallNotPermittedException;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;
import ma.sinistrepro.indemnities.exception.RemoteServiceUnavailableException;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

/**
 * Checks Retry, Circuit Breaker and timeouts on the real Feign clients against a local HTTP server
 * that plays service-sinistres and service-polices. The settings below are small versions of
 * config-repo/service-indemnites.properties, so the tests run fast.
 */
@SpringBootTest(properties = {
        "spring.kafka.listener.auto-startup=false",
        "spring.cloud.openfeign.circuitbreaker.enabled=true",
        "spring.cloud.openfeign.client.config.default.connect-timeout=500",
        "spring.cloud.openfeign.client.config.default.read-timeout=500",
        "resilience4j.timelimiter.configs.default.timeout-duration=3s",
        "resilience4j.circuitbreaker.configs.default.sliding-window-size=4",
        "resilience4j.circuitbreaker.configs.default.minimum-number-of-calls=4",
        "resilience4j.circuitbreaker.configs.default.failure-rate-threshold=50",
        "resilience4j.circuitbreaker.configs.default.wait-duration-in-open-state=60s",
        "resilience4j.circuitbreaker.configs.default.ignore-exceptions=feign.FeignException$FeignClientException",
        "resilience.retry.max-retries=2",
        "resilience.retry.delay=10ms"
})
class FeignResilienceTests {

    private static final String CLAIM_JSON = """
            {"id":12,"policyId":3,"souscripteurEmail":"insured@example.com","dateSinistre":"2026-11-01",
             "type":"COLLISION","montantDeclare":4200.00,"montantRetenu":4000.00,"statut":"EN_EXPERTISE"}
            """;

    private static final AtomicInteger hits = new AtomicInteger();
    private static volatile Responder responder;
    private static final HttpServer server = startServer();

    @Autowired
    private ClaimServiceClient claimClient;

    @Autowired
    private PolicyServiceClient policyClient;

    @Autowired
    private WebApplicationContext context;

    @Autowired
    private CircuitBreakerRegistry circuitBreakers;

    @DynamicPropertySource
    static void remoteServices(DynamicPropertyRegistry registry) {
        String url = "http://localhost:" + server.getAddress().getPort();
        registry.add("spring.cloud.openfeign.client.config.service-sinistres.url", () -> url);
        registry.add("spring.cloud.openfeign.client.config.service-polices.url", () -> url);
    }

    @BeforeEach
    void reset() {
        hits.set(0);
        // Each test starts with closed circuits.
        circuitBreakers.getAllCircuitBreakers().forEach(CircuitBreaker::reset);
    }

    @AfterAll
    static void stopServer() {
        server.stop(0);
    }

    @Test
    void serverErrorIsRetriedThenReportedAsUnavailable() {
        responder = exchange -> send(exchange, 500, "{}");

        assertThatThrownBy(() -> claimClient.getClaimById(12L))
                .isInstanceOf(RemoteServiceUnavailableException.class)
                .hasMessageContaining("service-sinistres");
        assertThat(hits).as("first attempt + 2 retries").hasValue(3);
    }

    @Test
    void temporaryErrorIsRecoveredByRetry() {
        responder = exchange -> {
            if (hits.get() == 1) {
                send(exchange, 500, "{}");
            } else {
                send(exchange, 200, CLAIM_JSON);
            }
        };

        ClaimResponse claim = claimClient.getClaimById(12L);

        assertThat(claim.montantRetenu()).isEqualByComparingTo("4000.00");
        assertThat(hits).hasValue(2);
    }

    @Test
    void notFoundIsABusinessAnswerAndIsNotRetried() {
        responder = exchange -> send(exchange, 404, "{}");

        assertThatThrownBy(() -> claimClient.getClaimById(12L))
                .isInstanceOf(FeignException.NotFound.class);
        assertThat(hits).hasValue(1);
    }

    @Test
    void slowServiceTimesOut() {
        responder = exchange -> {
            sleep(1500);
            send(exchange, 200, CLAIM_JSON);
        };

        long start = System.nanoTime();
        assertThatThrownBy(() -> claimClient.getClaimById(12L))
                .isInstanceOf(RemoteServiceUnavailableException.class);
        long elapsedMillis = (System.nanoTime() - start) / 1_000_000;

        assertThat(hits).hasValue(3);
        assertThat(elapsedMillis).as("each attempt is cut at the 500 ms read timeout").isLessThan(4000);
    }

    @Test
    void circuitOpensAfterRepeatedFailuresAndStopsCallingTheService() {
        responder = exchange -> send(exchange, 500, "{}");

        // Call 1: 3 failed attempts. Call 2: the 4th failure opens the circuit, its retries are rejected.
        for (int call = 0; call < 2; call++) {
            assertThatThrownBy(() -> policyClient.verifyCoverage(3L, "COLLISION"))
                    .isInstanceOf(RemoteServiceUnavailableException.class);
        }
        assertThat(hits).hasValue(4);

        // Circuit open: the service is not called any more.
        assertThatThrownBy(() -> policyClient.verifyCoverage(3L, "COLLISION"))
                .isInstanceOf(RemoteServiceUnavailableException.class)
                .hasCauseInstanceOf(CallNotPermittedException.class);
        assertThat(hits).hasValue(4);
    }

    @Test
    void calculationAnswers503WhenClaimsServiceIsDown() throws Exception {
        responder = exchange -> send(exchange, 503, "{}");
        MockMvc mockMvc = MockMvcBuilders.webAppContextSetup(context).build();

        mockMvc.perform(post("/api/indemnities/calculate/12"))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("service-sinistres")));
    }

    @FunctionalInterface
    private interface Responder {
        void respond(HttpExchange exchange) throws IOException;
    }

    private static HttpServer startServer() {
        try {
            HttpServer httpServer = HttpServer.create(new InetSocketAddress("localhost", 0), 0);
            httpServer.setExecutor(Executors.newCachedThreadPool());
            httpServer.createContext("/", exchange -> {
                hits.incrementAndGet();
                responder.respond(exchange);
            });
            httpServer.start();
            return httpServer;
        } catch (IOException exception) {
            throw new IllegalStateException(exception);
        }
    }

    private static void send(HttpExchange exchange, int status, String body) throws IOException {
        byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().add("Content-Type", "application/json");
        exchange.sendResponseHeaders(status, bytes.length);
        exchange.getResponseBody().write(bytes);
        exchange.close();
    }

    private static void sleep(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
        }
    }
}

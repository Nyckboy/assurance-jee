package ma.sinistrepro.gateway.config;

import org.apache.hc.client5.http.impl.classic.HttpClientBuilder;
import org.springframework.boot.http.client.ClientHttpRequestFactoryBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * HTTP client the gateway uses to forward requests to the services.
 *
 * <p>Apache HttpClient (on the classpath through the Eureka client) re-sends a request
 * automatically when the service answers 503 or 429, if the request has no body: GET, but
 * also POST /api/indemnities/calculate/{claimId}. A gateway must forward each request once:
 * re-sending a POST can create a resource twice, and the services already retry their own
 * calls (Retry + Circuit Breaker).
 */
@Configuration(proxyBeanMethods = false)
public class GatewayHttpClientConfig {

    @Bean
    ClientHttpRequestFactoryBuilder<?> clientHttpRequestFactoryBuilder() {
        return ClientHttpRequestFactoryBuilder.httpComponents()
                .withHttpClientCustomizer(HttpClientBuilder::disableAutomaticRetries);
    }
}

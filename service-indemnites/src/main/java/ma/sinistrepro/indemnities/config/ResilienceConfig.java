package ma.sinistrepro.indemnities.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.resilience.annotation.EnableResilientMethods;

// Active @Retryable (Spring Framework 7) sur les clients Feign.
// Le Circuit Breaker et le TimeLimiter (Resilience4j) sont activés par
// spring.cloud.openfeign.circuitbreaker.enabled dans config-repo/service-indemnites.properties.
@Configuration
@EnableResilientMethods
public class ResilienceConfig {
}

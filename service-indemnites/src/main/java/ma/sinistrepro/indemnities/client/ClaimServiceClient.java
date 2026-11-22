package ma.sinistrepro.indemnities.client;

import ma.sinistrepro.indemnities.exception.RemoteServiceUnavailableException;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.resilience.annotation.Retryable;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

// Résilience, de l'extérieur vers l'intérieur :
// Retry (@Retryable) -> Circuit Breaker + TimeLimiter (Resilience4j) -> Feign (timeouts connexion/lecture).
@FeignClient(name = "service-sinistres", fallbackFactory = ClaimServiceClientFallbackFactory.class)
public interface ClaimServiceClient {

    @Retryable(
            includes = RemoteServiceUnavailableException.class,
            maxRetriesString = "${resilience.retry.max-retries:2}",
            delayString = "${resilience.retry.delay:500ms}",
            multiplierString = "${resilience.retry.multiplier:2}"
    )
    @GetMapping("/api/claims/{id}")
    ClaimResponse getClaimById(@PathVariable("id") Long id);
}

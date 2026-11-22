package ma.sinistrepro.indemnities.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(
        name = "service-sinistres",
        url = "${services.sinistres.url}"
)
public interface ClaimServiceClient {

    @GetMapping("/api/claims/{id}")
    ClaimResponse getClaimById(@PathVariable("id") Long id);
}
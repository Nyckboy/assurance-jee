package ma.sinistrepro.claims.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

@FeignClient(name = "service-polices")
public interface PolicyClient {

    // Resolve service-polices through Eureka instead of a fixed URL.
    @GetMapping("/api/policies/{policyId}/coverage")
    CoverageResponse verifyCoverage(
            @PathVariable("policyId") Long policyId,
            @RequestParam("claimType") String claimType
    );
}
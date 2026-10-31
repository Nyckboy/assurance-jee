package ma.sinistrepro.policies.api;

import java.math.BigDecimal;

public record CoverageVerificationResponse(
        Long policyId,
        String claimType,
        boolean covered,
        String reason,
        BigDecimal plafond,
        BigDecimal franchise
) {
}

package ma.emsi.indemnites.client;

import java.math.BigDecimal;

public record CoverageResponse(
        Long policyId,
        String claimType,
        boolean covered,
        String reason,
        BigDecimal plafond,
        BigDecimal franchise
) {
}
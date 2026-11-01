
package ma.sinistrepro.claims.client;

import java.math.BigDecimal;

// Response received from service-polices.
public record CoverageResponse(
    Long policyId,
    String claimType,
    boolean covered,
    String reason,
    BigDecimal plafond,
    BigDecimal franchise
) {
}

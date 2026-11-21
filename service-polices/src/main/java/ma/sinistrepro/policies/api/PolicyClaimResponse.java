package ma.sinistrepro.policies.api;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

public record PolicyClaimResponse(
        Long claimId,
        LocalDate dateSinistre,
        String type,
        BigDecimal montantDeclare,
        Instant receivedAt
) {
}

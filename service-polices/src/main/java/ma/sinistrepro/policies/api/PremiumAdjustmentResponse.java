package ma.sinistrepro.policies.api;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

public record PremiumAdjustmentResponse(
        Long claimId,
        Long indemnityId,
        BigDecimal montantIndemnise,
        LocalDate dateVirement,
        BigDecimal primeAvant,
        BigDecimal primeApres,
        Instant appliedAt) {
}

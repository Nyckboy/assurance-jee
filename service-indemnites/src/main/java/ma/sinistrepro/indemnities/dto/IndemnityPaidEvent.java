package ma.sinistrepro.indemnities.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

public record IndemnityPaidEvent(
        Long indemnityId,
        Long claimId,
        Long policyId,
        BigDecimal montantCalcule,
        LocalDate dateVirement
) {
}
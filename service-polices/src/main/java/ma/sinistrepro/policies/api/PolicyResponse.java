package ma.sinistrepro.policies.api;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import ma.sinistrepro.policies.domain.PolicyStatus;

public record PolicyResponse(
        Long id,
        String souscripteurEmail,
        String typeCouverture,
        BigDecimal montantCouvert,
        BigDecimal prime,
        LocalDate dateEffet,
        PolicyStatus statut,
        List<GuaranteeResponse> guarantees
) {
}

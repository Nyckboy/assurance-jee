package ma.sinistrepro.claims.api;

import java.math.BigDecimal;
import java.time.LocalDate;
import ma.sinistrepro.claims.domain.ClaimFile;
import ma.sinistrepro.claims.domain.ClaimStatus;

// Contract read by service-indemnites (Feign, GET /api/claims/{id}): keep the field names.
public record ClaimResponse(
        Long id,
        Long policyId,
        String souscripteurEmail,
        LocalDate dateSinistre,
        String type,
        BigDecimal montantDeclare,
        BigDecimal montantRetenu,
        ClaimStatus statut) {

    static ClaimResponse from(ClaimFile claim) {
        return new ClaimResponse(claim.getId(), claim.getPolicyId(), claim.getSouscripteurEmail(),
                claim.getDateSinistre(), claim.getType(), claim.getMontantDeclare(), claim.getMontantRetenu(),
                claim.getStatut());
    }
}

package ma.emsi.indemnites.client;

import java.math.BigDecimal;
import java.time.LocalDate;

public record ClaimResponse(
        Long id,
        Long policyId,
        String souscripteurEmail,
        LocalDate dateSinistre,
        String type,
        BigDecimal montantDeclare,
        BigDecimal montantRetenu,
        String statut
) {
}
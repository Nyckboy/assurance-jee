package ma.sinistrepro.claims.api;

import java.math.BigDecimal;
import java.time.LocalDate;
import ma.sinistrepro.claims.domain.Expertise;

public record ExpertiseResponse(
        Long id,
        Long claimId,
        String expertNom,
        LocalDate date,
        BigDecimal montantEstime,
        String avis) {

    static ExpertiseResponse from(Expertise expertise) {
        return new ExpertiseResponse(expertise.getId(), expertise.getClaimId(), expertise.getExpertNom(),
                expertise.getDate(), expertise.getMontantEstime(), expertise.getAvis());
    }
}

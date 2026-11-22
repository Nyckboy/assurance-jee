package ma.sinistrepro.claims.api;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.LocalDate;
import ma.sinistrepro.claims.domain.Expertise;

// Body of POST /api/claims/{claimId}/expertises. The claim comes from the URL.
public record ExpertiseRequest(
        @NotBlank String expertNom,
        @NotNull @PastOrPresent LocalDate date,
        @NotNull @DecimalMin(value = "0.0") BigDecimal montantEstime,
        @NotBlank @Size(max = 2000) String avis) {

    Expertise toExpertise(Long claimId) {
        Expertise expertise = new Expertise();
        expertise.setClaimId(claimId);
        expertise.setExpertNom(expertNom);
        expertise.setDate(date);
        expertise.setMontantEstime(montantEstime);
        expertise.setAvis(avis);
        return expertise;
    }
}

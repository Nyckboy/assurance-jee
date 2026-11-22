package ma.sinistrepro.claims.api;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Positive;
import java.math.BigDecimal;
import java.time.LocalDate;
import ma.sinistrepro.claims.domain.ClaimFile;

// Body of POST /api/claims. The id, status and retained amount are set by the service, never by the caller.
public record ClaimRequest(
        @NotNull @Positive Long policyId,
        @NotBlank @Email String souscripteurEmail,
        @NotNull @PastOrPresent LocalDate dateSinistre,
        @NotBlank String type,
        @NotNull @DecimalMin(value = "0.0", inclusive = false) BigDecimal montantDeclare) {

    ClaimFile toClaimFile() {
        ClaimFile claim = new ClaimFile();
        claim.setPolicyId(policyId);
        claim.setSouscripteurEmail(souscripteurEmail);
        claim.setDateSinistre(dateSinistre);
        claim.setType(type);
        claim.setMontantDeclare(montantDeclare);
        return claim;
    }
}

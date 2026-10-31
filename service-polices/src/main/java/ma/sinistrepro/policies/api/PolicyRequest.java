package ma.sinistrepro.policies.api;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.LocalDate;
import ma.sinistrepro.policies.domain.PolicyStatus;

public record PolicyRequest(
        @Email @NotBlank String souscripteurEmail,
        @NotBlank String typeCouverture,
        @NotNull @DecimalMin(value = "0.0", inclusive = false) BigDecimal montantCouvert,
        @NotNull @DecimalMin(value = "0.0", inclusive = true) BigDecimal prime,
        @NotNull LocalDate dateEffet,
        @NotNull PolicyStatus statut
) {
}

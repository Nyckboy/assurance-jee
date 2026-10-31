package ma.sinistrepro.policies.api;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

public record GuaranteeRequest(
        @NotBlank String libelle,
        @NotNull @DecimalMin(value = "0.0", inclusive = false) BigDecimal plafond,
        @NotNull @DecimalMin(value = "0.0", inclusive = true) BigDecimal franchise
) {
}

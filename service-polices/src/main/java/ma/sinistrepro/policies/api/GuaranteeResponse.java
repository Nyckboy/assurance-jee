package ma.sinistrepro.policies.api;

import java.math.BigDecimal;

public record GuaranteeResponse(Long id, String libelle, BigDecimal plafond, BigDecimal franchise) {
}

package ma.emsi.indemnites.service;

import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.Objects;

@Component
public class IndemnityCalculator {

    public BigDecimal calculate(
            BigDecimal montantRetenu,
            BigDecimal plafond,
            BigDecimal franchise) {

        Objects.requireNonNull(montantRetenu, "Le montant retenu est obligatoire");
        Objects.requireNonNull(plafond, "Le plafond est obligatoire");
        Objects.requireNonNull(franchise, "La franchise est obligatoire");

        if (montantRetenu.compareTo(BigDecimal.ZERO) < 0
                || plafond.compareTo(BigDecimal.ZERO) < 0
                || franchise.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException(
                    "Les montants ne peuvent pas être négatifs."
            );
        }

        BigDecimal montantPlafonne = montantRetenu.min(plafond);
        BigDecimal indemnity = montantPlafonne.subtract(franchise);

        return indemnity.max(BigDecimal.ZERO);
    }
}
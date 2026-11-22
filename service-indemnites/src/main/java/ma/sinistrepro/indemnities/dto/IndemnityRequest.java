package ma.sinistrepro.indemnities.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;

public class IndemnityRequest {

    @NotNull(message = "L'identifiant du sinistre est obligatoire")
    @Positive(message = "L'identifiant du sinistre doit être positif")
    private Long claimId;

    @NotNull(message = "Le montant calcule est obligatoire")
    @DecimalMin(value = "0.0", inclusive = false,
            message = "Le montant calcule doit être supérieur à zéro")
    private BigDecimal montantCalcule;

    @NotNull(message = "La franchise appliquee est obligatoire")
    @DecimalMin(value = "0.0", message = "La franchise ne peut pas être négative")
    private BigDecimal franchiseAppliquee;

    public IndemnityRequest() {
    }

    public Long getClaimId() {
        return claimId;
    }

    public void setClaimId(Long claimId) {
        this.claimId = claimId;
    }

    public BigDecimal getMontantCalcule() {
        return montantCalcule;
    }

    public void setMontantCalcule(BigDecimal montantCalcule) {
        this.montantCalcule = montantCalcule;
    }

    public BigDecimal getFranchiseAppliquee() {
        return franchiseAppliquee;
    }

    public void setFranchiseAppliquee(BigDecimal franchiseAppliquee) {
        this.franchiseAppliquee = franchiseAppliquee;
    }
}

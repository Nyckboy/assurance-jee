package ma.emsi.indemnites.dto;

import ma.emsi.indemnites.enums.IndemnityStatus;

import java.math.BigDecimal;
import java.time.LocalDate;

public class IndemnityResponse {

    private Long id;
    private Long claimId;
    private BigDecimal montantCalcule;
    private BigDecimal franchiseAppliquee;
    private LocalDate dateVirement;
    private IndemnityStatus statut;

    public IndemnityResponse() {
    }

    public IndemnityResponse(Long id, Long claimId,
                             BigDecimal montantCalcule,
                             BigDecimal franchiseAppliquee,
                             LocalDate dateVirement,
                             IndemnityStatus statut) {
        this.id = id;
        this.claimId = claimId;
        this.montantCalcule = montantCalcule;
        this.franchiseAppliquee = franchiseAppliquee;
        this.dateVirement = dateVirement;
        this.statut = statut;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
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

    public LocalDate getDateVirement() {
        return dateVirement;
    }

    public void setDateVirement(LocalDate dateVirement) {
        this.dateVirement = dateVirement;
    }

    public IndemnityStatus getStatut() {
        return statut;
    }

    public void setStatut(IndemnityStatus statut) {
        this.statut = statut;
    }
}

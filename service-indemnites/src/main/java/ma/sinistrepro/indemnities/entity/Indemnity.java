package ma.sinistrepro.indemnities.entity;

import ma.sinistrepro.indemnities.enums.IndemnityStatus;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(
    name = "indemnities",
    uniqueConstraints = {
        @UniqueConstraint(
            name = "uk_indemnity_claim",
            columnNames = "claim_id"
        )
    }
)
public class Indemnity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "claim_id", nullable = false, unique = true)
    private Long claimId;

    @Column(name = "montant_calcule", nullable = false, precision = 12, scale = 2)
    private BigDecimal montantCalcule;

    @Column(name = "franchise_appliquee", nullable = false, precision = 12, scale = 2)
    private BigDecimal franchiseAppliquee;

    @Column(name = "date_virement")
    private LocalDate dateVirement;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private IndemnityStatus statut;

    public Indemnity() {
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


package ma.sinistrepro.claims.domain;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "claim_files")
public class ClaimFile {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long policyId;

    @Column(nullable = false)
    private String souscripteurEmail;

    @Column(nullable = false)
    private LocalDate dateSinistre;

    @Column(nullable = false)
    private String type;

    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal montantDeclare;

    @Column(precision = 15, scale = 2)
    private BigDecimal montantRetenu;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ClaimStatus statut = ClaimStatus.DECLARE;

    public ClaimFile() {
    }

    public Long getId() {
        return id;
    }

    public Long getPolicyId() {
        return policyId;
    }

    public void setPolicyId(Long policyId) {
        this.policyId = policyId;
    }

    public String getSouscripteurEmail() {
        return souscripteurEmail;
    }

    public void setSouscripteurEmail(String souscripteurEmail) {
        this.souscripteurEmail = souscripteurEmail;
    }

    public LocalDate getDateSinistre() {
        return dateSinistre;
    }

    public void setDateSinistre(LocalDate dateSinistre) {
        this.dateSinistre = dateSinistre;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public BigDecimal getMontantDeclare() {
        return montantDeclare;
    }

    public void setMontantDeclare(BigDecimal montantDeclare) {
        this.montantDeclare = montantDeclare;
    }

    public BigDecimal getMontantRetenu() {
        return montantRetenu;
    }

    public void setMontantRetenu(BigDecimal montantRetenu) {
        this.montantRetenu = montantRetenu;
    }

    public ClaimStatus getStatut() {
        return statut;
    }

    public void setStatut(ClaimStatus statut) {
        this.statut = statut;
    }
}

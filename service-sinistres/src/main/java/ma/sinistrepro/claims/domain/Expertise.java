
package ma.sinistrepro.claims.domain;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "expertises")
public class Expertise {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long claimId;

    @Column(nullable = false)
    private String expertNom;

    @Column(nullable = false)
    private LocalDate date;

    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal montantEstime;

    @Column(nullable = false, length = 2000)
    private String avis;

    public Expertise() {
    }

    public Long getId() {
        return id;
    }

    public Long getClaimId() {
        return claimId;
    }

    public void setClaimId(Long claimId) {
        this.claimId = claimId;
    }

    public String getExpertNom() {
        return expertNom;
    }

    public void setExpertNom(String expertNom) {
        this.expertNom = expertNom;
    }

    public LocalDate getDate() {
        return date;
    }

    public void setDate(LocalDate date) {
        this.date = date;
    }

    public BigDecimal getMontantEstime() {
        return montantEstime;
    }

    public void setMontantEstime(BigDecimal montantEstime) {
        this.montantEstime = montantEstime;
    }

    public String getAvis() {
        return avis;
    }

    public void setAvis(String avis) {
        this.avis = avis;
    }
}

package ma.sinistrepro.policies.domain;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "policies")
public class Policy {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String souscripteurEmail;

    @Column(nullable = false)
    private String typeCouverture;

    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal montantCouvert;

    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal prime;

    @Column(nullable = false)
    private LocalDate dateEffet;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PolicyStatus statut;

    @OneToMany(mappedBy = "policy", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Guarantee> guarantees = new ArrayList<>();

    protected Policy() {
    }

    public Policy(String souscripteurEmail, String typeCouverture, BigDecimal montantCouvert,
                  BigDecimal prime, LocalDate dateEffet, PolicyStatus statut) {
        this.souscripteurEmail = souscripteurEmail;
        this.typeCouverture = typeCouverture;
        this.montantCouvert = montantCouvert;
        this.prime = prime;
        this.dateEffet = dateEffet;
        this.statut = statut;
    }

    public Long getId() {
        return id;
    }

    public String getSouscripteurEmail() {
        return souscripteurEmail;
    }

    public String getTypeCouverture() {
        return typeCouverture;
    }

    public BigDecimal getMontantCouvert() {
        return montantCouvert;
    }

    public BigDecimal getPrime() {
        return prime;
    }

    public LocalDate getDateEffet() {
        return dateEffet;
    }

    public PolicyStatus getStatut() {
        return statut;
    }

    public List<Guarantee> getGuarantees() {
        return guarantees;
    }

    public void update(String souscripteurEmail, String typeCouverture, BigDecimal montantCouvert,
                       BigDecimal prime, LocalDate dateEffet, PolicyStatus statut) {
        this.souscripteurEmail = souscripteurEmail;
        this.typeCouverture = typeCouverture;
        this.montantCouvert = montantCouvert;
        this.prime = prime;
        this.dateEffet = dateEffet;
        this.statut = statut;
    }

    /**
     * Raises the premium by a rate (0.10 = +10%), rounded to the cent, and returns the new premium.
     */
    public BigDecimal increasePrime(BigDecimal rate) {
        this.prime = prime.multiply(BigDecimal.ONE.add(rate)).setScale(2, RoundingMode.HALF_UP);
        return prime;
    }

    public void addGuarantee(Guarantee guarantee) {
        guarantees.add(guarantee);
        guarantee.setPolicy(this);
    }

    public void removeGuarantee(Guarantee guarantee) {
        guarantees.remove(guarantee);
        guarantee.setPolicy(null);
    }
}

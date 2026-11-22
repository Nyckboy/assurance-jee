package ma.sinistrepro.policies.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

/**
 * A premium increase applied to a policy after an indemnity was paid (indemnity-paid event of
 * service-indemnites). One row per claim: it is both the audit trail and the duplicate guard.
 */
@Entity
@Table(name = "premium_adjustments")
public class PremiumAdjustment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private Long claimId;

    @Column(nullable = false)
    private Long indemnityId;

    @Column(nullable = false)
    private Long policyId;

    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal montantIndemnise;

    private LocalDate dateVirement;

    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal primeAvant;

    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal primeApres;

    @Column(nullable = false)
    private Instant appliedAt;

    protected PremiumAdjustment() {
    }

    public PremiumAdjustment(Long claimId, Long indemnityId, Long policyId, BigDecimal montantIndemnise,
                             LocalDate dateVirement, BigDecimal primeAvant, BigDecimal primeApres, Instant appliedAt) {
        this.claimId = claimId;
        this.indemnityId = indemnityId;
        this.policyId = policyId;
        this.montantIndemnise = montantIndemnise;
        this.dateVirement = dateVirement;
        this.primeAvant = primeAvant;
        this.primeApres = primeApres;
        this.appliedAt = appliedAt;
    }

    public Long getId() {
        return id;
    }

    public Long getClaimId() {
        return claimId;
    }

    public Long getIndemnityId() {
        return indemnityId;
    }

    public Long getPolicyId() {
        return policyId;
    }

    public BigDecimal getMontantIndemnise() {
        return montantIndemnise;
    }

    public LocalDate getDateVirement() {
        return dateVirement;
    }

    public BigDecimal getPrimeAvant() {
        return primeAvant;
    }

    public BigDecimal getPrimeApres() {
        return primeApres;
    }

    public Instant getAppliedAt() {
        return appliedAt;
    }
}

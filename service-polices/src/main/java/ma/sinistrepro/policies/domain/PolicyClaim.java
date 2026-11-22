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
 * A claim declared on a policy, recorded from the claim-declared event of service-sinistres.
 * The claims service owns the claim itself; this is only the policy's own history of it.
 */
@Entity
@Table(name = "policy_claims")
public class PolicyClaim {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private Long claimId;

    @Column(nullable = false)
    private Long policyId;

    @Column(nullable = false)
    private String souscripteurEmail;

    @Column(nullable = false)
    private LocalDate dateSinistre;

    @Column(name = "claim_type", nullable = false)
    private String type;

    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal montantDeclare;

    @Column(nullable = false)
    private Instant receivedAt;

    protected PolicyClaim() {
    }

    public PolicyClaim(Long claimId, Long policyId, String souscripteurEmail, LocalDate dateSinistre, String type,
                       BigDecimal montantDeclare, Instant receivedAt) {
        this.claimId = claimId;
        this.policyId = policyId;
        this.souscripteurEmail = souscripteurEmail;
        this.dateSinistre = dateSinistre;
        this.type = type;
        this.montantDeclare = montantDeclare;
        this.receivedAt = receivedAt;
    }

    public Long getId() {
        return id;
    }

    public Long getClaimId() {
        return claimId;
    }

    public Long getPolicyId() {
        return policyId;
    }

    public String getSouscripteurEmail() {
        return souscripteurEmail;
    }

    public LocalDate getDateSinistre() {
        return dateSinistre;
    }

    public String getType() {
        return type;
    }

    public BigDecimal getMontantDeclare() {
        return montantDeclare;
    }

    public Instant getReceivedAt() {
        return receivedAt;
    }
}

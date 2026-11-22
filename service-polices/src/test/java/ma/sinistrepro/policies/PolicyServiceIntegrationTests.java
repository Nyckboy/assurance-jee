package ma.sinistrepro.policies;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.LocalDate;
import ma.sinistrepro.policies.api.CoverageVerificationResponse;
import ma.sinistrepro.policies.api.GuaranteeRequest;
import ma.sinistrepro.policies.api.PolicyRequest;
import ma.sinistrepro.policies.api.PolicyResponse;
import ma.sinistrepro.policies.domain.PolicyStatus;
import ma.sinistrepro.policies.event.ClaimDeclaredEvent;
import ma.sinistrepro.policies.event.IndemnityPaidEvent;
import ma.sinistrepro.policies.repository.PolicyClaimRepository;
import ma.sinistrepro.policies.repository.PolicyRepository;
import ma.sinistrepro.policies.repository.PremiumAdjustmentRepository;
import ma.sinistrepro.policies.service.PolicyService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
class PolicyServiceIntegrationTests {

    @Autowired
    private PolicyService policyService;

    @Autowired
    private PolicyRepository policyRepository;

    @Autowired
    private PolicyClaimRepository policyClaimRepository;

    @Autowired
    private PremiumAdjustmentRepository premiumAdjustmentRepository;

    @BeforeEach
    void clearDatabase() {
        policyClaimRepository.deleteAll();
        premiumAdjustmentRepository.deleteAll();
        policyRepository.deleteAll();
    }

    @Test
    void activePolicyWithMatchingGuaranteeIsCovered() {
        PolicyResponse policy = policyService.createPolicy(new PolicyRequest(
                "insured@example.com", "AUTO", new BigDecimal("50000.00"), new BigDecimal("600.00"),
                LocalDate.of(2026, 1, 1), PolicyStatus.ACTIVE));
        policyService.addGuarantee(policy.id(), new GuaranteeRequest(
                "COLLISION", new BigDecimal("15000.00"), new BigDecimal("500.00")));

        CoverageVerificationResponse result = policyService.verifyCoverage(policy.id(), "collision");

        assertThat(result.covered()).isTrue();
        assertThat(result.plafond()).isEqualByComparingTo("15000.00");
        assertThat(result.franchise()).isEqualByComparingTo("500.00");
    }

    @Test
    void inactivePolicyIsNotCovered() {
        PolicyResponse policy = policyService.createPolicy(new PolicyRequest(
                "insured@example.com", "AUTO", new BigDecimal("50000.00"), new BigDecimal("600.00"),
                LocalDate.of(2026, 1, 1), PolicyStatus.SUSPENDED));

        CoverageVerificationResponse result = policyService.verifyCoverage(policy.id(), "COLLISION");

        assertThat(result.covered()).isFalse();
        assertThat(result.reason()).isEqualTo("Policy is not active");
    }

    @Test
    void declaredClaimIsRecordedOnceOnItsPolicy() {
        PolicyResponse policy = createActivePolicy();
        ClaimDeclaredEvent event = new ClaimDeclaredEvent(12L, policy.id(), "insured@example.com",
                LocalDate.of(2026, 11, 1), "COLLISION", new BigDecimal("4200.00"));

        assertThat(policyService.recordDeclaredClaim(event)).isTrue();
        assertThat(policyService.recordDeclaredClaim(event)).isFalse();

        assertThat(policyService.findPolicyClaims(policy.id()))
                .singleElement()
                .satisfies(claim -> {
                    assertThat(claim.claimId()).isEqualTo(12L);
                    assertThat(claim.montantDeclare()).isEqualByComparingTo("4200.00");
                });
    }

    @Test
    void claimOnUnknownPolicyIsIgnored() {
        ClaimDeclaredEvent event = new ClaimDeclaredEvent(13L, 999_999L, "insured@example.com",
                LocalDate.of(2026, 11, 1), "COLLISION", new BigDecimal("4200.00"));

        assertThat(policyService.recordDeclaredClaim(event)).isFalse();
        assertThat(policyClaimRepository.count()).isZero();
    }

    @Test
    void paidIndemnityRaisesThePremiumByTenPercentOncePerClaim() {
        PolicyResponse policy = createActivePolicy();
        IndemnityPaidEvent event = new IndemnityPaidEvent(7L, 12L, policy.id(), new BigDecimal("3700.00"),
                LocalDate.of(2026, 11, 22));

        assertThat(policyService.applyIndemnityPaid(event)).isTrue();
        assertThat(policyService.applyIndemnityPaid(event)).as("redelivered event").isFalse();

        assertThat(policyService.findPolicy(policy.id()).prime()).isEqualByComparingTo("660.00");
        assertThat(policyService.findPremiumAdjustments(policy.id()))
                .singleElement()
                .satisfies(adjustment -> {
                    assertThat(adjustment.claimId()).isEqualTo(12L);
                    assertThat(adjustment.indemnityId()).isEqualTo(7L);
                    assertThat(adjustment.montantIndemnise()).isEqualByComparingTo("3700.00");
                    assertThat(adjustment.primeAvant()).isEqualByComparingTo("600.00");
                    assertThat(adjustment.primeApres()).isEqualByComparingTo("660.00");
                });
    }

    @Test
    void eachPaidClaimRaisesTheCurrentPremium() {
        PolicyResponse policy = createActivePolicy();

        policyService.applyIndemnityPaid(new IndemnityPaidEvent(7L, 12L, policy.id(), new BigDecimal("3700.00"),
                LocalDate.of(2026, 11, 22)));
        policyService.applyIndemnityPaid(new IndemnityPaidEvent(8L, 13L, policy.id(), new BigDecimal("900.00"),
                LocalDate.of(2026, 11, 23)));

        // 600.00 -> 660.00 -> 726.00
        assertThat(policyService.findPolicy(policy.id()).prime()).isEqualByComparingTo("726.00");
        assertThat(policyService.findPremiumAdjustments(policy.id())).hasSize(2);
    }

    @Test
    void indemnityOnUnknownPolicyIsIgnored() {
        IndemnityPaidEvent event = new IndemnityPaidEvent(7L, 12L, 999_999L, new BigDecimal("3700.00"),
                LocalDate.of(2026, 11, 22));

        assertThat(policyService.applyIndemnityPaid(event)).isFalse();
        assertThat(premiumAdjustmentRepository.count()).isZero();
    }

    private PolicyResponse createActivePolicy() {
        return policyService.createPolicy(new PolicyRequest(
                "insured@example.com", "AUTO", new BigDecimal("50000.00"), new BigDecimal("600.00"),
                LocalDate.of(2026, 1, 1), PolicyStatus.ACTIVE));
    }
}

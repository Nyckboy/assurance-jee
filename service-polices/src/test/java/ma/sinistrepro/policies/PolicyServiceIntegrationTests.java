package ma.sinistrepro.policies;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.LocalDate;
import ma.sinistrepro.policies.api.CoverageVerificationResponse;
import ma.sinistrepro.policies.api.GuaranteeRequest;
import ma.sinistrepro.policies.api.PolicyRequest;
import ma.sinistrepro.policies.api.PolicyResponse;
import ma.sinistrepro.policies.domain.PolicyStatus;
import ma.sinistrepro.policies.repository.PolicyRepository;
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

    @BeforeEach
    void clearDatabase() {
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
}

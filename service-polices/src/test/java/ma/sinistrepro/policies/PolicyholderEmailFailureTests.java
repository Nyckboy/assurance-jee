package ma.sinistrepro.policies;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.LocalDate;
import ma.sinistrepro.policies.api.PolicyRequest;
import ma.sinistrepro.policies.api.PolicyResponse;
import ma.sinistrepro.policies.domain.PolicyStatus;
import ma.sinistrepro.policies.event.ClaimDeclaredEvent;
import ma.sinistrepro.policies.event.IndemnityPaidEvent;
import ma.sinistrepro.policies.service.PolicyService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

/**
 * A mail server that is down must not undo or block the business change: the email failure is only logged.
 */
@SpringBootTest(properties = {
        "spring.mail.host=localhost",
        // Nothing listens on port 1: the connection is refused.
        "spring.mail.port=1"
})
class PolicyholderEmailFailureTests {

    @Autowired
    private PolicyService policyService;

    @Test
    void claimAndPremiumAreRecordedEvenWhenTheMailServerIsDown() {
        PolicyResponse policy = policyService.createPolicy(new PolicyRequest("insured@example.com", "AUTO",
                new BigDecimal("50000.00"), new BigDecimal("600.00"), LocalDate.of(2026, 1, 1),
                PolicyStatus.ACTIVE));

        boolean claimRecorded = policyService.recordDeclaredClaim(new ClaimDeclaredEvent(31L, policy.id(),
                "insured@example.com", LocalDate.of(2026, 11, 1), "COLLISION", new BigDecimal("4200.00")));
        boolean premiumApplied = policyService.applyIndemnityPaid(new IndemnityPaidEvent(9L, 31L, policy.id(),
                new BigDecimal("3700.00"), LocalDate.of(2026, 11, 22)));

        assertThat(claimRecorded).isTrue();
        assertThat(premiumApplied).isTrue();
        assertThat(policyService.findPolicyClaims(policy.id())).hasSize(1);
        assertThat(policyService.findPolicy(policy.id()).prime()).isEqualByComparingTo("660.00");
    }
}

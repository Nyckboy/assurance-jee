package ma.sinistrepro.policies;

import static org.assertj.core.api.Assertions.assertThat;

import com.icegreen.greenmail.util.GreenMail;
import com.icegreen.greenmail.util.ServerSetupTest;
import jakarta.mail.Message;
import jakarta.mail.internet.MimeMessage;
import java.math.BigDecimal;
import java.time.LocalDate;
import ma.sinistrepro.policies.api.PolicyRequest;
import ma.sinistrepro.policies.api.PolicyResponse;
import ma.sinistrepro.policies.domain.PolicyStatus;
import ma.sinistrepro.policies.event.ClaimDeclaredEvent;
import ma.sinistrepro.policies.event.IndemnityPaidEvent;
import ma.sinistrepro.policies.service.PolicyService;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

/**
 * Sends the policyholder emails to a real SMTP server (GreenMail) and reads them back.
 */
@SpringBootTest
class PolicyholderEmailTests {

    private static final GreenMail smtp = startSmtp();

    @Autowired
    private PolicyService policyService;

    @DynamicPropertySource
    static void mailServer(DynamicPropertyRegistry registry) {
        registry.add("spring.mail.host", () -> "localhost");
        registry.add("spring.mail.port", () -> smtp.getSmtp().getPort());
    }

    @BeforeEach
    void emptyInbox() throws Exception {
        smtp.purgeEmailFromAllMailboxes();
    }

    @AfterAll
    static void stopSmtp() {
        smtp.stop();
    }

    @Test
    void recordedClaimIsEmailedToThePolicyholder() throws Exception {
        PolicyResponse policy = createPolicy("claim.owner@example.com");

        policyService.recordDeclaredClaim(new ClaimDeclaredEvent(12L, policy.id(), "claim.owner@example.com",
                LocalDate.of(2026, 11, 1), "COLLISION", new BigDecimal("4200.00")));

        MimeMessage email = singleEmail();
        assertThat(email.getRecipients(Message.RecipientType.TO)).extracting(Object::toString)
                .containsExactly("claim.owner@example.com");
        assertThat(email.getFrom()[0].toString()).contains("no-reply@sinistre-pro.local");
        assertThat(email.getSubject()).isEqualTo("Sinistre n°12 enregistré sur votre police n°" + policy.id());
        assertThat(body(email)).contains("COLLISION", "01/11/2026", "4200.00");
    }

    @Test
    void redeliveredClaimSendsNoSecondEmail() throws Exception {
        PolicyResponse policy = createPolicy("once@example.com");
        ClaimDeclaredEvent event = new ClaimDeclaredEvent(13L, policy.id(), "once@example.com",
                LocalDate.of(2026, 11, 1), "COLLISION", new BigDecimal("4200.00"));

        policyService.recordDeclaredClaim(event);
        policyService.recordDeclaredClaim(event);

        singleEmail();
        Thread.sleep(500);
        assertThat(smtp.getReceivedMessages()).hasSize(1);
    }

    @Test
    void paidIndemnityEmailAnnouncesTheNewPremium() throws Exception {
        PolicyResponse policy = createPolicy("premium.owner@example.com");

        policyService.applyIndemnityPaid(new IndemnityPaidEvent(7L, 21L, policy.id(), new BigDecimal("3700.00"),
                LocalDate.of(2026, 11, 22)));

        MimeMessage email = singleEmail();
        assertThat(email.getRecipients(Message.RecipientType.TO)).extracting(Object::toString)
                .containsExactly("premium.owner@example.com");
        assertThat(email.getSubject()).isEqualTo("Indemnité versée pour le sinistre n°21");
        assertThat(body(email)).contains("3700.00", "22/11/2026", "600.00", "660.00");
    }

    private PolicyResponse createPolicy(String email) {
        return policyService.createPolicy(new PolicyRequest(email, "AUTO", new BigDecimal("50000.00"),
                new BigDecimal("600.00"), LocalDate.of(2026, 1, 1), PolicyStatus.ACTIVE));
    }

    private static MimeMessage singleEmail() {
        assertThat(smtp.waitForIncomingEmail(5000, 1)).as("an email was received").isTrue();
        return smtp.getReceivedMessages()[0];
    }

    private static String body(MimeMessage email) throws Exception {
        return email.getContent().toString();
    }

    private static GreenMail startSmtp() {
        GreenMail greenMail = new GreenMail(ServerSetupTest.SMTP.dynamicPort());
        greenMail.start();
        return greenMail;
    }
}

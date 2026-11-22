package ma.sinistrepro.policies;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.LocalDate;
import ma.sinistrepro.policies.api.PolicyRequest;
import ma.sinistrepro.policies.api.PolicyResponse;
import ma.sinistrepro.policies.domain.PolicyStatus;
import ma.sinistrepro.policies.event.ClaimDeclaredListener;
import ma.sinistrepro.policies.event.IndemnityPaidListener;
import ma.sinistrepro.policies.service.PolicyService;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.test.context.EmbeddedKafka;

@SpringBootTest(properties = {
        "spring.kafka.listener.auto-startup=true",
        "spring.kafka.consumer.auto-offset-reset=earliest"
})
@EmbeddedKafka(topics = {ClaimDeclaredListener.TOPIC, IndemnityPaidListener.TOPIC}, bootstrapServersProperty = "spring.kafka.bootstrap-servers")
class PolicyEventListenersKafkaTests {

    @Autowired
    private PolicyService policyService;

    @Autowired
    private KafkaTemplate<String, String> kafkaTemplate;

    @Test
    void claimDeclaredMessageFromClaimsServiceIsRecordedOnPolicy() throws Exception {
        PolicyResponse policy = policyService.createPolicy(new PolicyRequest(
                "insured@example.com", "AUTO", new BigDecimal("50000.00"), new BigDecimal("600.00"),
                LocalDate.of(2026, 1, 1), PolicyStatus.ACTIVE));

        // An unreadable message must be skipped without blocking the next ones.
        kafkaTemplate.send(ClaimDeclaredListener.TOPIC, "11", "not json").get();

        // Same shape as the producer in service-sinistres: its own type header and an extra field.
        String payload = """
                {"claimId":12,"policyId":%d,"souscripteurEmail":"insured@example.com",
                 "dateSinistre":"2026-11-01","type":"COLLISION","montantDeclare":4200.00,"statut":"DECLARE"}
                """.formatted(policy.id());
        ProducerRecord<String, String> message = new ProducerRecord<>(ClaimDeclaredListener.TOPIC, "12", payload);
        message.headers().add("__TypeId__",
                "ma.sinistrepro.claims.event.ClaimDeclaredEvent".getBytes(StandardCharsets.UTF_8));
        kafkaTemplate.send(message).get();

        await().atMost(Duration.ofSeconds(15)).untilAsserted(() ->
                assertThat(policyService.findPolicyClaims(policy.id()))
                        .singleElement()
                        .satisfies(claim -> {
                            assertThat(claim.claimId()).isEqualTo(12L);
                            assertThat(claim.dateSinistre()).isEqualTo(LocalDate.of(2026, 11, 1));
                            assertThat(claim.montantDeclare()).isEqualByComparingTo("4200.00");
                        }));
    }

    @Test
    void indemnityPaidMessageFromIndemnitiesServiceRaisesThePremium() throws Exception {
        PolicyResponse policy = policyService.createPolicy(new PolicyRequest(
                "insured@example.com", "AUTO", new BigDecimal("50000.00"), new BigDecimal("600.00"),
                LocalDate.of(2026, 1, 1), PolicyStatus.ACTIVE));

        kafkaTemplate.send(IndemnityPaidListener.TOPIC, "21", "not json").get();

        // Same shape as the outbox payload of service-indemnites, plus a field this consumer does not know.
        String payload = """
                {"indemnityId":7,"claimId":21,"policyId":%d,"montantCalcule":3700.00,
                 "dateVirement":"2026-11-22","devise":"MAD"}
                """.formatted(policy.id());
        ProducerRecord<String, String> message = new ProducerRecord<>(IndemnityPaidListener.TOPIC, "21", payload);
        message.headers().add("__TypeId__",
                "ma.sinistrepro.indemnities.dto.IndemnityPaidEvent".getBytes(StandardCharsets.UTF_8));
        kafkaTemplate.send(message).get();
        // A redelivery must not raise the premium a second time.
        kafkaTemplate.send(IndemnityPaidListener.TOPIC, "21", payload).get();

        await().atMost(Duration.ofSeconds(15)).untilAsserted(() ->
                assertThat(policyService.findPremiumAdjustments(policy.id()))
                        .singleElement()
                        .satisfies(adjustment -> assertThat(adjustment.primeApres()).isEqualByComparingTo("660.00")));
        // Give the redelivered copy time to be consumed, then check it changed nothing.
        Thread.sleep(2000);
        assertThat(policyService.findPolicy(policy.id()).prime()).isEqualByComparingTo("660.00");
        assertThat(policyService.findPremiumAdjustments(policy.id())).hasSize(1);
    }
}

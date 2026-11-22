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
@EmbeddedKafka(topics = ClaimDeclaredListener.TOPIC, bootstrapServersProperty = "spring.kafka.bootstrap-servers")
class ClaimDeclaredListenerKafkaTests {

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
}

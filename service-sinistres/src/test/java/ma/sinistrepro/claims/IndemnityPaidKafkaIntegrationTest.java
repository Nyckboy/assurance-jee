
package ma.sinistrepro.claims;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDate;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.TimeUnit;

import ma.sinistrepro.claims.domain.ClaimFile;
import ma.sinistrepro.claims.domain.ClaimStatus;
import ma.sinistrepro.claims.event.IndemnityPaidEvent;
import ma.sinistrepro.claims.event.IndemnityPaidEventConsumer;
import ma.sinistrepro.claims.repository.ClaimFileRepository;

import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.serialization.StringSerializer;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.kafka.test.EmbeddedKafkaBroker;
import org.springframework.kafka.test.context.EmbeddedKafka;

import tools.jackson.databind.ObjectMapper;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(properties = {
        "eureka.client.enabled=false",
        "spring.cloud.discovery.enabled=false"
})
@EmbeddedKafka(
        partitions = 1,
        topics = "indemnity-paid",
        bootstrapServersProperty = "spring.kafka.bootstrap-servers"
)
class IndemnityPaidKafkaIntegrationTest {

    private static final String TOPIC = "indemnity-paid";

    @Autowired
    private EmbeddedKafkaBroker broker;

    @Autowired
    private ClaimFileRepository claimFileRepository;

    @Autowired
    private IndemnityPaidEventConsumer consumer;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void paidIndemnityUpdatesClaimAndDuplicateIsIdempotent() throws Exception {

        ClaimFile claim = claimFileRepository.save(newClaim());

        IndemnityPaidEvent event = new IndemnityPaidEvent(
                10L,
                claim.getId(),
                claim.getPolicyId(),
                new BigDecimal("12000.00"),
                LocalDate.of(2026, 11, 22)
        );

        String payload = objectMapper.writeValueAsString(event);

        // Publish through the embedded broker to exercise the real listener.
        try (KafkaProducer<String, String> producer = newProducer()) {
            producer.send(new ProducerRecord<>(
                    TOPIC,
                    claim.getId().toString(),
                    payload
            )).get(10, TimeUnit.SECONDS);
        }

        // Kafka listeners execute asynchronously.
        awaitClaimStatus(claim.getId(), ClaimStatus.INDEMNISE);

        ClaimFile updated = claimFileRepository.findById(claim.getId())
                .orElseThrow();

        assertEquals(ClaimStatus.INDEMNISE, updated.getStatut());

        // Payment must not overwrite the amount established by expertise.
        assertEquals(0, new BigDecimal("12000.00")
                .compareTo(updated.getMontantRetenu()));

        // Explicitly verify that replaying the event is safe.
        assertDoesNotThrow(() -> consumer.consume(payload));

        assertEquals(
                ClaimStatus.INDEMNISE,
                claimFileRepository.findById(claim.getId())
                        .orElseThrow()
                        .getStatut()
        );
    }

    @Test
    void mismatchedPolicyDoesNotModifyClaim() throws Exception {

        ClaimFile claim = claimFileRepository.save(newClaim());

        IndemnityPaidEvent event = new IndemnityPaidEvent(
                11L,
                claim.getId(),
                999L,
                new BigDecimal("12000.00"),
                LocalDate.of(2026, 11, 22)
        );

        String payload = objectMapper.writeValueAsString(event);

        // Invoke the consumer directly: an invalid Kafka message would
        // otherwise enter the listener's retry/error-handling mechanism.
        assertThrows(
                IllegalStateException.class,
                () -> consumer.consume(payload)
        );

        ClaimFile unchanged = claimFileRepository
                .findById(claim.getId())
                .orElseThrow();

        assertEquals(ClaimStatus.EN_EXPERTISE, unchanged.getStatut());
    }

    private KafkaProducer<String, String> newProducer() {

        Properties properties = new Properties();

        properties.put(
                ProducerConfig.BOOTSTRAP_SERVERS_CONFIG,
                broker.getBrokersAsString()
        );

        properties.put(
                ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG,
                StringSerializer.class
        );

        properties.put(
                ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG,
                StringSerializer.class
        );

        return new KafkaProducer<>(properties);
    }

    private void awaitClaimStatus(Long claimId, ClaimStatus expected)
            throws InterruptedException {

        long deadline = System.nanoTime() + Duration.ofSeconds(10).toNanos();

        while (System.nanoTime() < deadline) {

            ClaimStatus actual = claimFileRepository.findById(claimId)
                    .orElseThrow()
                    .getStatut();

            if (actual == expected) {
                return;
            }

            Thread.sleep(100);
        }

        fail("Claim " + claimId + " did not reach status " + expected);
    }

    private ClaimFile newClaim() {

        ClaimFile claim = new ClaimFile();

        claim.setPolicyId(3L);
        claim.setSouscripteurEmail("assure@example.com");
        claim.setDateSinistre(LocalDate.of(2026, 11, 1));
        claim.setType("COLLISION");
        claim.setMontantDeclare(new BigDecimal("15000.00"));
        claim.setMontantRetenu(new BigDecimal("12000.00"));
        claim.setStatut(ClaimStatus.EN_EXPERTISE);

        return claim;
    }
}

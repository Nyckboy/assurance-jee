
package ma.sinistrepro.claims;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import ma.sinistrepro.claims.domain.ClaimFile;
import ma.sinistrepro.claims.domain.ClaimStatus;
import ma.sinistrepro.claims.domain.Expertise;
import ma.sinistrepro.claims.repository.ClaimFileRepository;
import ma.sinistrepro.claims.service.ExpertiseService;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.TopicPartition;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.kafka.test.EmbeddedKafkaBroker;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.kafka.test.utils.KafkaTestUtils;
import ma.sinistrepro.claims.exception.InvalidClaimStatusException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(properties = {
        "eureka.client.enabled=false",
        "spring.cloud.discovery.enabled=false"
})
@EmbeddedKafka(
        partitions = 1,
        topics = "expertise-completed",
        bootstrapServersProperty = "spring.kafka.bootstrap-servers"
)
class ExpertiseCompletedKafkaIntegrationTest {

    private static final String TOPIC = "expertise-completed";

    @Autowired
    private EmbeddedKafkaBroker broker;

    @Autowired
    private ExpertiseService expertiseService;

    @Autowired
    private ClaimFileRepository claimFileRepository;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void completedExpertisePublishesExactlyOneEvent() throws Exception {

        // Seed the database directly to isolate expertise event publication.
        ClaimFile claim = claimFileRepository.save(newClaim());

        try (KafkaConsumer<String, String> consumer = consumerAtEnd()) {

            Expertise saved = expertiseService.createExpertise(
                    newExpertise(claim.getId())
            );

            assertNotNull(saved.getId());

            List<ConsumerRecord<String, String>> received = new ArrayList<>();
            consumer.poll(Duration.ofSeconds(10))
                    .records(TOPIC)
                    .forEach(received::add);

            assertEquals(1, received.size(),
                    "One completed expertise must produce one event");

            ConsumerRecord<String, String> record = received.get(0);

            assertEquals(claim.getId().toString(), record.key());

            JsonNode json = objectMapper.readTree(record.value());

            assertEquals(1, json.size(),
                    "The event contract contains only claimId");

            assertEquals(claim.getId().longValue(),
                    json.get("claimId").asLong());

            ClaimFile updated = claimFileRepository
                    .findById(claim.getId())
                    .orElseThrow();

            assertEquals(ClaimStatus.EN_EXPERTISE, updated.getStatut());
            assertEquals(0, new BigDecimal("12000.00")
                    .compareTo(updated.getMontantRetenu()));

            assertEquals(0, consumer.poll(Duration.ofMillis(800)).count(),
                    "No duplicate event should be published");
        }
    }

    @Test
    void rejectedExpertisePublishesNothing() {

        ClaimFile claim = newClaim();
        claim.setStatut(ClaimStatus.EN_EXPERTISE);
        claim = claimFileRepository.save(claim);

        try (KafkaConsumer<String, String> consumer = consumerAtEnd()) {

            Expertise expertise = newExpertise(claim.getId());

            assertThrows(InvalidClaimStatusException.class,
                    () -> expertiseService.createExpertise(expertise));

            assertEquals(0, consumer.poll(Duration.ofSeconds(2)).count(),
                    "Rejected expertise must not publish an event");
        }
    }

    private KafkaConsumer<String, String> consumerAtEnd() {

        Map<String, Object> properties = KafkaTestUtils.consumerProps(
                broker,
                "expertise-test-" + UUID.randomUUID(),
                false
        );

        properties.put(
                ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG,
                StringDeserializer.class
        );

        properties.put(
                ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG,
                StringDeserializer.class
        );

        KafkaConsumer<String, String> consumer =
                new KafkaConsumer<>(properties);

        TopicPartition partition = new TopicPartition(TOPIC, 0);

        consumer.assign(List.of(partition));
        consumer.seekToEnd(List.of(partition));

        // Establish the end offset before publishing the event.
        consumer.position(partition);

        return consumer;
    }

    private ClaimFile newClaim() {

        ClaimFile claim = new ClaimFile();

        claim.setPolicyId(3L);
        claim.setSouscripteurEmail("assure@example.com");
        claim.setDateSinistre(LocalDate.of(2026, 11, 1));
        claim.setType("COLLISION");
        claim.setMontantDeclare(new BigDecimal("15000.00"));
        claim.setStatut(ClaimStatus.DECLARE);

        return claim;
    }

    private Expertise newExpertise(Long claimId) {

        Expertise expertise = new Expertise();

        expertise.setClaimId(claimId);
        expertise.setExpertNom("Expert Test");
        expertise.setDate(LocalDate.of(2026, 11, 22));
        expertise.setMontantEstime(new BigDecimal("12000.00"));
        expertise.setAvis("FAVORABLE");

        return expertise;
    }
}

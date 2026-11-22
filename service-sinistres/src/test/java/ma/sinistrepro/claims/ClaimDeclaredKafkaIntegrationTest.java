package ma.sinistrepro.claims;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDate;
import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.util.UUID;

import ma.sinistrepro.claims.client.CoverageResponse;
import ma.sinistrepro.claims.client.PolicyClient;
import ma.sinistrepro.claims.domain.ClaimFile;
import ma.sinistrepro.claims.service.ClaimService;
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
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@SpringBootTest
@EmbeddedKafka(partitions = 1, topics = "claim-declared",
        bootstrapServersProperty = "spring.kafka.bootstrap-servers")
class ClaimDeclaredKafkaIntegrationTest {

    private static final String TOPIC = "claim-declared";

    @Autowired
    private EmbeddedKafkaBroker broker;

    @Autowired
    private ClaimService claimService;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private PolicyClient policyClient;

    @Test
    void coveredClaimPublishesExactlyOneEventWithContract() throws Exception {
        try (KafkaConsumer<String, String> consumer = consumerAtEnd()) {
            when(policyClient.verifyCoverage(eq(3L), eq("COLLISION")))
                    .thenReturn(new CoverageResponse(3L, "COLLISION", true, "Covered",
                            new BigDecimal("30000.00"), new BigDecimal("1000.00")));

            ClaimFile saved = claimService.createClaim(newClaim());
            List<ConsumerRecord<String, String>> received = new ArrayList<>();
            consumer.poll(Duration.ofSeconds(10)).records(TOPIC).forEach(received::add);
            assertEquals(1, received.size(), "One claim must produce one event");

            ConsumerRecord<String, String> record = received.get(0);
            assertEquals(saved.getId().toString(), record.key());

            JsonNode json = objectMapper.readTree(record.value());
            assertEquals(6, json.size(), "The validated event has exactly six fields");
            assertEquals(saved.getId().longValue(), json.get("claimId").asLong());
            assertEquals(3L, json.get("policyId").asLong());
            assertEquals("assure@example.com", json.get("souscripteurEmail").asText());
            assertEquals("2026-11-01", json.get("dateSinistre").asText());
            assertTrue(json.get("dateSinistre").isTextual());
            assertEquals("COLLISION", json.get("type").asText());
            assertTrue(json.get("montantDeclare").isNumber());
            assertEquals(0, new BigDecimal("4200.00")
                    .compareTo(new BigDecimal(json.get("montantDeclare").asText())));
            assertEquals(0, consumer.poll(Duration.ofMillis(800)).count(), "No duplicate event");
        }
    }

    @Test
    void uncoveredClaimPublishesNothing() {
        try (KafkaConsumer<String, String> consumer = consumerAtEnd()) {
            when(policyClient.verifyCoverage(eq(3L), eq("COLLISION")))
                    .thenReturn(new CoverageResponse(3L, "COLLISION", false,
                            "No matching guarantee", null, null));

            assertThrows(org.springframework.web.server.ResponseStatusException.class,
                    () -> claimService.createClaim(newClaim()));
            assertEquals(0, consumer.poll(Duration.ofSeconds(2)).count());
        }
    }

    private KafkaConsumer<String, String> consumerAtEnd() {
        Map<String, Object> properties = KafkaTestUtils.consumerProps(
                broker, "claim-test-" + UUID.randomUUID(), false);
        properties.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        properties.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);

        KafkaConsumer<String, String> consumer = new KafkaConsumer<>(properties);
        TopicPartition partition = new TopicPartition(TOPIC, 0);
        consumer.assign(List.of(partition));
        consumer.seekToEnd(List.of(partition));
        consumer.position(partition); // Resolve end offset before the producer sends.
        return consumer;
    }

    private ClaimFile newClaim() {
        ClaimFile claim = new ClaimFile();
        claim.setPolicyId(3L);
        claim.setSouscripteurEmail("assure@example.com");
        claim.setDateSinistre(LocalDate.of(2026, 11, 1));
        claim.setType("COLLISION");
        claim.setMontantDeclare(new BigDecimal("4200.00"));
        return claim;
    }
}

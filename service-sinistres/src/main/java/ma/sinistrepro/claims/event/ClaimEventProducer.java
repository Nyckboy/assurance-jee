
package ma.sinistrepro.claims.event;

import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component
public class ClaimEventProducer {

    private final KafkaTemplate<String, String> kafkaTemplate;

    public ClaimEventProducer(
            KafkaTemplate<String, String> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    // Publish a claim-declared event to Kafka.
    public void publishClaimDeclared(String eventJson, Long claimId) {
        kafkaTemplate.send(
            "claim-declared",
            claimId.toString(),
            eventJson
        );
    }

    // Publish an expertise-completed event to Kafka.
    public void publishExpertiseCompleted(String eventJson, Long claimId) {
        kafkaTemplate.send(
            "expertise-completed",
            claimId.toString(),
            eventJson
        );
    }
}

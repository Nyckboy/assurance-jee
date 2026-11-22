package ma.sinistrepro.claims.event;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
public final class ClaimEventProducer {

    public static final String CLAIM_DECLARED_TOPIC = "claim-declared";

    private static final Logger LOGGER =
            LoggerFactory.getLogger(ClaimEventProducer.class);

    private final KafkaTemplate<String, ClaimDeclaredEvent> kafkaTemplate;

    public ClaimEventProducer(
            KafkaTemplate<String, ClaimDeclaredEvent> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    // Publish only after the database transaction commits.
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onClaimDeclared(ClaimDeclaredEvent event) {
        kafkaTemplate.send(
                CLAIM_DECLARED_TOPIC,
                event.claimId().toString(),
                event
        ).whenComplete((result, error) -> {
            if (error != null) {
                LOGGER.error(
                        "Failed to publish claim-declared for claim {}",
                        event.claimId(),
                        error
                );
            }
        });
    }
}
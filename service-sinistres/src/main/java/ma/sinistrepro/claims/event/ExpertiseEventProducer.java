package ma.sinistrepro.claims.event;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
public final class ExpertiseEventProducer {

    private static final String TOPIC = "expertise-completed";

    private static final Logger LOGGER =
            LoggerFactory.getLogger(ExpertiseEventProducer.class);

    private final KafkaTemplate<String, ExpertiseCompletedEvent> kafkaTemplate;

    public ExpertiseEventProducer(
            KafkaTemplate<String, ExpertiseCompletedEvent> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    // Publish only when the expertise transaction commits successfully.
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onExpertiseCompleted(ExpertiseCompletedEvent event) {
        kafkaTemplate.send(
                TOPIC,
                event.claimId().toString(),
                event
        ).whenComplete((result, error) -> {
            if (error != null) {
                LOGGER.error(
                        "Failed to publish expertise-completed for claim {}",
                        event.claimId(),
                        error
                );
            }
        });
    }
}
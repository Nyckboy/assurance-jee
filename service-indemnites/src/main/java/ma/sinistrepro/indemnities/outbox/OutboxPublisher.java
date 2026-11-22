package ma.sinistrepro.indemnities.outbox;

import ma.sinistrepro.indemnities.messaging.IndemnityKafkaProducer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class OutboxPublisher {

    private static final Logger log =
            LoggerFactory.getLogger(OutboxPublisher.class);

    private final OutboxRepository outboxRepository;
    private final IndemnityKafkaProducer kafkaProducer;

    public OutboxPublisher(
            OutboxRepository outboxRepository,
            IndemnityKafkaProducer kafkaProducer) {
        this.outboxRepository = outboxRepository;
        this.kafkaProducer = kafkaProducer;
    }

    @Scheduled(fixedDelayString = "${outbox.publisher.delay:5000}")
    @Transactional
    public void publishPendingEvents() {

        var events =
                outboxRepository.findTop100ByPublishedAtIsNullOrderByCreatedAtAsc();

        for (OutboxEvent event : events) {
            try {
                kafkaProducer.publish(
                        event.getTopic(),
                        event.getEventKey(),
                        event.getPayload()
                );

                event.markAsPublished();
                outboxRepository.save(event);

                log.info("Événement Outbox {} publié sur {}",
                        event.getId(), event.getTopic());

            } catch (Exception exception) {
                log.error("Échec de publication de l'événement Outbox {}",
                        event.getId(), exception);
            }
        }
    }
}
package ma.sinistrepro.indemnities.messaging;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import ma.sinistrepro.indemnities.dto.ExpertiseCompletedEvent;
import ma.sinistrepro.indemnities.service.IndemnityService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
public class IndemnityKafkaConsumer {

    private static final Logger log =
            LoggerFactory.getLogger(IndemnityKafkaConsumer.class);

    private static final String TOPIC = "expertise-completed";

    private final ObjectMapper objectMapper;
    private final IndemnityService indemnityService;

    public IndemnityKafkaConsumer(
            ObjectMapper objectMapper,
            IndemnityService indemnityService) {
        this.objectMapper = objectMapper;
        this.indemnityService = indemnityService;
    }

    @KafkaListener(
            topics = TOPIC,
            groupId = "${spring.application.name}"
    )
    public void consumeExpertiseCompleted(String message) {

        final ExpertiseCompletedEvent event;

        try {
            event = objectMapper.readValue(
                    message,
                    ExpertiseCompletedEvent.class
            );
        } catch (JsonProcessingException exception) {
            log.error("Message expertise-completed invalide : {}", message, exception);
            return;
        }

        if (event.claimId() == null || event.claimId() <= 0) {
            log.error("Message expertise-completed sans claimId valide : {}", message);
            return;
        }

        log.info(
                "Expertise terminée reçue pour le sinistre {}",
                event.claimId()
        );

        // Le service récupère les données officielles via OpenFeign,
        // vérifie la couverture et calcule l'indemnité.
        indemnityService.handleExpertiseCompleted(event.claimId());

        log.info(
                "Indemnité préparée pour le sinistre {}",
                event.claimId()
        );
    }
}

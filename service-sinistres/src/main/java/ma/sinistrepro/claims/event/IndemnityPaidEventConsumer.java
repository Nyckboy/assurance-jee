package ma.sinistrepro.claims.event;

import ma.sinistrepro.claims.domain.ClaimFile;
import ma.sinistrepro.claims.domain.ClaimStatus;
import ma.sinistrepro.claims.repository.ClaimFileRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;

/**
 * Marks a claim INDEMNISE when service-indemnites publishes indemnity-paid.
 *
 * <p>A message that can never be applied (unreadable, incomplete, unknown claim, other policy, wrong status)
 * is logged and skipped: retrying it would fail the same way every time.
 */
@Component
public class IndemnityPaidEventConsumer {

    private static final Logger LOGGER =
            LoggerFactory.getLogger(IndemnityPaidEventConsumer.class);

    private final ClaimFileRepository claimFileRepository;
    private final ObjectMapper objectMapper;

    public IndemnityPaidEventConsumer(
            ClaimFileRepository claimFileRepository,
            ObjectMapper objectMapper) {

        this.claimFileRepository = claimFileRepository;
        this.objectMapper = objectMapper;
    }

    @Transactional
    @KafkaListener(topics = "indemnity-paid", groupId = "${spring.application.name}")
    public void consume(String message) {

        // Deserialize the exact contract published by service-indemnites.
        IndemnityPaidEvent event;
        try {
            event = objectMapper.readValue(message, IndemnityPaidEvent.class);
        } catch (JacksonException exception) {
            LOGGER.error("Skipping unreadable indemnity-paid message: {}", message, exception);
            return;
        }

        if (event.indemnityId() == null
                || event.claimId() == null
                || event.claimId() <= 0
                || event.policyId() == null
                || event.policyId() <= 0
                || event.montantCalcule() == null
                || event.montantCalcule().signum() < 0
                || event.dateVirement() == null) {
            LOGGER.error("Skipping invalid indemnity-paid message: {}", message);
            return;
        }

        ClaimFile claim = claimFileRepository.findById(event.claimId()).orElse(null);
        if (claim == null) {
            LOGGER.error("Skipping indemnity {}: claim {} not found", event.indemnityId(), event.claimId());
            return;
        }

        // Reject events associated with a different insurance policy.
        if (!event.policyId().equals(claim.getPolicyId())) {
            LOGGER.error("Skipping indemnity {}: claim {} belongs to policy {}, not {}",
                    event.indemnityId(), claim.getId(), claim.getPolicyId(), event.policyId());
            return;
        }

        // Kafka may deliver an event more than once.
        if (claim.getStatut() == ClaimStatus.INDEMNISE) {
            LOGGER.info("Claim {} already indemnified", claim.getId());
            return;
        }

        // Only an expert-assessed claim can be indemnified.
        if (claim.getStatut() != ClaimStatus.EN_EXPERTISE) {
            LOGGER.error("Skipping indemnity {}: claim {} is {}, not EN_EXPERTISE",
                    event.indemnityId(), claim.getId(), claim.getStatut());
            return;
        }

        // JPA dirty checking persists this change on transaction commit.
        claim.setStatut(ClaimStatus.INDEMNISE);

        LOGGER.info(
                "Claim {} indemnified by indemnity {}",
                claim.getId(),
                event.indemnityId()
        );
    }
}

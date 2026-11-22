
package ma.sinistrepro.claims.event;

import ma.sinistrepro.claims.domain.ClaimFile;
import ma.sinistrepro.claims.domain.ClaimStatus;
import ma.sinistrepro.claims.repository.ClaimFileRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.ObjectMapper;

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
        IndemnityPaidEvent event =
                objectMapper.readValue(message, IndemnityPaidEvent.class);

        if (event.indemnityId() == null
                || event.claimId() == null
                || event.claimId() <= 0
                || event.policyId() == null
                || event.policyId() <= 0
                || event.montantCalcule() == null
                || event.montantCalcule().signum() < 0
                || event.dateVirement() == null) {
            throw new IllegalArgumentException("Invalid indemnity-paid event");
        }

        ClaimFile claim = claimFileRepository.findById(event.claimId())
                .orElseThrow(() -> new IllegalStateException(
                        "Claim not found: " + event.claimId()));

        // Reject events associated with a different insurance policy.
        if (!event.policyId().equals(claim.getPolicyId())) {
            throw new IllegalStateException(
                    "Policy mismatch for claim " + event.claimId());
        }

        // Kafka may deliver an event more than once.
        if (claim.getStatut() == ClaimStatus.INDEMNISE) {
            LOGGER.info("Claim {} already indemnified", claim.getId());
            return;
        }

        // Only an expert-assessed claim can be indemnified.
        if (claim.getStatut() != ClaimStatus.EN_EXPERTISE) {
            throw new IllegalStateException(
                    "Invalid claim status for indemnification: "
                            + claim.getStatut());
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

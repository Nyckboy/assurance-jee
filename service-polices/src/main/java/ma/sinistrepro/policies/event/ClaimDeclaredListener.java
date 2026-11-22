package ma.sinistrepro.policies.event;

import ma.sinistrepro.policies.service.PolicyService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.json.JsonMapper;

/**
 * Consumes claim-declared events published by service-sinistres.
 * The value is read as raw JSON, so producer type headers (__TypeId__) are never needed.
 */
@Component
public class ClaimDeclaredListener {

    public static final String TOPIC = "claim-declared";

    private static final Logger log = LoggerFactory.getLogger(ClaimDeclaredListener.class);

    private final JsonMapper jsonMapper;
    private final PolicyService policyService;

    public ClaimDeclaredListener(JsonMapper jsonMapper, PolicyService policyService) {
        this.jsonMapper = jsonMapper;
        this.policyService = policyService;
    }

    @KafkaListener(topics = TOPIC, groupId = "${spring.application.name}")
    public void onClaimDeclared(String payload) {
        ClaimDeclaredEvent event;
        try {
            event = jsonMapper.readValue(payload, ClaimDeclaredEvent.class);
        } catch (JacksonException exception) {
            // A malformed message would fail again on every retry, so it is logged and skipped.
            log.error("Skipping unreadable {} message: {}", TOPIC, payload, exception);
            return;
        }

        if (event.claimId() == null || event.policyId() == null) {
            log.error("Skipping {} message without claimId or policyId: {}", TOPIC, payload);
            return;
        }
        policyService.recordDeclaredClaim(event);
    }
}

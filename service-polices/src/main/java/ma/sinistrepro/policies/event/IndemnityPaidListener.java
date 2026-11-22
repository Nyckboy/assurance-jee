package ma.sinistrepro.policies.event;

import ma.sinistrepro.policies.service.PolicyService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.json.JsonMapper;

/**
 * Consumes indemnity-paid events published by service-indemnites and applies the premium rule.
 * The value is read as raw JSON, so producer type headers (__TypeId__) are never needed.
 */
@Component
public class IndemnityPaidListener {

    public static final String TOPIC = "indemnity-paid";

    private static final Logger log = LoggerFactory.getLogger(IndemnityPaidListener.class);

    private final JsonMapper jsonMapper;
    private final PolicyService policyService;

    public IndemnityPaidListener(JsonMapper jsonMapper, PolicyService policyService) {
        this.jsonMapper = jsonMapper;
        this.policyService = policyService;
    }

    @KafkaListener(topics = TOPIC, groupId = "${spring.application.name}")
    public void onIndemnityPaid(String payload) {
        IndemnityPaidEvent event;
        try {
            event = jsonMapper.readValue(payload, IndemnityPaidEvent.class);
        } catch (JacksonException exception) {
            // A malformed message would fail again on every retry, so it is logged and skipped.
            log.error("Skipping unreadable {} message: {}", TOPIC, payload, exception);
            return;
        }

        if (event.claimId() == null || event.policyId() == null || event.indemnityId() == null
                || event.montantCalcule() == null) {
            log.error("Skipping {} message without claimId, policyId, indemnityId or montantCalcule: {}",
                    TOPIC, payload);
            return;
        }
        policyService.applyIndemnityPaid(event);
    }
}

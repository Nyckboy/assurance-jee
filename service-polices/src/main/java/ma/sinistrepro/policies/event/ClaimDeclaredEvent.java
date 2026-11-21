package ma.sinistrepro.policies.event;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Payload of the claim-declared topic, as agreed with service-sinistres.
 * Unknown fields are ignored so the producer can add fields without breaking this consumer.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record ClaimDeclaredEvent(
        Long claimId,
        Long policyId,
        String souscripteurEmail,
        LocalDate dateSinistre,
        String type,
        BigDecimal montantDeclare
) {
}

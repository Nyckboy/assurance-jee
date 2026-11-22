
package ma.sinistrepro.claims.event;

import java.math.BigDecimal;
import java.time.LocalDate;

// Event payload for the claim-declared Kafka topic.
// The schema must be agreed with service-polices consumers.
public record ClaimDeclaredEvent(
    Long claimId,
    Long policyId,
    String souscripteurEmail,
    LocalDate dateSinistre,
    String type,
    BigDecimal montantDeclare
) {
}

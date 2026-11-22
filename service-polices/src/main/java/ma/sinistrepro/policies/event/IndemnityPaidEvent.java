package ma.sinistrepro.policies.event;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Payload of the indemnity-paid event published by service-indemnites once the transfer is made.
 * Unknown fields are ignored so the producer can add fields without breaking this consumer.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record IndemnityPaidEvent(
        Long indemnityId,
        Long claimId,
        Long policyId,
        BigDecimal montantCalcule,
        LocalDate dateVirement) {
}

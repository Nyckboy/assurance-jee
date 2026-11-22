package ma.sinistrepro.policies.notification;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Published by the policy service when a paid indemnity raised a policy premium.
 * The email is sent only after the transaction commits.
 */
public record PremiumIncreasedNotification(
        String souscripteurEmail,
        Long policyId,
        Long claimId,
        BigDecimal montantIndemnise,
        LocalDate dateVirement,
        BigDecimal primeAvant,
        BigDecimal primeApres) {
}

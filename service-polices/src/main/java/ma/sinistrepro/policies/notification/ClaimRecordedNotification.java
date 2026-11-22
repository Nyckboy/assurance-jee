package ma.sinistrepro.policies.notification;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Published by the policy service when a declared claim was recorded on a policy.
 * The email is sent only after the transaction commits.
 */
public record ClaimRecordedNotification(
        String souscripteurEmail,
        Long policyId,
        Long claimId,
        String type,
        LocalDate dateSinistre,
        BigDecimal montantDeclare) {
}

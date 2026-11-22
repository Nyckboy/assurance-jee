package ma.sinistrepro.policies.notification;

import java.time.format.DateTimeFormatter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * Emails the policyholder about their policy.
 *
 * <ul>
 *   <li>Emails are sent after the database transaction commits, so a rolled-back change is never announced
 *       and a redelivered Kafka message (ignored by the service) sends nothing.</li>
 *   <li>A mail failure never undoes the business change: it is logged.</li>
 *   <li>Without {@code spring.mail.host} (for example in tests), emails are only logged.</li>
 * </ul>
 */
@Component
public class PolicyholderNotifier {

    private static final Logger log = LoggerFactory.getLogger(PolicyholderNotifier.class);
    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private final ObjectProvider<JavaMailSender> mailSender;
    private final String from;

    public PolicyholderNotifier(ObjectProvider<JavaMailSender> mailSender,
                                @Value("${policies.notifications.from:Sinistre-Pro <no-reply@sinistre-pro.local>}")
                                String from) {
        this.mailSender = mailSender;
        this.from = from;
    }

    @TransactionalEventListener
    public void onClaimRecorded(ClaimRecordedNotification notification) {
        String subject = "Sinistre n°%d enregistré sur votre police n°%d"
                .formatted(notification.claimId(), notification.policyId());
        String body = """
                Bonjour,

                Nous avons bien enregistré la déclaration de votre sinistre :

                - Numéro de sinistre : %d
                - Police : %d
                - Type : %s
                - Date du sinistre : %s
                - Montant déclaré : %s

                Votre dossier va maintenant être examiné par un expert. Nous vous tiendrons informé(e) de son avancement.

                Cordialement,
                Sinistre-Pro
                """.formatted(notification.claimId(), notification.policyId(), notification.type(),
                notification.dateSinistre().format(DATE), notification.montantDeclare().toPlainString());
        send(notification.souscripteurEmail(), subject, body);
    }

    @TransactionalEventListener
    public void onPremiumIncreased(PremiumIncreasedNotification notification) {
        String subject = "Indemnité versée pour le sinistre n°%d".formatted(notification.claimId());
        String dateVirement = notification.dateVirement() == null
                ? "" : " le " + notification.dateVirement().format(DATE);
        String body = """
                Bonjour,

                L'indemnité de %s pour votre sinistre n°%d a été versée%s.

                Conformément aux conditions de votre contrat, ce sinistre est répercuté sur votre prime :
                la prime de votre police n°%d passe de %s à %s.

                Cordialement,
                Sinistre-Pro
                """.formatted(notification.montantIndemnise().toPlainString(), notification.claimId(), dateVirement,
                notification.policyId(), notification.primeAvant().toPlainString(),
                notification.primeApres().toPlainString());
        send(notification.souscripteurEmail(), subject, body);
    }

    private void send(String to, String subject, String body) {
        JavaMailSender sender = mailSender.getIfAvailable();
        if (sender == null) {
            log.info("Email not sent (spring.mail.host is not set) to {}: {}", to, subject);
            return;
        }

        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(from);
        message.setTo(to);
        message.setSubject(subject);
        message.setText(body);
        try {
            sender.send(message);
            log.info("Email sent to {}: {}", to, subject);
        } catch (MailException exception) {
            log.warn("Email to {} failed ({}): {}", to, exception.getMessage(), subject);
        }
    }
}

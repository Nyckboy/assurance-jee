package ma.sinistrepro.indemnities.client;

import feign.FeignException;
import ma.sinistrepro.indemnities.exception.RemoteServiceUnavailableException;

// Traduit l'échec d'un appel Feign, utilisé par les fallbacks des Circuit Breakers.
final class RemoteCallFailures {

    private RemoteCallFailures() {
    }

    static RuntimeException translate(String serviceName, Throwable cause) {
        // Une erreur 4xx est une réponse métier (sinistre introuvable, requête invalide...) :
        // elle est renvoyée telle quelle, sans retry, et GlobalExceptionHandler la traite.
        if (cause instanceof FeignException.FeignClientException clientError) {
            return clientError;
        }
        // Tout le reste (5xx, timeout, connexion refusée, aucune instance, circuit ouvert)
        // signifie que le service est indisponible : c'est ce cas que @Retryable réessaie.
        return new RemoteServiceUnavailableException(serviceName, cause);
    }
}

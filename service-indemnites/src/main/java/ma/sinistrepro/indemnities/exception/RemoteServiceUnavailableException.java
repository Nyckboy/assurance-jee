package ma.sinistrepro.indemnities.exception;

// Un microservice distant ne répond pas, répond trop lentement, renvoie une erreur 5xx,
// n'est pas enregistré dans Eureka, ou son Circuit Breaker est ouvert.
public class RemoteServiceUnavailableException extends RuntimeException {

    private final String serviceName;

    public RemoteServiceUnavailableException(String serviceName, Throwable cause) {
        super("Le service " + serviceName + " est momentanément indisponible.", cause);
        this.serviceName = serviceName;
    }

    public String getServiceName() {
        return serviceName;
    }
}

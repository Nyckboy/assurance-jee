package ma.sinistrepro.claims.exception;

import org.springframework.http.HttpStatus;

/**
 * Base of the errors this service answers with a specific HTTP status.
 * ApiExceptionHandler turns them into the JSON error response.
 */
public abstract class ClaimsApiException extends RuntimeException {

    private final HttpStatus status;

    protected ClaimsApiException(HttpStatus status, String message, Throwable cause) {
        super(message, cause);
        this.status = status;
    }

    public HttpStatus getStatus() {
        return status;
    }
}

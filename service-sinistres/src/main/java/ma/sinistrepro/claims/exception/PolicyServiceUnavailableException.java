package ma.sinistrepro.claims.exception;

import org.springframework.http.HttpStatus;

// 503: service-polices could not be reached to verify the coverage.
public class PolicyServiceUnavailableException extends ClaimsApiException {

    public PolicyServiceUnavailableException(String message, Throwable cause) {
        super(HttpStatus.SERVICE_UNAVAILABLE, message, cause);
    }
}

package ma.sinistrepro.claims.exception;

import org.springframework.http.HttpStatus;

// 404: the claim does not exist.
public class ResourceNotFoundException extends ClaimsApiException {

    public ResourceNotFoundException(String message) {
        super(HttpStatus.NOT_FOUND, message, null);
    }
}

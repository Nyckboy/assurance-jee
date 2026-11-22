package ma.sinistrepro.claims.exception;

import org.springframework.http.HttpStatus;

// 400: the request refers to something invalid, for example a policy that does not exist.
public class InvalidRequestException extends ClaimsApiException {

    public InvalidRequestException(String message) {
        this(message, null);
    }

    public InvalidRequestException(String message, Throwable cause) {
        super(HttpStatus.BAD_REQUEST, message, cause);
    }
}

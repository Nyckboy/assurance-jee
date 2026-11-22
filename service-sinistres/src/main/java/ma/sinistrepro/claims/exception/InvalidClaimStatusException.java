package ma.sinistrepro.claims.exception;

import org.springframework.http.HttpStatus;

// 409: the claim is not in the status this action requires (for example an expertise on a claim already assessed).
public class InvalidClaimStatusException extends ClaimsApiException {

    public InvalidClaimStatusException(String message) {
        super(HttpStatus.CONFLICT, message, null);
    }
}

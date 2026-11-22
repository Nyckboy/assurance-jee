package ma.sinistrepro.claims.exception;

import org.springframework.http.HttpStatus;

// 422: the policy exists but does not cover this type of claim.
public class ClaimNotCoveredException extends ClaimsApiException {

    public ClaimNotCoveredException(String message) {
        super(HttpStatus.UNPROCESSABLE_ENTITY, message, null);
    }
}

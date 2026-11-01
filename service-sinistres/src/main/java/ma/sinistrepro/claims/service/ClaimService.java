
package ma.sinistrepro.claims.service;

import java.util.List;
import ma.sinistrepro.claims.domain.ClaimFile;

public interface ClaimService {

    // Returns all registered claim files.
    List<ClaimFile> getAllClaims();

    // Retrieves a claim by its unique identifier.
    ClaimFile getClaimById(Long id);

    // Creates a claim after validating insurance coverage.
    ClaimFile createClaim(ClaimFile claim);
}

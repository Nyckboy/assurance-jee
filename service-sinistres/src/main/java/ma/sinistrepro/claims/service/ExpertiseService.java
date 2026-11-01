
package ma.sinistrepro.claims.service;

import java.util.List;
import ma.sinistrepro.claims.domain.Expertise;

public interface ExpertiseService {

    // Register an expertise for an existing claim.
    Expertise createExpertise(Expertise expertise);

    // Retrieve expertises associated with a claim.
    List<Expertise> getExpertisesByClaimId(Long claimId);
}


package ma.sinistrepro.claims.service;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;


import feign.FeignException;
import ma.sinistrepro.claims.client.PolicyClient;
import ma.sinistrepro.claims.client.CoverageResponse;
import ma.sinistrepro.claims.domain.ClaimFile;
import ma.sinistrepro.claims.repository.ClaimFileRepository;

@Service
public class ClaimServiceImpl implements ClaimService {


private final ClaimFileRepository claimFileRepository;
private final PolicyClient policyClient;

public ClaimServiceImpl(
        ClaimFileRepository claimFileRepository,
        PolicyClient policyClient) {
    this.claimFileRepository = claimFileRepository;
    this.policyClient = policyClient;
}


    @Override
    public List<ClaimFile> getAllClaims() {
        // Fetch claim files from the service's own database.
        return claimFileRepository.findAll();
    }


@Override
public ClaimFile createClaim(ClaimFile claim) {
    // Validate input before contacting the policy service.
    if (claim == null
            || claim.getPolicyId() == null
            || claim.getPolicyId() <= 0
            || claim.getSouscripteurEmail() == null
            || claim.getSouscripteurEmail().isBlank()
            || claim.getDateSinistre() == null
            || claim.getType() == null
            || claim.getType().isBlank()
            || claim.getMontantDeclare() == null
            || claim.getMontantDeclare().signum() <= 0) {
        throw new ResponseStatusException(
            HttpStatus.BAD_REQUEST,
            "Invalid claim data"
        );
    }

    CoverageResponse coverage;

    try {
        // Check coverage through the policy service's REST API.
        coverage = policyClient.verifyCoverage(
            claim.getPolicyId(),
            claim.getType()
        );
    } catch (FeignException.NotFound exception) {
        throw new ResponseStatusException(
            HttpStatus.BAD_REQUEST,
            "Insurance policy not found",
            exception
        );
    } catch (FeignException exception) {
        throw new ResponseStatusException(
            HttpStatus.SERVICE_UNAVAILABLE,
            "Policy verification unavailable",
            exception
        );
    }

    if (coverage == null || !coverage.covered()) {
        throw new ResponseStatusException(
            HttpStatus.UNPROCESSABLE_ENTITY,
            "Insurance does not cover this claim"
        );
    }

    // Never accept a client-supplied ID, status or retained amount.
    ClaimFile newClaim = new ClaimFile();
    newClaim.setPolicyId(claim.getPolicyId());
    newClaim.setSouscripteurEmail(claim.getSouscripteurEmail());
    newClaim.setDateSinistre(claim.getDateSinistre());
    newClaim.setType(claim.getType());
    newClaim.setMontantDeclare(claim.getMontantDeclare());

    return claimFileRepository.save(newClaim);
}


    @Override
    public ClaimFile getClaimById(Long id) {
        // Reject invalid identifiers before querying the database.
        if (id == null || id <= 0) {
            throw new ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                "Invalid claim ID"
            );
        }

        // Return HTTP 404 when the claim does not exist.
        return claimFileRepository.findById(id)
            .orElseThrow(() -> new ResponseStatusException(
                HttpStatus.NOT_FOUND,
                "Claim not found: " + id
            ));
    }
}

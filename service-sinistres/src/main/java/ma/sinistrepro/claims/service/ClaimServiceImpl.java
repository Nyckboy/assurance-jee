
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
import ma.sinistrepro.claims.event.ClaimEventProducer;

@Service
public class ClaimServiceImpl implements ClaimService {

    private final ClaimFileRepository claimFileRepository;
    private final PolicyClient policyClient;
    private final ClaimEventProducer claimEventProducer;

    public ClaimServiceImpl(
            ClaimFileRepository claimFileRepository,
            PolicyClient policyClient,
            ClaimEventProducer claimEventProducer) {

        this.claimFileRepository = claimFileRepository;
        this.policyClient = policyClient;
        this.claimEventProducer = claimEventProducer;
    }

    @Override
    public List<ClaimFile> getAllClaims() {
        // Retrieve claims from the local database.
        return claimFileRepository.findAll();
    }

    @Override
    public ClaimFile getClaimById(Long id) {

        if (id == null || id <= 0) {
            throw new ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                "Invalid claim ID"
            );
        }

        return claimFileRepository.findById(id)
            .orElseThrow(() -> new ResponseStatusException(
                HttpStatus.NOT_FOUND,
                "Claim not found: " + id
            ));
    }

    @Override
    public ClaimFile createClaim(ClaimFile claim) {

        // Validate the claim before contacting another service.
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
            // Verify insurance coverage through OpenFeign.
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

        // Do not trust client-provided IDs or claim statuses.
        ClaimFile newClaim = new ClaimFile();

        newClaim.setPolicyId(claim.getPolicyId());
        newClaim.setSouscripteurEmail(claim.getSouscripteurEmail());
        newClaim.setDateSinistre(claim.getDateSinistre());
        newClaim.setType(claim.getType());
        newClaim.setMontantDeclare(claim.getMontantDeclare());

        // Save the validated claim in the local database.
        return claimFileRepository.save(newClaim);
    }
}

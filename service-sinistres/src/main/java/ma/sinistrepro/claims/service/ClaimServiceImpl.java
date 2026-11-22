package ma.sinistrepro.claims.service;

import java.util.List;

import feign.FeignException;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ma.sinistrepro.claims.exception.ClaimNotCoveredException;
import ma.sinistrepro.claims.exception.InvalidRequestException;
import ma.sinistrepro.claims.exception.PolicyServiceUnavailableException;
import ma.sinistrepro.claims.exception.ResourceNotFoundException;

import ma.sinistrepro.claims.client.CoverageResponse;
import ma.sinistrepro.claims.client.PolicyClient;
import ma.sinistrepro.claims.domain.ClaimFile;
import ma.sinistrepro.claims.event.ClaimDeclaredEvent;
import ma.sinistrepro.claims.repository.ClaimFileRepository;

@Service
public class ClaimServiceImpl implements ClaimService {

    private final ClaimFileRepository claimFileRepository;
    private final PolicyClient policyClient;
    private final ApplicationEventPublisher eventPublisher;

    public ClaimServiceImpl(
            ClaimFileRepository claimFileRepository,
            PolicyClient policyClient,
            ApplicationEventPublisher eventPublisher) {
        this.claimFileRepository = claimFileRepository;
        this.policyClient = policyClient;
        this.eventPublisher = eventPublisher;
    }

    @Override
    @Transactional(readOnly = true)
    public List<ClaimFile> getAllClaims() {
        return claimFileRepository.findAll();
    }

    @Override
    @Transactional(readOnly = true)
    public ClaimFile getClaimById(Long id) {
        if (id == null || id <= 0) {
            throw new InvalidRequestException("Invalid claim ID");
        }
        return claimFileRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Claim not found: " + id));
    }

    @Override
    @Transactional
    public ClaimFile createClaim(ClaimFile claim) {
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
            throw new InvalidRequestException("Invalid claim data");
        }

        final CoverageResponse coverage;
        try {
            coverage = policyClient.verifyCoverage(claim.getPolicyId(), claim.getType());
        } catch (FeignException.NotFound exception) {
            throw new InvalidRequestException("Insurance policy not found", exception);
        } catch (FeignException exception) {
            throw new PolicyServiceUnavailableException("Policy verification unavailable", exception);
        }

        if (coverage == null || !coverage.covered()) {
            throw new ClaimNotCoveredException("Insurance does not cover this claim"
                    + (coverage == null ? "" : ": " + coverage.reason()));
        }

        // Ignore identifiers and mutable status submitted by the caller.
        ClaimFile newClaim = new ClaimFile();
        newClaim.setPolicyId(claim.getPolicyId());
        newClaim.setSouscripteurEmail(claim.getSouscripteurEmail());
        newClaim.setDateSinistre(claim.getDateSinistre());
        newClaim.setType(claim.getType());
        newClaim.setMontantDeclare(claim.getMontantDeclare());

        ClaimFile saved = claimFileRepository.save(newClaim);

        // Listener publishes only after the surrounding JPA transaction commits.
        eventPublisher.publishEvent(new ClaimDeclaredEvent(
                saved.getId(), saved.getPolicyId(), saved.getSouscripteurEmail(),
                saved.getDateSinistre(), saved.getType(), saved.getMontantDeclare()));

        return saved;
    }
}

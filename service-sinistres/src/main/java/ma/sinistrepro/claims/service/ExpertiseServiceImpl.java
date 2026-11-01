
package ma.sinistrepro.claims.service;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import ma.sinistrepro.claims.domain.ClaimFile;
import ma.sinistrepro.claims.domain.ClaimStatus;
import ma.sinistrepro.claims.domain.Expertise;
import ma.sinistrepro.claims.repository.ClaimFileRepository;
import ma.sinistrepro.claims.repository.ExpertiseRepository;

@Service
public class ExpertiseServiceImpl implements ExpertiseService {

    private final ExpertiseRepository expertiseRepository;
    private final ClaimFileRepository claimFileRepository;

    public ExpertiseServiceImpl(
            ExpertiseRepository expertiseRepository,
            ClaimFileRepository claimFileRepository) {
        this.expertiseRepository = expertiseRepository;
        this.claimFileRepository = claimFileRepository;
    }

    @Override
    @Transactional
    public Expertise createExpertise(Expertise expertise) {

        // Validate the assessment before modifying the database.
        if (expertise == null
                || expertise.getClaimId() == null
                || expertise.getClaimId() <= 0
                || expertise.getExpertNom() == null
                || expertise.getExpertNom().isBlank()
                || expertise.getDate() == null
                || expertise.getMontantEstime() == null
                || expertise.getMontantEstime().signum() < 0
                || expertise.getAvis() == null
                || expertise.getAvis().isBlank()) {
            throw new ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                "Invalid expertise data"
            );
        }

        // Ensure the referenced claim exists.
        ClaimFile claim = claimFileRepository
            .findById(expertise.getClaimId())
            .orElseThrow(() -> new ResponseStatusException(
                HttpStatus.NOT_FOUND,
                "Claim not found"
            ));

        // Only declared claims can enter expertise.
        if (claim.getStatut() != ClaimStatus.DECLARE) {
            throw new ResponseStatusException(
                HttpStatus.CONFLICT,
                "Claim cannot enter expertise from its current status"
            );
        }

        // Never accept a client-provided expertise identifier.
        Expertise newExpertise = new Expertise();
        newExpertise.setClaimId(claim.getId());
        newExpertise.setExpertNom(expertise.getExpertNom());
        newExpertise.setDate(expertise.getDate());
        newExpertise.setMontantEstime(expertise.getMontantEstime());
        newExpertise.setAvis(expertise.getAvis());

        Expertise saved = expertiseRepository.save(newExpertise);

        // Persist the claim's transition in the same transaction.
        claim.setStatut(ClaimStatus.EN_EXPERTISE);
        claim.setMontantRetenu(expertise.getMontantEstime());

        return saved;
    }

    @Override
    @Transactional(readOnly = true)
    public List<Expertise> getExpertisesByClaimId(Long claimId) {

        if (claimId == null || claimId <= 0) {
            throw new ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                "Invalid claim ID"
            );
        }

        if (!claimFileRepository.existsById(claimId)) {
            throw new ResponseStatusException(
                HttpStatus.NOT_FOUND,
                "Claim not found"
            );
        }

        return expertiseRepository.findByClaimId(claimId);
    }
}

package ma.sinistrepro.claims.service;

import java.util.List;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ma.sinistrepro.claims.exception.InvalidClaimStatusException;
import ma.sinistrepro.claims.exception.InvalidRequestException;
import ma.sinistrepro.claims.exception.ResourceNotFoundException;

import ma.sinistrepro.claims.domain.ClaimFile;
import ma.sinistrepro.claims.domain.ClaimStatus;
import ma.sinistrepro.claims.domain.Expertise;
import ma.sinistrepro.claims.event.ExpertiseCompletedEvent;
import ma.sinistrepro.claims.repository.ClaimFileRepository;
import ma.sinistrepro.claims.repository.ExpertiseRepository;

@Service
public class ExpertiseServiceImpl implements ExpertiseService {

    private final ExpertiseRepository expertiseRepository;
    private final ClaimFileRepository claimFileRepository;
    private final ApplicationEventPublisher eventPublisher;

    public ExpertiseServiceImpl(
            ExpertiseRepository expertiseRepository,
            ClaimFileRepository claimFileRepository,
            ApplicationEventPublisher eventPublisher) {

        this.expertiseRepository = expertiseRepository;
        this.claimFileRepository = claimFileRepository;
        this.eventPublisher = eventPublisher;
    }

    @Override
    @Transactional
    public Expertise createExpertise(Expertise expertise) {

        // Validate the expertise before modifying the database.
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

            throw new InvalidRequestException("Invalid expertise data");
        }

        // Ensure the referenced claim exists.
        ClaimFile claim = claimFileRepository
                .findById(expertise.getClaimId())
                .orElseThrow(() -> new ResourceNotFoundException("Claim not found: " + expertise.getClaimId()));

        // Only declared claims can enter expertise.
        if (claim.getStatut() != ClaimStatus.DECLARE) {
            throw new InvalidClaimStatusException("Claim cannot enter expertise from its current status");
        }

        // Never accept a client-provided expertise identifier.
        Expertise newExpertise = new Expertise();

        newExpertise.setClaimId(claim.getId());
        newExpertise.setExpertNom(expertise.getExpertNom());
        newExpertise.setDate(expertise.getDate());
        newExpertise.setMontantEstime(expertise.getMontantEstime());
        newExpertise.setAvis(expertise.getAvis());

        // Save the expertise in the local database.
        Expertise saved = expertiseRepository.save(newExpertise);

        // Persist the claim status and retained amount
        // within the same database transaction.
        claim.setStatut(ClaimStatus.EN_EXPERTISE);
        claim.setMontantRetenu(expertise.getMontantEstime());

        // Trigger the event after saving the expertise.
        // ExpertiseEventProducer publishes to Kafka only after commit.
        eventPublisher.publishEvent(
                new ExpertiseCompletedEvent(claim.getId())
        );

        return saved;
    }

    @Override
    @Transactional(readOnly = true)
    public List<Expertise> getExpertisesByClaimId(Long claimId) {

        if (claimId == null || claimId <= 0) {
            throw new InvalidRequestException("Invalid claim ID");
        }

        if (!claimFileRepository.existsById(claimId)) {
            throw new ResourceNotFoundException("Claim not found: " + claimId);
        }

        return expertiseRepository.findByClaimId(claimId);
    }
}
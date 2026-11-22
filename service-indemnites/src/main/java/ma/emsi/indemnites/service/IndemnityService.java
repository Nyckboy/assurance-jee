package ma.emsi.indemnites.service;

import java.time.LocalDate;
import java.util.List;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import ma.emsi.indemnites.client.ClaimResponse;
import ma.emsi.indemnites.client.ClaimServiceClient;
import ma.emsi.indemnites.client.CoverageResponse;
import ma.emsi.indemnites.client.PolicyServiceClient;
import ma.emsi.indemnites.dto.IndemnityPaidEvent;
import ma.emsi.indemnites.dto.IndemnityRequest;
import ma.emsi.indemnites.dto.IndemnityResponse;
import ma.emsi.indemnites.entity.Indemnity;
import ma.emsi.indemnites.enums.IndemnityStatus;
import ma.emsi.indemnites.exception.ResourceConflictException;
import ma.emsi.indemnites.exception.ResourceNotFoundException;
import ma.emsi.indemnites.outbox.OutboxEvent;
import ma.emsi.indemnites.outbox.OutboxRepository;
import ma.emsi.indemnites.repository.IndemnityRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class IndemnityService {

    private final IndemnityRepository indemnityRepository;
    private final ClaimServiceClient claimServiceClient;
    private final PolicyServiceClient policyServiceClient;
    private final IndemnityCalculator indemnityCalculator;
    private final OutboxRepository outboxRepository;
    private final ObjectMapper objectMapper;

    // Constructeur conservé pour les tests unitaires existants.
    public IndemnityService(
            IndemnityRepository indemnityRepository,
            ClaimServiceClient claimServiceClient,
            PolicyServiceClient policyServiceClient,
            IndemnityCalculator indemnityCalculator) {
        this(
                indemnityRepository,
                claimServiceClient,
                policyServiceClient,
                indemnityCalculator,
                null,
                null
        );
    }

    @Autowired
    public IndemnityService(
            IndemnityRepository indemnityRepository,
            ClaimServiceClient claimServiceClient,
            PolicyServiceClient policyServiceClient,
            IndemnityCalculator indemnityCalculator,
            OutboxRepository outboxRepository,
            ObjectMapper objectMapper) {

        this.indemnityRepository = indemnityRepository;
        this.claimServiceClient = claimServiceClient;
        this.policyServiceClient = policyServiceClient;
        this.indemnityCalculator = indemnityCalculator;
        this.outboxRepository = outboxRepository;
        this.objectMapper = objectMapper;
    }

    public void handleExpertiseCompleted(Long claimId) {
        if (claimId == null || claimId <= 0) {
            throw new IllegalArgumentException(
                    "L'identifiant du sinistre doit être positif."
            );
        }

        if (indemnityRepository.existsByClaimId(claimId)) {
            return;
        }

        calculateIndemnity(claimId);
    }
    public IndemnityResponse calculateIndemnity(Long claimId) {

        if (claimId == null || claimId <= 0) {
            throw new IllegalArgumentException(
                    "L'identifiant du sinistre doit être positif."
            );
        }

        if (indemnityRepository.existsByClaimId(claimId)) {
            throw new ResourceConflictException(
                    "Une indemnité existe déjà pour ce sinistre : " + claimId
            );
        }

        ClaimResponse claim = claimServiceClient.getClaimById(claimId);

        if (claim == null) {
            throw new ResourceNotFoundException(
                    "Le sinistre demandé est introuvable : " + claimId
            );
        }

        if (claim.policyId() == null
                || claim.type() == null
                || claim.type().isBlank()
                || claim.montantRetenu() == null) {
            throw new IllegalArgumentException(
                    "Le sinistre ne contient pas toutes les informations nécessaires au calcul."
            );
        }

        CoverageResponse coverage = policyServiceClient.verifyCoverage(
                claim.policyId(),
                claim.type()
        );

        if (coverage == null) {
            throw new IllegalArgumentException(
                    "Le service des polices n'a retourné aucune information de couverture."
            );
        }

        if (!coverage.covered()) {
            throw new IllegalArgumentException(
                    "Le sinistre n'est pas couvert par la police : "
                            + coverage.reason()
            );
        }

        if (coverage.plafond() == null || coverage.franchise() == null) {
            throw new IllegalArgumentException(
                    "Le plafond ou la franchise de la garantie est manquant."
            );
        }

        var montantCalcule = indemnityCalculator.calculate(
                claim.montantRetenu(),
                coverage.plafond(),
                coverage.franchise()
        );

        Indemnity indemnity = new Indemnity();
        indemnity.setClaimId(claimId);
        indemnity.setMontantCalcule(montantCalcule);
        indemnity.setFranchiseAppliquee(coverage.franchise());
        indemnity.setStatut(IndemnityStatus.CALCULEE);

        return toResponse(indemnityRepository.save(indemnity));
    }

    public IndemnityResponse createIndemnity(IndemnityRequest request) {

        if (indemnityRepository.existsByClaimId(request.getClaimId())) {
            throw new ResourceConflictException(
                    "Une indemnité existe déjà pour ce sinistre : " + request.getClaimId()
            );
        }

        Indemnity indemnity = new Indemnity();
        indemnity.setClaimId(request.getClaimId());
        indemnity.setMontantCalcule(request.getMontantCalcule());
        indemnity.setFranchiseAppliquee(request.getFranchiseAppliquee());
        indemnity.setStatut(IndemnityStatus.EN_ATTENTE);

        return toResponse(indemnityRepository.save(indemnity));
    }

    public IndemnityResponse confirmPayment(Long id) {

        if (outboxRepository == null || objectMapper == null) {
            throw new IllegalStateException(
                    "Le mécanisme Outbox n'est pas configuré."
            );
        }

        Indemnity indemnity = findIndemnityById(id);

        // Évite de publier plusieurs événements pour le même paiement.
        if (indemnity.getStatut() == IndemnityStatus.PAYEE) {
            return toResponse(indemnity);
        }

        if (indemnity.getStatut() != IndemnityStatus.CALCULEE) {
            throw new ResourceConflictException(
                    "Seule une indemnité calculée peut être confirmée comme payée."
            );
        }

        // Récupération du policyId officiel auprès du service des sinistres.
        ClaimResponse claim =
                claimServiceClient.getClaimById(indemnity.getClaimId());

        if (claim == null || claim.policyId() == null) {
            throw new IllegalArgumentException(
                    "Impossible de récupérer la police associée au sinistre."
            );
        }

        LocalDate dateVirement = LocalDate.now();

        indemnity.setDateVirement(dateVirement);
        indemnity.setStatut(IndemnityStatus.PAYEE);

        Indemnity saved = indemnityRepository.save(indemnity);

        IndemnityPaidEvent event = new IndemnityPaidEvent(
                saved.getId(),
                saved.getClaimId(),
                claim.policyId(),
                saved.getMontantCalcule(),
                saved.getDateVirement()
        );

        try {
            String payload = objectMapper.writeValueAsString(event);

            outboxRepository.save(new OutboxEvent(
                    "indemnity-paid",
                    saved.getClaimId().toString(),
                    payload
            ));
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException(
                    "Impossible de créer l'événement indemnity-paid.",
                    exception
            );
        }

        return toResponse(saved);
    }

    @Transactional(readOnly = true)
    public List<IndemnityResponse> getAllIndemnities() {
        return indemnityRepository.findAll()
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public IndemnityResponse getIndemnityById(Long id) {
        return toResponse(findIndemnityById(id));
    }

    public IndemnityResponse updateIndemnity(Long id, IndemnityRequest request) {

        Indemnity indemnity = findIndemnityById(id);

        if (indemnity.getStatut() == IndemnityStatus.PAYEE) {
            throw new ResourceConflictException(
                    "Une indemnité déjà payée ne peut pas être modifiée."
            );
        }

        if (!indemnity.getClaimId().equals(request.getClaimId())
                && indemnityRepository.existsByClaimId(request.getClaimId())) {
            throw new ResourceConflictException(
                    "Une indemnité existe déjà pour ce sinistre : " + request.getClaimId()
            );
        }

        indemnity.setClaimId(request.getClaimId());
        indemnity.setMontantCalcule(request.getMontantCalcule());
        indemnity.setFranchiseAppliquee(request.getFranchiseAppliquee());

        return toResponse(indemnityRepository.save(indemnity));
    }

    public void deleteIndemnity(Long id) {
        Indemnity indemnity = findIndemnityById(id);

        if (indemnity.getStatut() == IndemnityStatus.PAYEE) {
            throw new ResourceConflictException(
                    "Une indemnité déjà payée ne peut pas être supprimée."
            );
        }

        indemnityRepository.delete(indemnity);
    }

    private Indemnity findIndemnityById(Long id) {
        return indemnityRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Indemnité introuvable avec l'identifiant : " + id
                ));
    }

    private IndemnityResponse toResponse(Indemnity indemnity) {
        return new IndemnityResponse(
                indemnity.getId(),
                indemnity.getClaimId(),
                indemnity.getMontantCalcule(),
                indemnity.getFranchiseAppliquee(),
                indemnity.getDateVirement(),
                indemnity.getStatut()
        );
    }
}
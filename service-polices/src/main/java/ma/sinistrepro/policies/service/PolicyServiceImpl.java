package ma.sinistrepro.policies.service;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import ma.sinistrepro.policies.api.CoverageVerificationResponse;
import ma.sinistrepro.policies.api.GuaranteeRequest;
import ma.sinistrepro.policies.api.GuaranteeResponse;
import ma.sinistrepro.policies.api.PolicyClaimResponse;
import ma.sinistrepro.policies.api.PolicyRequest;
import ma.sinistrepro.policies.api.PolicyResponse;
import ma.sinistrepro.policies.api.PremiumAdjustmentResponse;
import ma.sinistrepro.policies.domain.Guarantee;
import ma.sinistrepro.policies.domain.Policy;
import ma.sinistrepro.policies.domain.PolicyClaim;
import ma.sinistrepro.policies.domain.PolicyStatus;
import ma.sinistrepro.policies.domain.PremiumAdjustment;
import ma.sinistrepro.policies.event.ClaimDeclaredEvent;
import ma.sinistrepro.policies.event.IndemnityPaidEvent;
import ma.sinistrepro.policies.notification.ClaimRecordedNotification;
import ma.sinistrepro.policies.notification.PremiumIncreasedNotification;
import ma.sinistrepro.policies.repository.GuaranteeRepository;
import ma.sinistrepro.policies.repository.PolicyClaimRepository;
import ma.sinistrepro.policies.repository.PolicyRepository;
import ma.sinistrepro.policies.repository.PremiumAdjustmentRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class PolicyServiceImpl implements PolicyService {

    private static final Logger log = LoggerFactory.getLogger(PolicyServiceImpl.class);

    private final PolicyRepository policyRepository;
    private final GuaranteeRepository guaranteeRepository;
    private final PolicyClaimRepository policyClaimRepository;
    private final PremiumAdjustmentRepository premiumAdjustmentRepository;
    private final BigDecimal premiumIncreaseRate;
    private final ApplicationEventPublisher eventPublisher;

    public PolicyServiceImpl(PolicyRepository policyRepository, GuaranteeRepository guaranteeRepository,
                             PolicyClaimRepository policyClaimRepository,
                             PremiumAdjustmentRepository premiumAdjustmentRepository,
                             @Value("${policies.premium.increase-rate-per-paid-claim:0.10}")
                             BigDecimal premiumIncreaseRate,
                             ApplicationEventPublisher eventPublisher) {
        this.policyRepository = policyRepository;
        this.guaranteeRepository = guaranteeRepository;
        this.policyClaimRepository = policyClaimRepository;
        this.premiumAdjustmentRepository = premiumAdjustmentRepository;
        this.premiumIncreaseRate = premiumIncreaseRate;
        this.eventPublisher = eventPublisher;
    }

    @Override
    public PolicyResponse createPolicy(PolicyRequest request) {
        Policy policy = new Policy(request.souscripteurEmail(), request.typeCouverture(), request.montantCouvert(),
                request.prime(), request.dateEffet(), request.statut());
        return toPolicyResponse(policyRepository.save(policy));
    }

    @Override
    @Transactional(readOnly = true)
    public List<PolicyResponse> findAllPolicies() {
        return policyRepository.findAll().stream().map(this::toPolicyResponse).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public PolicyResponse findPolicy(Long policyId) {
        return toPolicyResponse(findPolicyEntity(policyId));
    }

    @Override
    public PolicyResponse updatePolicy(Long policyId, PolicyRequest request) {
        Policy policy = findPolicyEntity(policyId);
        policy.update(request.souscripteurEmail(), request.typeCouverture(), request.montantCouvert(), request.prime(),
                request.dateEffet(), request.statut());
        return toPolicyResponse(policy);
    }

    @Override
    public void deletePolicy(Long policyId) {
        policyRepository.delete(findPolicyEntity(policyId));
    }

    @Override
    public GuaranteeResponse addGuarantee(Long policyId, GuaranteeRequest request) {
        Policy policy = findPolicyEntity(policyId);
        Guarantee guarantee = new Guarantee(request.libelle(), request.plafond(), request.franchise());
        policy.addGuarantee(guarantee);
        // Saving the guarantee itself inserts it now, so the response carries its generated id.
        // (Saving the already-loaded policy would only insert it at commit, after the response is built.)
        return toGuaranteeResponse(guaranteeRepository.save(guarantee));
    }

    @Override
    public GuaranteeResponse updateGuarantee(Long policyId, Long guaranteeId, GuaranteeRequest request) {
        Guarantee guarantee = findGuaranteeEntity(policyId, guaranteeId);
        guarantee.update(request.libelle(), request.plafond(), request.franchise());
        return toGuaranteeResponse(guarantee);
    }

    @Override
    public void deleteGuarantee(Long policyId, Long guaranteeId) {
        Policy policy = findPolicyEntity(policyId);
        Guarantee guarantee = findGuaranteeEntity(policyId, guaranteeId);
        policy.removeGuarantee(guarantee);
    }

    @Override
    @Transactional(readOnly = true)
    public CoverageVerificationResponse verifyCoverage(Long policyId, String claimType) {
        Policy policy = findPolicyEntity(policyId);
        if (policy.getStatut() != PolicyStatus.ACTIVE) {
            return new CoverageVerificationResponse(policyId, claimType, false, "Policy is not active", null, null);
        }

        return guaranteeRepository.findByPolicyIdAndLibelleIgnoreCase(policyId, claimType)
                .map(guarantee -> new CoverageVerificationResponse(policyId, claimType, true, "Covered",
                        guarantee.getPlafond(), guarantee.getFranchise()))
                .orElseGet(() -> new CoverageVerificationResponse(policyId, claimType, false,
                        "No matching guarantee", null, null));
    }

    @Override
    public boolean recordDeclaredClaim(ClaimDeclaredEvent event) {
        if (!policyRepository.existsById(event.policyId())) {
            log.warn("Ignoring claim {}: policy {} does not exist", event.claimId(), event.policyId());
            return false;
        }
        if (policyClaimRepository.existsByClaimId(event.claimId())) {
            log.info("Ignoring claim {}: already recorded", event.claimId());
            return false;
        }

        policyClaimRepository.save(new PolicyClaim(event.claimId(), event.policyId(), event.souscripteurEmail(),
                event.dateSinistre(), event.type(), event.montantDeclare(), Instant.now()));
        log.info("Claim {} ({}, {}) recorded on policy {}", event.claimId(), event.type(), event.dateSinistre(),
                event.policyId());
        // Emailed to the policyholder once this transaction commits.
        eventPublisher.publishEvent(new ClaimRecordedNotification(event.souscripteurEmail(), event.policyId(),
                event.claimId(), event.type(), event.dateSinistre(), event.montantDeclare()));
        return true;
    }

    @Override
    @Transactional(readOnly = true)
    public List<PolicyClaimResponse> findPolicyClaims(Long policyId) {
        findPolicyEntity(policyId);
        return policyClaimRepository.findByPolicyIdOrderByDateSinistreDesc(policyId).stream()
                .map(claim -> new PolicyClaimResponse(claim.getClaimId(), claim.getDateSinistre(), claim.getType(),
                        claim.getMontantDeclare(), claim.getReceivedAt()))
                .toList();
    }

    @Override
    public boolean applyIndemnityPaid(IndemnityPaidEvent event) {
        Policy policy = policyRepository.findById(event.policyId()).orElse(null);
        if (policy == null) {
            log.warn("Ignoring indemnity {} for claim {}: policy {} does not exist", event.indemnityId(),
                    event.claimId(), event.policyId());
            return false;
        }
        if (premiumAdjustmentRepository.existsByClaimId(event.claimId())) {
            log.info("Ignoring indemnity {}: claim {} was already applied to the premium", event.indemnityId(),
                    event.claimId());
            return false;
        }

        BigDecimal primeAvant = policy.getPrime();
        BigDecimal primeApres = policy.increasePrime(premiumIncreaseRate);
        premiumAdjustmentRepository.save(new PremiumAdjustment(event.claimId(), event.indemnityId(), policy.getId(),
                event.montantCalcule(), event.dateVirement(), primeAvant, primeApres, Instant.now()));
        log.info("Indemnity of {} paid for claim {}: premium of policy {} goes from {} to {}", event.montantCalcule(),
                event.claimId(), policy.getId(), primeAvant, primeApres);
        // Emailed to the policyholder once this transaction commits.
        eventPublisher.publishEvent(new PremiumIncreasedNotification(policy.getSouscripteurEmail(), policy.getId(),
                event.claimId(), event.montantCalcule(), event.dateVirement(), primeAvant, primeApres));
        return true;
    }

    @Override
    @Transactional(readOnly = true)
    public List<PremiumAdjustmentResponse> findPremiumAdjustments(Long policyId) {
        findPolicyEntity(policyId);
        return premiumAdjustmentRepository.findByPolicyIdOrderByAppliedAtAsc(policyId).stream()
                .map(adjustment -> new PremiumAdjustmentResponse(adjustment.getClaimId(),
                        adjustment.getIndemnityId(), adjustment.getMontantIndemnise(), adjustment.getDateVirement(),
                        adjustment.getPrimeAvant(), adjustment.getPrimeApres(), adjustment.getAppliedAt()))
                .toList();
    }

    private Policy findPolicyEntity(Long policyId) {
        return policyRepository.findById(policyId)
                .orElseThrow(() -> new ResourceNotFoundException("Policy " + policyId + " was not found"));
    }

    private Guarantee findGuaranteeEntity(Long policyId, Long guaranteeId) {
        return guaranteeRepository.findByIdAndPolicyId(guaranteeId, policyId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Guarantee " + guaranteeId + " was not found for policy " + policyId));
    }

    private PolicyResponse toPolicyResponse(Policy policy) {
        List<GuaranteeResponse> guarantees = policy.getGuarantees().stream().map(this::toGuaranteeResponse).toList();
        return new PolicyResponse(policy.getId(), policy.getSouscripteurEmail(), policy.getTypeCouverture(),
                policy.getMontantCouvert(), policy.getPrime(), policy.getDateEffet(), policy.getStatut(), guarantees);
    }

    private GuaranteeResponse toGuaranteeResponse(Guarantee guarantee) {
        return new GuaranteeResponse(guarantee.getId(), guarantee.getLibelle(), guarantee.getPlafond(),
                guarantee.getFranchise());
    }
}

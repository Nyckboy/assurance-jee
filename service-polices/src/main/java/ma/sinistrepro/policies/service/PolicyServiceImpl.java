package ma.sinistrepro.policies.service;

import java.time.Instant;
import java.util.List;
import ma.sinistrepro.policies.api.CoverageVerificationResponse;
import ma.sinistrepro.policies.api.GuaranteeRequest;
import ma.sinistrepro.policies.api.GuaranteeResponse;
import ma.sinistrepro.policies.api.PolicyClaimResponse;
import ma.sinistrepro.policies.api.PolicyRequest;
import ma.sinistrepro.policies.api.PolicyResponse;
import ma.sinistrepro.policies.domain.Guarantee;
import ma.sinistrepro.policies.domain.Policy;
import ma.sinistrepro.policies.domain.PolicyClaim;
import ma.sinistrepro.policies.domain.PolicyStatus;
import ma.sinistrepro.policies.event.ClaimDeclaredEvent;
import ma.sinistrepro.policies.repository.GuaranteeRepository;
import ma.sinistrepro.policies.repository.PolicyClaimRepository;
import ma.sinistrepro.policies.repository.PolicyRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class PolicyServiceImpl implements PolicyService {

    private static final Logger log = LoggerFactory.getLogger(PolicyServiceImpl.class);

    private final PolicyRepository policyRepository;
    private final GuaranteeRepository guaranteeRepository;
    private final PolicyClaimRepository policyClaimRepository;

    public PolicyServiceImpl(PolicyRepository policyRepository, GuaranteeRepository guaranteeRepository,
                             PolicyClaimRepository policyClaimRepository) {
        this.policyRepository = policyRepository;
        this.guaranteeRepository = guaranteeRepository;
        this.policyClaimRepository = policyClaimRepository;
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
        policyRepository.save(policy);
        return toGuaranteeResponse(guarantee);
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
        // No mail server yet: the notification to the policyholder is logged.
        log.info("Notification to {}: claim {} ({}, {}) was declared on policy {}", event.souscripteurEmail(),
                event.claimId(), event.type(), event.dateSinistre(), event.policyId());
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

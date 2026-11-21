package ma.sinistrepro.policies.service;

import java.util.List;
import ma.sinistrepro.policies.api.CoverageVerificationResponse;
import ma.sinistrepro.policies.api.GuaranteeRequest;
import ma.sinistrepro.policies.api.GuaranteeResponse;
import ma.sinistrepro.policies.api.PolicyClaimResponse;
import ma.sinistrepro.policies.api.PolicyRequest;
import ma.sinistrepro.policies.api.PolicyResponse;
import ma.sinistrepro.policies.event.ClaimDeclaredEvent;

public interface PolicyService {

    PolicyResponse createPolicy(PolicyRequest request);

    List<PolicyResponse> findAllPolicies();

    PolicyResponse findPolicy(Long policyId);

    PolicyResponse updatePolicy(Long policyId, PolicyRequest request);

    void deletePolicy(Long policyId);

    GuaranteeResponse addGuarantee(Long policyId, GuaranteeRequest request);

    GuaranteeResponse updateGuarantee(Long policyId, Long guaranteeId, GuaranteeRequest request);

    void deleteGuarantee(Long policyId, Long guaranteeId);

    CoverageVerificationResponse verifyCoverage(Long policyId, String claimType);

    /**
     * Records a declared claim on its policy and notifies the policyholder.
     *
     * @return false when the event was ignored (unknown policy or claim already recorded)
     */
    boolean recordDeclaredClaim(ClaimDeclaredEvent event);

    List<PolicyClaimResponse> findPolicyClaims(Long policyId);
}

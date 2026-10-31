package ma.sinistrepro.policies.service;

import java.util.List;
import ma.sinistrepro.policies.api.CoverageVerificationResponse;
import ma.sinistrepro.policies.api.GuaranteeRequest;
import ma.sinistrepro.policies.api.GuaranteeResponse;
import ma.sinistrepro.policies.api.PolicyRequest;
import ma.sinistrepro.policies.api.PolicyResponse;

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
}

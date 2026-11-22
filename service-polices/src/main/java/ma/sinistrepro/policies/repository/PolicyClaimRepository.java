package ma.sinistrepro.policies.repository;

import java.util.List;
import ma.sinistrepro.policies.domain.PolicyClaim;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PolicyClaimRepository extends JpaRepository<PolicyClaim, Long> {

    boolean existsByClaimId(Long claimId);

    List<PolicyClaim> findByPolicyIdOrderByDateSinistreDesc(Long policyId);
}

package ma.sinistrepro.policies.repository;

import java.util.List;
import ma.sinistrepro.policies.domain.PremiumAdjustment;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PremiumAdjustmentRepository extends JpaRepository<PremiumAdjustment, Long> {

    boolean existsByClaimId(Long claimId);

    List<PremiumAdjustment> findByPolicyIdOrderByAppliedAtAsc(Long policyId);
}

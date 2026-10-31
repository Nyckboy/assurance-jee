package ma.sinistrepro.policies.repository;

import java.util.Optional;
import ma.sinistrepro.policies.domain.Guarantee;
import org.springframework.data.jpa.repository.JpaRepository;

public interface GuaranteeRepository extends JpaRepository<Guarantee, Long> {

    Optional<Guarantee> findByIdAndPolicyId(Long id, Long policyId);

    Optional<Guarantee> findByPolicyIdAndLibelleIgnoreCase(Long policyId, String libelle);
}

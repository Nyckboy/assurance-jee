package ma.sinistrepro.policies.repository;

import ma.sinistrepro.policies.domain.Policy;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PolicyRepository extends JpaRepository<Policy, Long> {
}

package ma.emsi.indemnites.repository;

import ma.emsi.indemnites.entity.Indemnity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface IndemnityRepository extends JpaRepository<Indemnity, Long> {

    Optional<Indemnity> findByClaimId(Long claimId);

    boolean existsByClaimId(Long claimId);
}

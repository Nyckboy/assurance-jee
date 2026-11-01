
package ma.sinistrepro.claims.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import ma.sinistrepro.claims.domain.Expertise;

public interface ExpertiseRepository
        extends JpaRepository<Expertise, Long> {

    // Retrieve all expert assessments for a claim.
    List<Expertise> findByClaimId(Long claimId);
}

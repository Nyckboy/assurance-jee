
package ma.sinistrepro.claims.repository;

import ma.sinistrepro.claims.domain.Expertise;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ExpertiseRepository
        extends JpaRepository<Expertise, Long> {
}

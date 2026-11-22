
package ma.sinistrepro.claims.repository;

import ma.sinistrepro.claims.domain.ClaimFile;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ClaimFileRepository
        extends JpaRepository<ClaimFile, Long> {
}

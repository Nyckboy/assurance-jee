
package ma.sinistrepro.claims.service;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import ma.sinistrepro.claims.domain.ClaimFile;
import ma.sinistrepro.claims.repository.ClaimFileRepository;

@Service
public class ClaimServiceImpl implements ClaimService {

    private final ClaimFileRepository claimFileRepository;

    public ClaimServiceImpl(ClaimFileRepository claimFileRepository) {
        this.claimFileRepository = claimFileRepository;
    }

    @Override
    public List<ClaimFile> getAllClaims() {
        // Fetch claim files from the service's own database.
        return claimFileRepository.findAll();
    }

    @Override
    public ClaimFile getClaimById(Long id) {
        // Reject invalid identifiers before querying the database.
        if (id == null || id <= 0) {
            throw new ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                "Invalid claim ID"
            );
        }

        // Return HTTP 404 when the claim does not exist.
        return claimFileRepository.findById(id)
            .orElseThrow(() -> new ResponseStatusException(
                HttpStatus.NOT_FOUND,
                "Claim not found: " + id
            ));
    }
}

package ma.sinistrepro.claims.api;

import jakarta.validation.Valid;
import java.net.URI;
import java.util.List;
import ma.sinistrepro.claims.domain.ClaimFile;
import ma.sinistrepro.claims.service.ClaimService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/claims")
public class ClaimController {

    private final ClaimService claimService;

    public ClaimController(ClaimService claimService) {
        this.claimService = claimService;
    }

    // Retrieve every registered claim file.
    @GetMapping
    public List<ClaimResponse> getAllClaims() {
        return claimService.getAllClaims().stream().map(ClaimResponse::from).toList();
    }

    // Retrieve one claim file by its identifier.
    @GetMapping("/{id}")
    public ClaimResponse getClaimById(@PathVariable Long id) {
        return ClaimResponse.from(claimService.getClaimById(id));
    }

    // Create a claim after insurance coverage verification.
    @PostMapping
    public ResponseEntity<ClaimResponse> createClaim(@Valid @RequestBody ClaimRequest request) {
        ClaimFile created = claimService.createClaim(request.toClaimFile());
        return ResponseEntity
                .created(URI.create("/api/claims/" + created.getId()))
                .body(ClaimResponse.from(created));
    }
}


package ma.sinistrepro.claims.api;

import java.util.List;
import java.net.URI;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import ma.sinistrepro.claims.domain.ClaimFile;
import ma.sinistrepro.claims.service.ClaimService;



@RestController
@RequestMapping("/api/claims")
public class ClaimController {

    private final ClaimService claimService;

    public ClaimController(ClaimService claimService) {
        this.claimService = claimService;
    }

    // Retrieve every registered claim file.
    @GetMapping
    public ResponseEntity<List<ClaimFile>> getAllClaims() {
        return ResponseEntity.ok(claimService.getAllClaims());
    }

    // Retrieve one claim file by its identifier.
    @GetMapping("/{id}")
    public ResponseEntity<ClaimFile> getClaimById(
            @PathVariable Long id) {
        return ResponseEntity.ok(claimService.getClaimById(id));
    }


// Create a claim after insurance coverage verification.
@PostMapping
public ResponseEntity<ClaimFile> createClaim(
        @RequestBody ClaimFile claim) {

    ClaimFile created = claimService.createClaim(claim);

    return ResponseEntity
        .created(URI.create("/api/claims/" + created.getId()))
        .body(created);
}

}

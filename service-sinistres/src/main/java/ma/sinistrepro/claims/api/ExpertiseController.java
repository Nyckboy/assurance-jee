
package ma.sinistrepro.claims.api;

import java.net.URI;
import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import ma.sinistrepro.claims.domain.Expertise;
import ma.sinistrepro.claims.service.ExpertiseService;

@RestController
@RequestMapping("/api/claims")
public class ExpertiseController {

    private final ExpertiseService expertiseService;

    public ExpertiseController(ExpertiseService expertiseService) {
        this.expertiseService = expertiseService;
    }

    // Register a new expertise for an existing claim.
    @PostMapping("/{claimId}/expertises")
    public ResponseEntity<Expertise> createExpertise(
            @PathVariable Long claimId,
            @RequestBody Expertise expertise) {

        // The URL identifies the claim, not the request body.
        if (expertise == null) {
            return ResponseEntity.badRequest().build();
        }

        expertise.setClaimId(claimId);

        Expertise created = expertiseService.createExpertise(expertise);

        return ResponseEntity
            .created(URI.create(
                "/api/claims/" + claimId
                + "/expertises/" + created.getId()))
            .body(created);
    }

    // Retrieve all expertises associated with a claim.
    @GetMapping("/{claimId}/expertises")
    public ResponseEntity<List<Expertise>> getExpertises(
            @PathVariable Long claimId) {

        return ResponseEntity.ok(
            expertiseService.getExpertisesByClaimId(claimId)
        );
    }
}

package ma.sinistrepro.claims.api;

import jakarta.validation.Valid;
import java.net.URI;
import java.util.List;
import ma.sinistrepro.claims.domain.Expertise;
import ma.sinistrepro.claims.service.ExpertiseService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/claims")
public class ExpertiseController {

    private final ExpertiseService expertiseService;

    public ExpertiseController(ExpertiseService expertiseService) {
        this.expertiseService = expertiseService;
    }

    // Register a new expertise for an existing claim. The URL identifies the claim.
    @PostMapping("/{claimId}/expertises")
    public ResponseEntity<ExpertiseResponse> createExpertise(@PathVariable Long claimId,
                                                             @Valid @RequestBody ExpertiseRequest request) {
        Expertise created = expertiseService.createExpertise(request.toExpertise(claimId));
        return ResponseEntity
                .created(URI.create("/api/claims/" + claimId + "/expertises/" + created.getId()))
                .body(ExpertiseResponse.from(created));
    }

    // Retrieve all expertises associated with a claim.
    @GetMapping("/{claimId}/expertises")
    public List<ExpertiseResponse> getExpertises(@PathVariable Long claimId) {
        return expertiseService.getExpertisesByClaimId(claimId).stream().map(ExpertiseResponse::from).toList();
    }
}

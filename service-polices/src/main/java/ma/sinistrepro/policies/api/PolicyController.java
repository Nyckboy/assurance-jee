package ma.sinistrepro.policies.api;

import jakarta.validation.Valid;
import java.net.URI;
import java.util.List;
import ma.sinistrepro.policies.service.PolicyService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/policies")
public class PolicyController {

    private final PolicyService policyService;

    public PolicyController(PolicyService policyService) {
        this.policyService = policyService;
    }

    @PostMapping
    public ResponseEntity<PolicyResponse> createPolicy(@Valid @RequestBody PolicyRequest request) {
        PolicyResponse created = policyService.createPolicy(request);
        return ResponseEntity.created(URI.create("/api/policies/" + created.id())).body(created);
    }

    @GetMapping
    public List<PolicyResponse> findAllPolicies() {
        return policyService.findAllPolicies();
    }

    @GetMapping("/{policyId}")
    public PolicyResponse findPolicy(@PathVariable Long policyId) {
        return policyService.findPolicy(policyId);
    }

    @PutMapping("/{policyId}")
    public PolicyResponse updatePolicy(@PathVariable Long policyId, @Valid @RequestBody PolicyRequest request) {
        return policyService.updatePolicy(policyId, request);
    }

    @DeleteMapping("/{policyId}")
    public ResponseEntity<Void> deletePolicy(@PathVariable Long policyId) {
        policyService.deletePolicy(policyId);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{policyId}/guarantees")
    public ResponseEntity<GuaranteeResponse> addGuarantee(@PathVariable Long policyId,
                                                            @Valid @RequestBody GuaranteeRequest request) {
        GuaranteeResponse created = policyService.addGuarantee(policyId, request);
        return ResponseEntity.created(URI.create("/api/policies/" + policyId + "/guarantees/" + created.id()))
                .body(created);
    }

    @PutMapping("/{policyId}/guarantees/{guaranteeId}")
    public GuaranteeResponse updateGuarantee(@PathVariable Long policyId, @PathVariable Long guaranteeId,
                                             @Valid @RequestBody GuaranteeRequest request) {
        return policyService.updateGuarantee(policyId, guaranteeId, request);
    }

    @DeleteMapping("/{policyId}/guarantees/{guaranteeId}")
    public ResponseEntity<Void> deleteGuarantee(@PathVariable Long policyId, @PathVariable Long guaranteeId) {
        policyService.deleteGuarantee(policyId, guaranteeId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{policyId}/coverage")
    public CoverageVerificationResponse verifyCoverage(@PathVariable Long policyId,
                                                       @RequestParam String claimType) {
        return policyService.verifyCoverage(policyId, claimType);
    }
}

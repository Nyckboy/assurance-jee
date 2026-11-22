package ma.emsi.indemnites.controller;

import jakarta.validation.Valid;
import ma.emsi.indemnites.dto.IndemnityRequest;
import ma.emsi.indemnites.dto.IndemnityResponse;
import ma.emsi.indemnites.service.IndemnityService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/indemnities")
public class IndemnityController {

    private final IndemnityService indemnityService;

    public IndemnityController(IndemnityService indemnityService) {
        this.indemnityService = indemnityService;
    }

    @PostMapping("/calculate/{claimId}")
    public ResponseEntity<IndemnityResponse> calculateIndemnity(
            @PathVariable Long claimId) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(indemnityService.calculateIndemnity(claimId));
    }

    @PostMapping
    public ResponseEntity<IndemnityResponse> createIndemnity(
            @Valid @RequestBody IndemnityRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(indemnityService.createIndemnity(request));
    }

    @PostMapping("/{id}/pay")
    public ResponseEntity<IndemnityResponse> confirmPayment(
            @PathVariable Long id) {
        return ResponseEntity.ok(indemnityService.confirmPayment(id));
    }

    @GetMapping
    public ResponseEntity<List<IndemnityResponse>> getAllIndemnities() {
        return ResponseEntity.ok(indemnityService.getAllIndemnities());
    }

    @GetMapping("/{id}")
    public ResponseEntity<IndemnityResponse> getIndemnityById(
            @PathVariable Long id) {
        return ResponseEntity.ok(indemnityService.getIndemnityById(id));
    }

    @PutMapping("/{id}")
    public ResponseEntity<IndemnityResponse> updateIndemnity(
            @PathVariable Long id,
            @Valid @RequestBody IndemnityRequest request) {
        return ResponseEntity.ok(
                indemnityService.updateIndemnity(id, request)
        );
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteIndemnity(@PathVariable Long id) {
        indemnityService.deleteIndemnity(id);
        return ResponseEntity.noContent().build();
    }
}
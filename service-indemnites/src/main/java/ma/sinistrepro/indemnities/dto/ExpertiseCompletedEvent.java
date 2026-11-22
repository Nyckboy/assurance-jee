package ma.sinistrepro.indemnities.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record ExpertiseCompletedEvent(
        Long claimId
) {
}

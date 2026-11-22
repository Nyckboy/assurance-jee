package ma.sinistrepro.claims;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasKey;
import static org.hamcrest.Matchers.not;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import feign.FeignException;
import feign.Request;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import ma.sinistrepro.claims.client.CoverageResponse;
import ma.sinistrepro.claims.client.PolicyClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

/**
 * HTTP contract of the claims API: request DTOs with @Valid, response DTOs (read by service-indemnites)
 * and the error responses of ApiExceptionHandler.
 */
@SpringBootTest
@EmbeddedKafka(partitions = 1, topics = {"claim-declared", "expertise-completed"},
        bootstrapServersProperty = "spring.kafka.bootstrap-servers")
class ClaimApiTests {

    private static final String CLAIM = """
            {"policyId":3,"souscripteurEmail":"insured@example.com","dateSinistre":"2026-11-01",
             "type":"COLLISION","montantDeclare":4200.00}
            """;
    private static final String EXPERTISE = """
            {"expertNom":"Expert A","date":"2026-11-05","montantEstime":4000.00,"avis":"Choc avant, réparable"}
            """;

    @Autowired
    private WebApplicationContext context;

    @MockitoBean
    private PolicyClient policyClient;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(context).build();
    }

    @Test
    void declaredClaimIsReturnedWithTheFieldsReadByServiceIndemnites() throws Exception {
        coverCollision();

        String location = postJson("/api/claims", CLAIM)
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", containsString("/api/claims/")))
                .andReturn().getResponse().getHeader("Location");

        mockMvc.perform(get(location))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.policyId").value(3))
                .andExpect(jsonPath("$.souscripteurEmail").value("insured@example.com"))
                .andExpect(jsonPath("$.dateSinistre").value("2026-11-01"))
                .andExpect(jsonPath("$.type").value("COLLISION"))
                .andExpect(jsonPath("$.montantDeclare").value(4200.00))
                .andExpect(jsonPath("$.montantRetenu").doesNotExist())
                .andExpect(jsonPath("$.statut").value("DECLARE"));
    }

    @Test
    void callerCannotChooseTheStatusOrRetainedAmount() throws Exception {
        coverCollision();

        postJson("/api/claims", """
                {"policyId":3,"souscripteurEmail":"insured@example.com","dateSinistre":"2026-11-01",
                 "type":"COLLISION","montantDeclare":4200.00,"statut":"INDEMNISE","montantRetenu":99999}
                """)
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.statut").value("DECLARE"))
                .andExpect(jsonPath("$.montantRetenu").doesNotExist());
    }

    @Test
    void invalidClaimListsEveryInvalidField() throws Exception {
        postJson("/api/claims", """
                {"policyId":-1,"souscripteurEmail":"not-an-email","type":"","montantDeclare":0}
                """)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.message", containsString("policyId")))
                .andExpect(jsonPath("$.message", containsString("souscripteurEmail")))
                .andExpect(jsonPath("$.message", containsString("dateSinistre")))
                .andExpect(jsonPath("$.message", containsString("type")))
                .andExpect(jsonPath("$.message", containsString("montantDeclare")));
    }

    @Test
    void malformedJsonIsABadRequest() throws Exception {
        postJson("/api/claims", "{not json")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Malformed JSON request body"));
    }

    @Test
    void uncoveredClaimIsRejectedWithTheReason() throws Exception {
        when(policyClient.verifyCoverage(anyLong(), anyString()))
                .thenReturn(new CoverageResponse(3L, "COLLISION", false, "No matching guarantee", null, null));

        postJson("/api/claims", CLAIM)
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.message", containsString("No matching guarantee")));
    }

    @Test
    void unknownPolicyIsABadRequest() throws Exception {
        when(policyClient.verifyCoverage(anyLong(), anyString())).thenThrow(feignError(404));

        postJson("/api/claims", CLAIM)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Insurance policy not found"));
    }

    @Test
    void unreachablePolicyServiceIsServiceUnavailable() throws Exception {
        when(policyClient.verifyCoverage(anyLong(), anyString())).thenThrow(feignError(503));

        postJson("/api/claims", CLAIM)
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.message").value("Policy verification unavailable"));
    }

    @Test
    void unknownClaimIsNotFound() throws Exception {
        mockMvc.perform(get("/api/claims/999999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("Not Found"))
                .andExpect(jsonPath("$.message").value("Claim not found: 999999"));
    }

    @Test
    void expertiseIsRecordedOnceThenTheClaimCannotEnterExpertiseAgain() throws Exception {
        coverCollision();
        String location = postJson("/api/claims", CLAIM).andReturn().getResponse().getHeader("Location");

        postJson(location + "/expertises", EXPERTISE)
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.expertNom").value("Expert A"))
                .andExpect(jsonPath("$.montantEstime").value(4000.00))
                .andExpect(jsonPath("$", not(hasKey("claim"))));

        mockMvc.perform(get(location))
                .andExpect(jsonPath("$.statut").value("EN_EXPERTISE"))
                .andExpect(jsonPath("$.montantRetenu").value(4000.00));

        postJson(location + "/expertises", EXPERTISE)
                .andExpect(status().isConflict());
    }

    @Test
    void invalidExpertiseIsABadRequest() throws Exception {
        coverCollision();
        String location = postJson("/api/claims", CLAIM).andReturn().getResponse().getHeader("Location");

        postJson(location + "/expertises", """
                {"expertNom":"","montantEstime":-1}
                """)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", containsString("expertNom")))
                .andExpect(jsonPath("$.message", containsString("avis")));
    }

    private void coverCollision() {
        when(policyClient.verifyCoverage(eq(3L), eq("COLLISION")))
                .thenReturn(new CoverageResponse(3L, "COLLISION", true, "Covered",
                        new BigDecimal("15000.00"), new BigDecimal("500.00")));
    }

    private ResultActions postJson(String url, String body) throws Exception {
        return mockMvc.perform(post(url).contentType(MediaType.APPLICATION_JSON).content(body));
    }

    private static FeignException feignError(int status) {
        Request request = Request.create(Request.HttpMethod.GET, "/api/policies/3/coverage", Map.of(), null,
                StandardCharsets.UTF_8, null);
        return FeignException.errorStatus("verifyCoverage",
                feign.Response.builder().status(status).reason("error").request(request).headers(Map.of()).build());
    }
}

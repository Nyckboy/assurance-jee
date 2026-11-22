package ma.sinistrepro.indemnities.controller;

import ma.sinistrepro.indemnities.dto.IndemnityRequest;
import ma.sinistrepro.indemnities.dto.IndemnityResponse;
import ma.sinistrepro.indemnities.enums.IndemnityStatus;
import ma.sinistrepro.indemnities.service.IndemnityService;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;

import java.math.BigDecimal;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class IndemnityControllerTest {

    @Mock
    private IndemnityService indemnityService;

    @InjectMocks
    private IndemnityController indemnityController;

    private MockMvc mockMvc;

    private LocalValidatorFactoryBean validator;

    @BeforeEach
    void setUp() {
        validator = new LocalValidatorFactoryBean();
        validator.afterPropertiesSet();

        mockMvc = MockMvcBuilders
                .standaloneSetup(indemnityController)
                .setValidator(validator)
                .build();
    }

    private IndemnityResponse sampleResponse() {
        return new IndemnityResponse(
                1L,
                10L,
                new BigDecimal("7000"),
                new BigDecimal("1000"),
                null,
                IndemnityStatus.CALCULEE
        );
    }

    @Test
    void calculateIndemnityShouldReturnCreated() throws Exception {
        when(indemnityService.calculateIndemnity(10L))
                .thenReturn(sampleResponse());

        mockMvc.perform(post("/api/indemnities/calculate/10"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.claimId").value(10))
                .andExpect(jsonPath("$.montantCalcule").value(7000));

        verify(indemnityService).calculateIndemnity(10L);
    }

    @Test
    void createIndemnityShouldReturnCreated() throws Exception {
        when(indemnityService.createIndemnity(any(IndemnityRequest.class)))
                .thenReturn(sampleResponse());

        String requestJson = """
                {
                    "claimId": 10,
                    "montantCalcule": 7000,
                    "franchiseAppliquee": 1000
                }
                """;

        mockMvc.perform(post("/api/indemnities")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.claimId").value(10))
                .andExpect(jsonPath("$.statut").value("CALCULEE"));

        verify(indemnityService).createIndemnity(any(IndemnityRequest.class));
    }

    @Test
    void createIndemnityShouldRejectInvalidRequest() throws Exception {
        String invalidJson = """
                {
                    "claimId": 0,
                    "montantCalcule": 0,
                    "franchiseAppliquee": -1
                }
                """;

        mockMvc.perform(post("/api/indemnities")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(invalidJson))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(indemnityService);
    }

    @Test
    void getAllIndemnitiesShouldReturnOk() throws Exception {
        when(indemnityService.getAllIndemnities())
                .thenReturn(List.of(sampleResponse()));

        mockMvc.perform(get("/api/indemnities"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(1))
                .andExpect(jsonPath("$[0].claimId").value(10));

        verify(indemnityService).getAllIndemnities();
    }

    @Test
    void getIndemnityByIdShouldReturnOk() throws Exception {
        when(indemnityService.getIndemnityById(1L))
                .thenReturn(sampleResponse());

        mockMvc.perform(get("/api/indemnities/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.claimId").value(10));

        verify(indemnityService).getIndemnityById(1L);
    }

    @Test
    void updateIndemnityShouldReturnOk() throws Exception {
        when(indemnityService.updateIndemnity(
                eq(1L),
                any(IndemnityRequest.class)
        )).thenReturn(sampleResponse());

        String requestJson = """
                {
                    "claimId": 10,
                    "montantCalcule": 7000,
                    "franchiseAppliquee": 1000
                }
                """;

        mockMvc.perform(put("/api/indemnities/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.claimId").value(10));

        verify(indemnityService).updateIndemnity(
                eq(1L),
                any(IndemnityRequest.class)
        );
    }

    @Test
    void deleteIndemnityShouldReturnNoContent() throws Exception {
        mockMvc.perform(delete("/api/indemnities/1"))
                .andExpect(status().isNoContent());

        verify(indemnityService).deleteIndemnity(1L);
    }
}
package ma.sinistrepro.indemnities.service;

import ma.sinistrepro.indemnities.client.ClaimResponse;
import ma.sinistrepro.indemnities.client.ClaimServiceClient;
import ma.sinistrepro.indemnities.client.CoverageResponse;
import ma.sinistrepro.indemnities.client.PolicyServiceClient;
import ma.sinistrepro.indemnities.dto.IndemnityResponse;
import ma.sinistrepro.indemnities.dto.IndemnityRequest;
import ma.sinistrepro.indemnities.entity.Indemnity;
import ma.sinistrepro.indemnities.enums.IndemnityStatus;
import ma.sinistrepro.indemnities.exception.ResourceNotFoundException;
import ma.sinistrepro.indemnities.exception.ResourceConflictException;
import ma.sinistrepro.indemnities.repository.IndemnityRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class IndemnityServiceTest {

    @Mock
    private IndemnityRepository indemnityRepository;

    @Mock
    private ClaimServiceClient claimServiceClient;

    @Mock
    private PolicyServiceClient policyServiceClient;

    @Mock
    private IndemnityCalculator indemnityCalculator;

    @InjectMocks
    private IndemnityService indemnityService;

    private ClaimResponse claim;
    private CoverageResponse coverage;

    @BeforeEach
    void setUp() {
        claim = new ClaimResponse(
                10L,
                20L,
                "client@test.com",
                LocalDate.of(2026, 11, 20),
                "AUTO",
                new BigDecimal("10000"),
                new BigDecimal("9000"),
                "EN_EXPERTISE"
        );

        coverage = new CoverageResponse(
                20L,
                "AUTO",
                true,
                "Covered",
                new BigDecimal("8000"),
                new BigDecimal("1000")
        );
    }

    @Test
    void calculateIndemnityShouldSaveCalculatedIndemnity() {
        when(indemnityRepository.existsByClaimId(10L)).thenReturn(false);
        when(claimServiceClient.getClaimById(10L)).thenReturn(claim);
        when(policyServiceClient.verifyCoverage(20L, "AUTO"))
                .thenReturn(coverage);
        when(indemnityCalculator.calculate(
                new BigDecimal("9000"),
                new BigDecimal("8000"),
                new BigDecimal("1000")
        )).thenReturn(new BigDecimal("7000"));

        when(indemnityRepository.save(any(Indemnity.class)))
                .thenAnswer(invocation -> {
                    Indemnity indemnity = invocation.getArgument(0);
                    indemnity.setId(1L);
                    return indemnity;
                });

        IndemnityResponse response =
                indemnityService.calculateIndemnity(10L);

        assertNotNull(response);
        assertEquals(1L, response.getId());
        assertEquals(10L, response.getClaimId());
        assertEquals(new BigDecimal("7000"), response.getMontantCalcule());
        assertEquals(new BigDecimal("1000"), response.getFranchiseAppliquee());
        assertEquals(IndemnityStatus.CALCULEE, response.getStatut());

        ArgumentCaptor<Indemnity> captor =
                ArgumentCaptor.forClass(Indemnity.class);

        verify(indemnityRepository).save(captor.capture());

        assertEquals(10L, captor.getValue().getClaimId());
        assertEquals(new BigDecimal("7000"),
                captor.getValue().getMontantCalcule());
        assertEquals(IndemnityStatus.CALCULEE,
                captor.getValue().getStatut());
    }

    @Test
    void calculateIndemnityShouldRejectInvalidClaimId() {
        assertThrows(
                IllegalArgumentException.class,
                () -> indemnityService.calculateIndemnity(0L)
        );

        verifyNoInteractions(indemnityRepository,
                claimServiceClient, policyServiceClient);
    }

    @Test
    void calculateIndemnityShouldRejectExistingIndemnity() {
        when(indemnityRepository.existsByClaimId(10L)).thenReturn(true);

        assertThrows(
                ResourceConflictException.class,
                () -> indemnityService.calculateIndemnity(10L)
        );

        verifyNoInteractions(claimServiceClient, policyServiceClient);
        verify(indemnityRepository, never()).save(any());
    }

    @Test
    void calculateIndemnityShouldRejectMissingClaim() {
        when(indemnityRepository.existsByClaimId(10L)).thenReturn(false);
        when(claimServiceClient.getClaimById(10L)).thenReturn(null);

        assertThrows(
                ResourceNotFoundException.class,
                () -> indemnityService.calculateIndemnity(10L)
        );

        verifyNoInteractions(policyServiceClient);
        verify(indemnityRepository, never()).save(any());
    }

    @Test
    void calculateIndemnityShouldRejectUncoveredClaim() {
        CoverageResponse uncovered = new CoverageResponse(
                20L,
                "AUTO",
                false,
                "No matching guarantee",
                null,
                null
        );

        when(indemnityRepository.existsByClaimId(10L)).thenReturn(false);
        when(claimServiceClient.getClaimById(10L)).thenReturn(claim);
        when(policyServiceClient.verifyCoverage(20L, "AUTO"))
                .thenReturn(uncovered);

        assertThrows(
                IllegalArgumentException.class,
                () -> indemnityService.calculateIndemnity(10L)
        );

        verify(indemnityRepository, never()).save(any());
        verifyNoInteractions(indemnityCalculator);
    }

    @Test
    void calculateIndemnityShouldRejectMissingCoverageAmounts() {
        CoverageResponse incompleteCoverage = new CoverageResponse(
                20L,
                "AUTO",
                true,
                "Covered",
                null,
                null
        );

        when(indemnityRepository.existsByClaimId(10L)).thenReturn(false);
        when(claimServiceClient.getClaimById(10L)).thenReturn(claim);
        when(policyServiceClient.verifyCoverage(20L, "AUTO"))
                .thenReturn(incompleteCoverage);

        assertThrows(
                IllegalArgumentException.class,
                () -> indemnityService.calculateIndemnity(10L)
        );

        verify(indemnityRepository, never()).save(any());
        verifyNoInteractions(indemnityCalculator);
    }

    @Test
    void createIndemnityShouldSaveNewIndemnity() {
        IndemnityRequest request = new IndemnityRequest();
        request.setClaimId(10L);
        request.setMontantCalcule(new BigDecimal("7000"));
        request.setFranchiseAppliquee(new BigDecimal("1000"));

        when(indemnityRepository.existsByClaimId(10L)).thenReturn(false);

        when(indemnityRepository.save(any(Indemnity.class)))
                .thenAnswer(invocation -> {
                    Indemnity indemnity = invocation.getArgument(0);
                    indemnity.setId(2L);
                    return indemnity;
                });

        IndemnityResponse response = indemnityService.createIndemnity(request);

        assertNotNull(response);
        assertEquals(2L, response.getId());
        assertEquals(10L, response.getClaimId());
        assertEquals(new BigDecimal("7000"), response.getMontantCalcule());
        assertEquals(new BigDecimal("1000"), response.getFranchiseAppliquee());
        assertEquals(IndemnityStatus.EN_ATTENTE, response.getStatut());

        verify(indemnityRepository).save(any(Indemnity.class));
    }

    @Test
    void createIndemnityShouldRejectDuplicateClaim() {
        IndemnityRequest request = new IndemnityRequest();
        request.setClaimId(10L);
        request.setMontantCalcule(new BigDecimal("7000"));
        request.setFranchiseAppliquee(new BigDecimal("1000"));

        when(indemnityRepository.existsByClaimId(10L)).thenReturn(true);

        assertThrows(
                ResourceConflictException.class,
                () -> indemnityService.createIndemnity(request)
        );

        verify(indemnityRepository, never()).save(any());
    }

    @Test
    void getAllIndemnitiesShouldReturnAllSavedIndemnities() {
        Indemnity indemnity = new Indemnity();
        indemnity.setId(1L);
        indemnity.setClaimId(10L);
        indemnity.setMontantCalcule(new BigDecimal("7000"));
        indemnity.setFranchiseAppliquee(new BigDecimal("1000"));
        indemnity.setStatut(IndemnityStatus.CALCULEE);

        when(indemnityRepository.findAll()).thenReturn(List.of(indemnity));

        var responses = indemnityService.getAllIndemnities();

        assertEquals(1, responses.size());
        assertEquals(1L, responses.get(0).getId());
        assertEquals(10L, responses.get(0).getClaimId());
        assertEquals(new BigDecimal("7000"),
                responses.get(0).getMontantCalcule());
    }

    @Test
    void getIndemnityByIdShouldReturnExistingIndemnity() {
        Indemnity indemnity = new Indemnity();
        indemnity.setId(1L);
        indemnity.setClaimId(10L);
        indemnity.setMontantCalcule(new BigDecimal("7000"));
        indemnity.setFranchiseAppliquee(new BigDecimal("1000"));
        indemnity.setStatut(IndemnityStatus.CALCULEE);

        when(indemnityRepository.findById(1L))
                .thenReturn(Optional.of(indemnity));

        IndemnityResponse response = indemnityService.getIndemnityById(1L);

        assertNotNull(response);
        assertEquals(1L, response.getId());
        assertEquals(10L, response.getClaimId());
        assertEquals(IndemnityStatus.CALCULEE, response.getStatut());
    }

    @Test
    void updateIndemnityShouldModifyExistingIndemnity() {
        Indemnity existing = new Indemnity();
        existing.setId(1L);
        existing.setClaimId(10L);
        existing.setMontantCalcule(new BigDecimal("7000"));
        existing.setFranchiseAppliquee(new BigDecimal("1000"));
        existing.setStatut(IndemnityStatus.EN_ATTENTE);

        IndemnityRequest request = new IndemnityRequest();
        request.setClaimId(10L);
        request.setMontantCalcule(new BigDecimal("7500"));
        request.setFranchiseAppliquee(new BigDecimal("500"));

        when(indemnityRepository.findById(1L))
                .thenReturn(Optional.of(existing));

        when(indemnityRepository.save(any(Indemnity.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        IndemnityResponse response =
                indemnityService.updateIndemnity(1L, request);

        assertEquals(new BigDecimal("7500"), response.getMontantCalcule());
        assertEquals(new BigDecimal("500"), response.getFranchiseAppliquee());
        assertEquals(10L, response.getClaimId());

        verify(indemnityRepository).save(existing);
    }

    @Test
    void updateIndemnityShouldRejectDuplicateClaim() {
        Indemnity existing = new Indemnity();
        existing.setId(1L);
        existing.setClaimId(10L);

        IndemnityRequest request = new IndemnityRequest();
        request.setClaimId(20L);
        request.setMontantCalcule(new BigDecimal("7500"));
        request.setFranchiseAppliquee(new BigDecimal("500"));

        when(indemnityRepository.findById(1L))
                .thenReturn(Optional.of(existing));

        when(indemnityRepository.existsByClaimId(20L)).thenReturn(true);

        assertThrows(
                ResourceConflictException.class,
                () -> indemnityService.updateIndemnity(1L, request)
        );

        verify(indemnityRepository, never()).save(any());
    }

    @Test
    void deleteIndemnityShouldDeleteExistingIndemnity() {
        Indemnity existing = new Indemnity();
        existing.setId(1L);
        existing.setClaimId(10L);

        when(indemnityRepository.findById(1L))
                .thenReturn(Optional.of(existing));

        indemnityService.deleteIndemnity(1L);

        verify(indemnityRepository).delete(existing);
    }
}
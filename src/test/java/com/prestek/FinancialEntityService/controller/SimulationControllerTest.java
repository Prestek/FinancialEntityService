package com.prestek.FinancialEntityService.controller;

import com.prestek.FinancialEntityService.dto.RecommendationDto;
import com.prestek.FinancialEntityService.dto.SimulationRequest;
import com.prestek.FinancialEntityService.dto.SimulationResponse;
import com.prestek.FinancialEntityService.service.SimulationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SimulationControllerTest {

    @Mock
    private SimulationService simulationService;

    @InjectMocks
    private SimulationController simulationController;

    private SimulationRequest validRequest;
    private SimulationResponse validResponse;

    @BeforeEach
    void setUp() {
        validRequest = new SimulationRequest();
        validRequest.setUserId("user123");
        validRequest.setAmount(20000000.0);
        validRequest.setTermMonths(48);
        validRequest.setMonthlyIncome(5000000.0);

        validResponse = SimulationResponse.builder()
                .recommendation(RecommendationDto.builder()
                        .bestOption("Bancolombia")
                        .reason("Mejor tasa")
                        .riskAssessment("bajo")
                        .summary("Recomendado")
                        .build())
                .build();
    }

    @Test
    void simulateLoan_WithValidRequestAndToken_ShouldReturnSimulation() {
        // Arrange
        String authToken = "Bearer valid-token";
        when(simulationService.simulateLoan(any(SimulationRequest.class), anyString()))
                .thenReturn(Mono.just(validResponse));

        // Act & Assert
        StepVerifier.create(simulationController.simulateLoan(validRequest, authToken))
                .expectNext(validResponse)
                .verifyComplete();
    }

    @Test
    void simulateLoan_WithoutToken_ShouldCallServiceWithNull() {
        // Arrange
        when(simulationService.simulateLoan(any(SimulationRequest.class), isNull()))
                .thenReturn(Mono.just(validResponse));

        // Act & Assert
        StepVerifier.create(simulationController.simulateLoan(validRequest, null))
                .expectNext(validResponse)
                .verifyComplete();
    }

    @Test
    void simulateLoan_ServiceReturnsError_ShouldPropagateError() {
        // Arrange
        String authToken = "Bearer valid-token";
        when(simulationService.simulateLoan(any(SimulationRequest.class), anyString()))
                .thenReturn(Mono.error(new RuntimeException("Service error")));

        // Act & Assert
        StepVerifier.create(simulationController.simulateLoan(validRequest, authToken))
                .expectError(RuntimeException.class)
                .verify();
    }
}

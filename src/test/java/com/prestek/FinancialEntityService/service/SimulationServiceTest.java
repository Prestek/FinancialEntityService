package com.prestek.FinancialEntityService.service;

import com.prestek.FinancialEntityService.dto.RecommendationDto;
import com.prestek.FinancialEntityService.dto.SimulationRequest;
import com.prestek.FinancialEntityService.dto.SimulationResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.util.function.Function;
import java.util.function.Predicate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SimulationServiceTest {

    @Mock
    private WebClient.Builder webClientBuilder;

    @Mock
    private WebClient webClient;

    @Mock
    private WebClient.RequestBodyUriSpec requestBodyUriSpec;

    @Mock
    private WebClient.RequestBodySpec requestBodySpec;

    @Mock
    private WebClient.RequestHeadersSpec requestHeadersSpec;

    @Mock
    private WebClient.ResponseSpec responseSpec;

    private SimulationService simulationService;

    private String n8nUrl = "http://localhost:5678/webhook-test/simulate-credit";

    @BeforeEach
    void setUp() {
        simulationService = new SimulationService(webClientBuilder);
        ReflectionTestUtils.setField(simulationService, "n8nSimulationUrl", n8nUrl);
    }

    @Test
    void simulateLoan_WithValidRequest_ShouldReturnSimulation() {
        // Arrange
        SimulationRequest request = new SimulationRequest();
        request.setUserId("user123");
        request.setAmount(20000000.0);
        request.setTermMonths(48);
        request.setMonthlyIncome(5000000.0);

        String authToken = "Bearer valid-token";

        SimulationResponse expectedResponse = SimulationResponse.builder()
                .recommendation(RecommendationDto.builder()
                        .bestOption("Bancolombia")
                        .reason("Mejor tasa")
                        .riskAssessment("bajo")
                        .summary("Recomendado")
                        .build())
                .build();

        when(webClientBuilder.build()).thenReturn(webClient);
        when(webClient.post()).thenReturn(requestBodyUriSpec);
        when(requestBodyUriSpec.uri(anyString())).thenReturn(requestBodySpec);
        when(requestBodySpec.header(anyString(), anyString())).thenReturn(requestBodySpec);
        when(requestBodySpec.bodyValue(any())).thenReturn(requestHeadersSpec);
        when(requestHeadersSpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.onStatus(any(), any())).thenReturn(responseSpec);
        when(responseSpec.bodyToMono(SimulationResponse.class)).thenReturn(Mono.just(expectedResponse));

        // Act & Assert
        StepVerifier.create(simulationService.simulateLoan(request, authToken))
                .expectNext(expectedResponse)
                .verifyComplete();
    }

    @Test
    void simulateLoan_WithNullUserId_ShouldReturnError() {
        // Arrange
        SimulationRequest request = new SimulationRequest();
        request.setUserId(null);
        request.setAmount(20000000.0);
        request.setTermMonths(48);
        request.setMonthlyIncome(5000000.0);

        String authToken = "Bearer valid-token";

        // Act & Assert
        StepVerifier.create(simulationService.simulateLoan(request, authToken))
                .expectError(IllegalArgumentException.class)
                .verify();
    }

    @Test
    void simulateLoan_WithInvalidAmount_ShouldReturnError() {
        // Arrange
        SimulationRequest request = new SimulationRequest();
        request.setUserId("user123");
        request.setAmount(500000.0); // Less than 1M minimum
        request.setTermMonths(48);
        request.setMonthlyIncome(5000000.0);

        String authToken = "Bearer valid-token";

        // Act & Assert
        StepVerifier.create(simulationService.simulateLoan(request, authToken))
                .expectError(IllegalArgumentException.class)
                .verify();
    }

    @Test
    void simulateLoan_WithInvalidTermMonths_ShouldReturnError() {
        // Arrange
        SimulationRequest request = new SimulationRequest();
        request.setUserId("user123");
        request.setAmount(20000000.0);
        request.setTermMonths(3); // Less than 6 months minimum
        request.setMonthlyIncome(5000000.0);

        String authToken = "Bearer valid-token";

        // Act & Assert
        StepVerifier.create(simulationService.simulateLoan(request, authToken))
                .expectError(IllegalArgumentException.class)
                .verify();
    }

    @Test
    void simulateLoan_WithNullMonthlyIncome_ShouldReturnError() {
        // Arrange
        SimulationRequest request = new SimulationRequest();
        request.setUserId("user123");
        request.setAmount(20000000.0);
        request.setTermMonths(48);
        request.setMonthlyIncome(null);

        String authToken = "Bearer valid-token";

        // Act & Assert
        StepVerifier.create(simulationService.simulateLoan(request, authToken))
                .expectError(IllegalArgumentException.class)
                .verify();
    }

    @Test
    void simulateLoan_WithNullAuthToken_ShouldReturnError() {
        // Arrange
        SimulationRequest request = new SimulationRequest();
        request.setUserId("user123");
        request.setAmount(20000000.0);
        request.setTermMonths(48);
        request.setMonthlyIncome(5000000.0);

        // Act & Assert
        StepVerifier.create(simulationService.simulateLoan(request, null))
                .expectError(IllegalArgumentException.class)
                .verify();
    }

    @Test
    void simulateLoan_N8NError_ShouldPropagateError() {
        // Arrange
        SimulationRequest request = new SimulationRequest();
        request.setUserId("user123");
        request.setAmount(20000000.0);
        request.setTermMonths(48);
        request.setMonthlyIncome(5000000.0);

        String authToken = "Bearer valid-token";

        when(webClientBuilder.build()).thenReturn(webClient);
        when(webClient.post()).thenReturn(requestBodyUriSpec);
        when(requestBodyUriSpec.uri(anyString())).thenReturn(requestBodySpec);
        when(requestBodySpec.header(anyString(), anyString())).thenReturn(requestBodySpec);
        when(requestBodySpec.bodyValue(any())).thenReturn(requestHeadersSpec);
        when(requestHeadersSpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.onStatus(any(), any())).thenReturn(responseSpec);
        when(responseSpec.bodyToMono(SimulationResponse.class))
                .thenReturn(Mono.error(new RuntimeException("N8N service unavailable")));

        // Act & Assert
        StepVerifier.create(simulationService.simulateLoan(request, authToken))
                .expectError(RuntimeException.class)
                .verify();
    }

    @Test
    void simulateLoan_ShouldSendAuthorizationHeader() {
        // Arrange
        SimulationRequest request = new SimulationRequest();
        request.setUserId("user123");
        request.setAmount(20000000.0);
        request.setTermMonths(48);
        request.setMonthlyIncome(5000000.0);

        String authToken = "Bearer test-token";

        SimulationResponse expectedResponse = SimulationResponse.builder()
                .recommendation(RecommendationDto.builder()
                        .bestOption("Bancolombia")
                        .build())
                .build();

        when(webClientBuilder.build()).thenReturn(webClient);
        when(webClient.post()).thenReturn(requestBodyUriSpec);
        when(requestBodyUriSpec.uri(anyString())).thenReturn(requestBodySpec);
        when(requestBodySpec.header(anyString(), anyString())).thenReturn(requestBodySpec);
        when(requestBodySpec.bodyValue(any())).thenReturn(requestHeadersSpec);
        when(requestHeadersSpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.onStatus(any(), any())).thenReturn(responseSpec);
        when(responseSpec.bodyToMono(SimulationResponse.class)).thenReturn(Mono.just(expectedResponse));

        // Act
        StepVerifier.create(simulationService.simulateLoan(request, authToken))
                .expectNext(expectedResponse)
                .verifyComplete();

        // Assert
        ArgumentCaptor<String> headerCaptor = ArgumentCaptor.forClass(String.class);
        verify(requestBodySpec).header(eq("Authorization"), headerCaptor.capture());
        assertThat(headerCaptor.getValue()).isEqualTo(authToken);
    }

    // ==================== Pruebas de validación de límites ====================

    @Test
    void simulateLoan_WithAmountAtMinimumBoundary_ShouldSucceed() {
        // Arrange - Monto exactamente en el límite mínimo (1M)
        SimulationRequest request = new SimulationRequest();
        request.setUserId("user123");
        request.setAmount(1000000.0);
        request.setTermMonths(48);
        request.setMonthlyIncome(5000000.0);

        String authToken = "Bearer valid-token";

        SimulationResponse expectedResponse = SimulationResponse.builder()
                .recommendation(RecommendationDto.builder()
                        .bestOption("Bancolombia")
                        .build())
                .build();

        when(webClientBuilder.build()).thenReturn(webClient);
        when(webClient.post()).thenReturn(requestBodyUriSpec);
        when(requestBodyUriSpec.uri(anyString())).thenReturn(requestBodySpec);
        when(requestBodySpec.header(anyString(), anyString())).thenReturn(requestBodySpec);
        when(requestBodySpec.bodyValue(any())).thenReturn(requestHeadersSpec);
        when(requestHeadersSpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.onStatus(any(), any())).thenReturn(responseSpec);
        when(responseSpec.bodyToMono(SimulationResponse.class)).thenReturn(Mono.just(expectedResponse));

        // Act & Assert
        StepVerifier.create(simulationService.simulateLoan(request, authToken))
                .expectNext(expectedResponse)
                .verifyComplete();
    }

    @Test
    void simulateLoan_WithAmountBelowMinimum_ShouldReturnError() {
        // Arrange - Monto justo debajo del mínimo
        SimulationRequest request = new SimulationRequest();
        request.setUserId("user123");
        request.setAmount(999999.0);
        request.setTermMonths(48);
        request.setMonthlyIncome(5000000.0);

        String authToken = "Bearer valid-token";

        // Act & Assert
        StepVerifier.create(simulationService.simulateLoan(request, authToken))
                .expectErrorMatches(error -> error instanceof IllegalArgumentException &&
                        error.getMessage().contains("Amount out of range"))
                .verify();
    }

    @Test
    void simulateLoan_WithAmountAtMaximumBoundary_ShouldSucceed() {
        // Arrange - Monto exactamente en el límite máximo (50M)
        SimulationRequest request = new SimulationRequest();
        request.setUserId("user123");
        request.setAmount(50000000.0);
        request.setTermMonths(48);
        request.setMonthlyIncome(5000000.0);

        String authToken = "Bearer valid-token";

        SimulationResponse expectedResponse = SimulationResponse.builder()
                .recommendation(RecommendationDto.builder()
                        .bestOption("Bancolombia")
                        .build())
                .build();

        when(webClientBuilder.build()).thenReturn(webClient);
        when(webClient.post()).thenReturn(requestBodyUriSpec);
        when(requestBodyUriSpec.uri(anyString())).thenReturn(requestBodySpec);
        when(requestBodySpec.header(anyString(), anyString())).thenReturn(requestBodySpec);
        when(requestBodySpec.bodyValue(any())).thenReturn(requestHeadersSpec);
        when(requestHeadersSpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.onStatus(any(), any())).thenReturn(responseSpec);
        when(responseSpec.bodyToMono(SimulationResponse.class)).thenReturn(Mono.just(expectedResponse));

        // Act & Assert
        StepVerifier.create(simulationService.simulateLoan(request, authToken))
                .expectNext(expectedResponse)
                .verifyComplete();
    }

    @Test
    void simulateLoan_WithAmountAboveMaximum_ShouldReturnError() {
        // Arrange - Monto justo arriba del máximo
        SimulationRequest request = new SimulationRequest();
        request.setUserId("user123");
        request.setAmount(50000001.0);
        request.setTermMonths(48);
        request.setMonthlyIncome(5000000.0);

        String authToken = "Bearer valid-token";

        // Act & Assert
        StepVerifier.create(simulationService.simulateLoan(request, authToken))
                .expectErrorMatches(error -> error instanceof IllegalArgumentException &&
                        error.getMessage().contains("Amount out of range"))
                .verify();
    }

    @Test
    void simulateLoan_WithTermAtMinimumBoundary_ShouldSucceed() {
        // Arrange - Término exactamente en el mínimo (6 meses)
        SimulationRequest request = new SimulationRequest();
        request.setUserId("user123");
        request.setAmount(20000000.0);
        request.setTermMonths(6);
        request.setMonthlyIncome(5000000.0);

        String authToken = "Bearer valid-token";

        SimulationResponse expectedResponse = SimulationResponse.builder()
                .recommendation(RecommendationDto.builder()
                        .bestOption("Bancolombia")
                        .build())
                .build();

        when(webClientBuilder.build()).thenReturn(webClient);
        when(webClient.post()).thenReturn(requestBodyUriSpec);
        when(requestBodyUriSpec.uri(anyString())).thenReturn(requestBodySpec);
        when(requestBodySpec.header(anyString(), anyString())).thenReturn(requestBodySpec);
        when(requestBodySpec.bodyValue(any())).thenReturn(requestHeadersSpec);
        when(requestHeadersSpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.onStatus(any(), any())).thenReturn(responseSpec);
        when(responseSpec.bodyToMono(SimulationResponse.class)).thenReturn(Mono.just(expectedResponse));

        // Act & Assert
        StepVerifier.create(simulationService.simulateLoan(request, authToken))
                .expectNext(expectedResponse)
                .verifyComplete();
    }

    @Test
    void simulateLoan_WithTermBelowMinimum_ShouldReturnError() {
        // Arrange - Término justo debajo del mínimo
        SimulationRequest request = new SimulationRequest();
        request.setUserId("user123");
        request.setAmount(20000000.0);
        request.setTermMonths(5);
        request.setMonthlyIncome(5000000.0);

        String authToken = "Bearer valid-token";

        // Act & Assert
        StepVerifier.create(simulationService.simulateLoan(request, authToken))
                .expectErrorMatches(error -> error instanceof IllegalArgumentException &&
                        error.getMessage().contains("Term out of range"))
                .verify();
    }

    @Test
    void simulateLoan_WithTermAtMaximumBoundary_ShouldSucceed() {
        // Arrange - Término exactamente en el máximo (60 meses)
        SimulationRequest request = new SimulationRequest();
        request.setUserId("user123");
        request.setAmount(20000000.0);
        request.setTermMonths(60);
        request.setMonthlyIncome(5000000.0);

        String authToken = "Bearer valid-token";

        SimulationResponse expectedResponse = SimulationResponse.builder()
                .recommendation(RecommendationDto.builder()
                        .bestOption("Bancolombia")
                        .build())
                .build();

        when(webClientBuilder.build()).thenReturn(webClient);
        when(webClient.post()).thenReturn(requestBodyUriSpec);
        when(requestBodyUriSpec.uri(anyString())).thenReturn(requestBodySpec);
        when(requestBodySpec.header(anyString(), anyString())).thenReturn(requestBodySpec);
        when(requestBodySpec.bodyValue(any())).thenReturn(requestHeadersSpec);
        when(requestHeadersSpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.onStatus(any(), any())).thenReturn(responseSpec);
        when(responseSpec.bodyToMono(SimulationResponse.class)).thenReturn(Mono.just(expectedResponse));

        // Act & Assert
        StepVerifier.create(simulationService.simulateLoan(request, authToken))
                .expectNext(expectedResponse)
                .verifyComplete();
    }

    @Test
    void simulateLoan_WithTermAboveMaximum_ShouldReturnError() {
        // Arrange - Término justo arriba del máximo
        SimulationRequest request = new SimulationRequest();
        request.setUserId("user123");
        request.setAmount(20000000.0);
        request.setTermMonths(61);
        request.setMonthlyIncome(5000000.0);

        String authToken = "Bearer valid-token";

        // Act & Assert
        StepVerifier.create(simulationService.simulateLoan(request, authToken))
                .expectErrorMatches(error -> error instanceof IllegalArgumentException &&
                        error.getMessage().contains("Term out of range"))
                .verify();
    }

    @Test
    void simulateLoan_WithZeroMonthlyIncome_ShouldReturnError() {
        // Arrange - Income en 0
        SimulationRequest request = new SimulationRequest();
        request.setUserId("user123");
        request.setAmount(20000000.0);
        request.setTermMonths(48);
        request.setMonthlyIncome(0.0);

        String authToken = "Bearer valid-token";

        // Act & Assert
        StepVerifier.create(simulationService.simulateLoan(request, authToken))
                .expectErrorMatches(error -> error instanceof IllegalArgumentException &&
                        error.getMessage().contains("monthlyIncome must be greater than zero"))
                .verify();
    }

    @Test
    void simulateLoan_WithNegativeMonthlyIncome_ShouldReturnError() {
        // Arrange - Income negativo
        SimulationRequest request = new SimulationRequest();
        request.setUserId("user123");
        request.setAmount(20000000.0);
        request.setTermMonths(48);
        request.setMonthlyIncome(-1000.0);

        String authToken = "Bearer valid-token";

        // Act & Assert
        StepVerifier.create(simulationService.simulateLoan(request, authToken))
                .expectErrorMatches(error -> error instanceof IllegalArgumentException &&
                        error.getMessage().contains("monthlyIncome must be greater than zero"))
                .verify();
    }

    @Test
    void simulateLoan_WithBlankUserId_ShouldReturnError() {
        // Arrange - UserId en blanco (no null, pero vacío)
        SimulationRequest request = new SimulationRequest();
        request.setUserId("   ");
        request.setAmount(20000000.0);
        request.setTermMonths(48);
        request.setMonthlyIncome(5000000.0);

        String authToken = "Bearer valid-token";

        // Act & Assert
        StepVerifier.create(simulationService.simulateLoan(request, authToken))
                .expectErrorMatches(error -> error instanceof IllegalArgumentException &&
                        error.getMessage().contains("userId is required"))
                .verify();
    }

    @Test
    void simulateLoan_WithBlankAuthToken_ShouldReturnError() {
        // Arrange - Token en blanco
        SimulationRequest request = new SimulationRequest();
        request.setUserId("user123");
        request.setAmount(20000000.0);
        request.setTermMonths(48);
        request.setMonthlyIncome(5000000.0);

        // Act & Assert
        StepVerifier.create(simulationService.simulateLoan(request, "   "))
                .expectErrorMatches(error -> error instanceof IllegalArgumentException &&
                        error.getMessage().contains("Authorization token is required"))
                .verify();
    }

    @Test
    void simulateLoan_WithNullAmount_ShouldReturnError() {
        // Arrange - Amount null
        SimulationRequest request = new SimulationRequest();
        request.setUserId("user123");
        request.setAmount(null);
        request.setTermMonths(48);
        request.setMonthlyIncome(5000000.0);

        String authToken = "Bearer valid-token";

        // Act & Assert
        StepVerifier.create(simulationService.simulateLoan(request, authToken))
                .expectErrorMatches(error -> error instanceof IllegalArgumentException &&
                        error.getMessage().contains("amount"))
                .verify();
    }

    @Test
    void simulateLoan_WithNullTermMonths_ShouldReturnError() {
        // Arrange - TermMonths null
        SimulationRequest request = new SimulationRequest();
        request.setUserId("user123");
        request.setAmount(20000000.0);
        request.setTermMonths(null);
        request.setMonthlyIncome(5000000.0);

        String authToken = "Bearer valid-token";

        // Act & Assert
        StepVerifier.create(simulationService.simulateLoan(request, authToken))
                .expectErrorMatches(error -> error instanceof IllegalArgumentException &&
                        error.getMessage().contains("termMonths"))
                .verify();
    }

    // ==================== Pruebas de manejo de errores HTTP ====================

    @Test
    void simulateLoan_WithN8N400Error_ShouldHandleClientError() {
        // Arrange
        SimulationRequest request = new SimulationRequest();
        request.setUserId("user123");
        request.setAmount(20000000.0);
        request.setTermMonths(48);
        request.setMonthlyIncome(5000000.0);

        String authToken = "Bearer valid-token";

        // Simular error 400
        WebClientResponseException badRequestException = WebClientResponseException.create(
                400,
                "Bad Request",
                null,
                "Invalid simulation parameters".getBytes(),
                null);

        when(webClientBuilder.build()).thenReturn(webClient);
        when(webClient.post()).thenReturn(requestBodyUriSpec);
        when(requestBodyUriSpec.uri(anyString())).thenReturn(requestBodySpec);
        when(requestBodySpec.header(anyString(), anyString())).thenReturn(requestBodySpec);
        when(requestBodySpec.bodyValue(any())).thenReturn(requestHeadersSpec);
        when(requestHeadersSpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.onStatus(any(), any())).thenReturn(responseSpec);
        when(responseSpec.bodyToMono(SimulationResponse.class))
                .thenReturn(Mono.error(badRequestException));

        // Act & Assert
        StepVerifier.create(simulationService.simulateLoan(request, authToken))
                .expectErrorMatches(error -> error instanceof WebClientResponseException &&
                        ((WebClientResponseException) error).getStatusCode().value() == 400)
                .verify();
    }

    @Test
    void simulateLoan_WithN8N404Error_ShouldHandleNotFoundError() {
        // Arrange
        SimulationRequest request = new SimulationRequest();
        request.setUserId("user123");
        request.setAmount(20000000.0);
        request.setTermMonths(48);
        request.setMonthlyIncome(5000000.0);

        String authToken = "Bearer valid-token";

        // Simular error 404
        WebClientResponseException notFoundException = WebClientResponseException.create(
                404,
                "Not Found",
                null,
                "Endpoint not found".getBytes(),
                null);

        when(webClientBuilder.build()).thenReturn(webClient);
        when(webClient.post()).thenReturn(requestBodyUriSpec);
        when(requestBodyUriSpec.uri(anyString())).thenReturn(requestBodySpec);
        when(requestBodySpec.header(anyString(), anyString())).thenReturn(requestBodySpec);
        when(requestBodySpec.bodyValue(any())).thenReturn(requestHeadersSpec);
        when(requestHeadersSpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.onStatus(any(), any())).thenReturn(responseSpec);
        when(responseSpec.bodyToMono(SimulationResponse.class))
                .thenReturn(Mono.error(notFoundException));

        // Act & Assert
        StepVerifier.create(simulationService.simulateLoan(request, authToken))
                .expectErrorMatches(error -> error instanceof WebClientResponseException &&
                        ((WebClientResponseException) error).getStatusCode().value() == 404)
                .verify();
    }

    @Test
    void simulateLoan_WithN8N500Error_ShouldHandleServerError() {
        // Arrange
        SimulationRequest request = new SimulationRequest();
        request.setUserId("user123");
        request.setAmount(20000000.0);
        request.setTermMonths(48);
        request.setMonthlyIncome(5000000.0);

        String authToken = "Bearer valid-token";

        // Simular error 500
        WebClientResponseException serverException = WebClientResponseException.create(
                500,
                "Internal Server Error",
                null,
                "N8N workflow failed".getBytes(),
                null);

        when(webClientBuilder.build()).thenReturn(webClient);
        when(webClient.post()).thenReturn(requestBodyUriSpec);
        when(requestBodyUriSpec.uri(anyString())).thenReturn(requestBodySpec);
        when(requestBodySpec.header(anyString(), anyString())).thenReturn(requestBodySpec);
        when(requestBodySpec.bodyValue(any())).thenReturn(requestHeadersSpec);
        when(requestHeadersSpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.onStatus(any(), any())).thenReturn(responseSpec);
        when(responseSpec.bodyToMono(SimulationResponse.class))
                .thenReturn(Mono.error(serverException));

        // Act & Assert
        StepVerifier.create(simulationService.simulateLoan(request, authToken))
                .expectErrorMatches(error -> error instanceof WebClientResponseException &&
                        ((WebClientResponseException) error).getStatusCode().value() == 500)
                .verify();
    }

    @Test
    void simulateLoan_WithN8N503Error_ShouldHandleServiceUnavailable() {
        // Arrange
        SimulationRequest request = new SimulationRequest();
        request.setUserId("user123");
        request.setAmount(20000000.0);
        request.setTermMonths(48);
        request.setMonthlyIncome(5000000.0);

        String authToken = "Bearer valid-token";

        // Simular error 503
        WebClientResponseException serviceUnavailableException = WebClientResponseException.create(
                503,
                "Service Unavailable",
                null,
                "N8N service temporarily unavailable".getBytes(),
                null);

        when(webClientBuilder.build()).thenReturn(webClient);
        when(webClient.post()).thenReturn(requestBodyUriSpec);
        when(requestBodyUriSpec.uri(anyString())).thenReturn(requestBodySpec);
        when(requestBodySpec.header(anyString(), anyString())).thenReturn(requestBodySpec);
        when(requestBodySpec.bodyValue(any())).thenReturn(requestHeadersSpec);
        when(requestHeadersSpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.onStatus(any(), any())).thenReturn(responseSpec);
        when(responseSpec.bodyToMono(SimulationResponse.class))
                .thenReturn(Mono.error(serviceUnavailableException));

        // Act & Assert
        StepVerifier.create(simulationService.simulateLoan(request, authToken))
                .expectErrorMatches(error -> error instanceof WebClientResponseException &&
                        ((WebClientResponseException) error).getStatusCode().value() == 503)
                .verify();
    }

    // ==================== Pruebas del callback onStatus para errores HTTP
    // ====================

    @Test
    void simulateLoan_OnStatus4xxError_ShouldInvokeErrorHandler() {
        // Arrange
        SimulationRequest request = new SimulationRequest();
        request.setUserId("user123");
        request.setAmount(20000000.0);
        request.setTermMonths(48);
        request.setMonthlyIncome(5000000.0);

        String authToken = "Bearer valid-token";

        // Mock del ClientResponse para simular error 400
        ClientResponse clientResponse = mock(ClientResponse.class);
        when(clientResponse.statusCode()).thenReturn(HttpStatus.BAD_REQUEST);
        when(clientResponse.bodyToMono(String.class)).thenReturn(Mono.just("Invalid request parameters"));

        when(webClientBuilder.build()).thenReturn(webClient);
        when(webClient.post()).thenReturn(requestBodyUriSpec);
        when(requestBodyUriSpec.uri(anyString())).thenReturn(requestBodySpec);
        when(requestBodySpec.header(anyString(), anyString())).thenReturn(requestBodySpec);
        when(requestBodySpec.bodyValue(any())).thenReturn(requestHeadersSpec);
        when(requestHeadersSpec.retrieve()).thenReturn(responseSpec);

        // Capturar el predicado y la función del onStatus
        when(responseSpec.onStatus(any(Predicate.class), any(Function.class))).thenAnswer(invocation -> {
            Predicate<HttpStatus> predicate = invocation.getArgument(0);
            Function<ClientResponse, Mono<? extends Throwable>> errorHandler = invocation.getArgument(1);

            // Verificar que el predicado identifica correctamente errores 4xx
            assertThat(predicate.test(HttpStatus.BAD_REQUEST)).isTrue();
            assertThat(predicate.test(HttpStatus.NOT_FOUND)).isTrue();
            assertThat(predicate.test(HttpStatus.OK)).isFalse();

            // Ejecutar el error handler para verificar que genera el error correcto
            Mono<? extends Throwable> errorMono = errorHandler.apply(clientResponse);

            StepVerifier.create(errorMono)
                    .expectErrorMatches(error -> error instanceof RuntimeException &&
                            error.getMessage().contains("N8N error") &&
                            error.getMessage().contains("400") &&
                            error.getMessage().contains("Invalid request parameters"))
                    .verify();

            return responseSpec;
        });

        when(responseSpec.bodyToMono(SimulationResponse.class))
                .thenReturn(
                        Mono.error(new RuntimeException("N8N error: 400 BAD_REQUEST - Invalid request parameters")));

        // Act & Assert
        StepVerifier.create(simulationService.simulateLoan(request, authToken))
                .expectErrorMessage("N8N error: 400 BAD_REQUEST - Invalid request parameters")
                .verify();
    }

    @Test
    void simulateLoan_OnStatus5xxError_ShouldInvokeErrorHandler() {
        // Arrange
        SimulationRequest request = new SimulationRequest();
        request.setUserId("user123");
        request.setAmount(20000000.0);
        request.setTermMonths(48);
        request.setMonthlyIncome(5000000.0);

        String authToken = "Bearer valid-token";

        // Mock del ClientResponse para simular error 500
        ClientResponse clientResponse = mock(ClientResponse.class);
        when(clientResponse.statusCode()).thenReturn(HttpStatus.INTERNAL_SERVER_ERROR);
        when(clientResponse.bodyToMono(String.class)).thenReturn(Mono.just("Internal server error occurred"));

        when(webClientBuilder.build()).thenReturn(webClient);
        when(webClient.post()).thenReturn(requestBodyUriSpec);
        when(requestBodyUriSpec.uri(anyString())).thenReturn(requestBodySpec);
        when(requestBodySpec.header(anyString(), anyString())).thenReturn(requestBodySpec);
        when(requestBodySpec.bodyValue(any())).thenReturn(requestHeadersSpec);
        when(requestHeadersSpec.retrieve()).thenReturn(responseSpec);

        // Capturar el predicado y la función del onStatus
        when(responseSpec.onStatus(any(Predicate.class), any(Function.class))).thenAnswer(invocation -> {
            Predicate<HttpStatus> predicate = invocation.getArgument(0);
            Function<ClientResponse, Mono<? extends Throwable>> errorHandler = invocation.getArgument(1);

            // Verificar que el predicado identifica correctamente errores 5xx
            assertThat(predicate.test(HttpStatus.INTERNAL_SERVER_ERROR)).isTrue();
            assertThat(predicate.test(HttpStatus.SERVICE_UNAVAILABLE)).isTrue();
            assertThat(predicate.test(HttpStatus.OK)).isFalse();

            // Ejecutar el error handler para verificar que genera el error correcto
            Mono<? extends Throwable> errorMono = errorHandler.apply(clientResponse);

            StepVerifier.create(errorMono)
                    .expectErrorMatches(error -> error instanceof RuntimeException &&
                            error.getMessage().contains("N8N error") &&
                            error.getMessage().contains("500") &&
                            error.getMessage().contains("Internal server error occurred"))
                    .verify();

            return responseSpec;
        });

        when(responseSpec.bodyToMono(SimulationResponse.class))
                .thenReturn(Mono.error(
                        new RuntimeException("N8N error: 500 INTERNAL_SERVER_ERROR - Internal server error occurred")));

        // Act & Assert
        StepVerifier.create(simulationService.simulateLoan(request, authToken))
                .expectErrorMessage("N8N error: 500 INTERNAL_SERVER_ERROR - Internal server error occurred")
                .verify();
    }

    @Test
    void simulateLoan_OnStatusWithEmptyErrorBody_ShouldHandleGracefully() {
        // Arrange
        SimulationRequest request = new SimulationRequest();
        request.setUserId("user123");
        request.setAmount(20000000.0);
        request.setTermMonths(48);
        request.setMonthlyIncome(5000000.0);

        String authToken = "Bearer valid-token";

        // Mock del ClientResponse con body vacío
        ClientResponse clientResponse = mock(ClientResponse.class);
        // No configuramos statusCode() porque en este test esperamos respuesta exitosa
        when(clientResponse.bodyToMono(String.class)).thenReturn(Mono.empty());

        when(webClientBuilder.build()).thenReturn(webClient);
        when(webClient.post()).thenReturn(requestBodyUriSpec);
        when(requestBodyUriSpec.uri(anyString())).thenReturn(requestBodySpec);
        when(requestBodySpec.header(anyString(), anyString())).thenReturn(requestBodySpec);
        when(requestBodySpec.bodyValue(any())).thenReturn(requestHeadersSpec);
        when(requestHeadersSpec.retrieve()).thenReturn(responseSpec);

        // Capturar y ejecutar el error handler
        when(responseSpec.onStatus(any(Predicate.class), any(Function.class))).thenAnswer(invocation -> {
            Function<ClientResponse, Mono<? extends Throwable>> errorHandler = invocation.getArgument(1);

            // Cuando el body está vacío, flatMap no se ejecuta y el Mono se completa sin
            // emitir ningún error
            // Este es el comportamiento real de Reactor cuando bodyToMono retorna
            // Mono.empty()
            Mono<? extends Throwable> errorMono = errorHandler.apply(clientResponse);

            // Verificar que el Mono se completa vacío (sin error) porque flatMap no se
            // ejecuta con Mono.empty()
            StepVerifier.create(errorMono)
                    .expectComplete()
                    .verify();

            return responseSpec;
        });

        // Dado que el error handler no lanza error con body vacío, el flujo continúa
        // normalmente
        SimulationResponse mockResponse = new SimulationResponse();
        RecommendationDto recommendation = new RecommendationDto();
        recommendation.setBestOption("Bancolombia");
        mockResponse.setRecommendation(recommendation);
        when(responseSpec.bodyToMono(SimulationResponse.class))
                .thenReturn(Mono.just(mockResponse));

        // Act & Assert - El servicio se completa normalmente porque el error handler no
        // lanzó excepción
        StepVerifier.create(simulationService.simulateLoan(request, authToken))
                .expectNext(mockResponse)
                .expectComplete()
                .verify();
    }
}

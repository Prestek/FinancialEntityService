package com.prestek.FinancialEntityService.service;

import com.prestek.FinancialEntityCore.dto.ApplicationDto;
import com.prestek.FinancialEntityCore.model.Application;
import com.prestek.FinancialEntityService.dto.BankApplicationDto;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.time.LocalDateTime;
import java.util.List;
import java.util.function.Function;
import java.util.function.Predicate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BankAggregationServiceTest {

    @Mock
    private WebClient.Builder webClientBuilder;

    @Mock
    private WebClient webClient;

    @Mock
    private WebClient.RequestHeadersUriSpec requestHeadersUriSpec;

    @Mock
    private WebClient.RequestHeadersSpec requestHeadersSpec;

    @Mock
    private WebClient.ResponseSpec responseSpec;

    private BankAggregationService service;

    @BeforeEach
    void setUp() {
        when(webClientBuilder.build()).thenReturn(webClient);
        service = new BankAggregationService(webClientBuilder);

        // Configuración base del mock chain
        when(webClient.get()).thenReturn(requestHeadersUriSpec);
        when(requestHeadersUriSpec.uri(anyString())).thenReturn(requestHeadersSpec);
        when(requestHeadersSpec.header(anyString(), anyString())).thenReturn(requestHeadersSpec);
        when(requestHeadersSpec.retrieve()).thenReturn(responseSpec);
    }

    @Test
    void getAllApplicationsFromBanks_WithValidResponse_ShouldAggregateResults() {
        // Arrange
        String userId = "user123";
        String jwtToken = "Bearer valid-token";

        ApplicationDto app1 = createApplicationDto(1L, "PENDING", 10000000.0);
        ApplicationDto app2 = createApplicationDto(2L, "APPROVED", 20000000.0);

        // Mock para que cada banco retorne aplicaciones
        when(responseSpec.onStatus(any(Predicate.class), any(Function.class))).thenReturn(responseSpec);
        when(responseSpec.bodyToFlux(ApplicationDto.class))
                .thenReturn(Flux.just(app1))
                .thenReturn(Flux.just(app2))
                .thenReturn(Flux.empty());

        // Act & Assert
        StepVerifier.create(service.getAllApplicationsFromBanks(userId, jwtToken))
                .assertNext(applications -> {
                    assertThat(applications).hasSize(2);
                    assertThat(applications).extracting(BankApplicationDto::getBankName)
                            .containsExactlyInAnyOrder("Bancolombia", "Davivienda");
                    assertThat(applications).allMatch(app -> app.getUserId().equals(userId));
                })
                .verifyComplete();
    }

    @Test
    void getAllApplicationsFromBanks_WithEmptyResponse_ShouldReturnEmptyList() {
        // Arrange
        String userId = "user999";
        String jwtToken = "Bearer valid-token";

        when(responseSpec.onStatus(any(Predicate.class), any(Function.class))).thenReturn(responseSpec);
        when(responseSpec.bodyToFlux(ApplicationDto.class)).thenReturn(Flux.empty());

        // Act & Assert
        StepVerifier.create(service.getAllApplicationsFromBanks(userId, jwtToken))
                .assertNext(applications -> {
                    assertThat(applications).isEmpty();
                })
                .verifyComplete();
    }

    @Test
    void getAllApplicationsFromBanks_WithNullToken_ShouldWork() {
        // Arrange
        String userId = "user123";
        String jwtToken = null;

        ApplicationDto app = createApplicationDto(1L, "PENDING", 5000000.0);

        when(responseSpec.onStatus(any(Predicate.class), any(Function.class))).thenReturn(responseSpec);
        when(responseSpec.bodyToFlux(ApplicationDto.class))
                .thenReturn(Flux.just(app))
                .thenReturn(Flux.empty())
                .thenReturn(Flux.empty());

        // Act & Assert
        StepVerifier.create(service.getAllApplicationsFromBanks(userId, jwtToken))
                .assertNext(applications -> {
                    assertThat(applications).hasSize(1);
                    assertThat(applications.get(0).getStatus()).isEqualTo("PENDING");
                })
                .verifyComplete();
    }

    @Test
    void getAllApplicationsFromBanks_WithMultipleApplicationsPerBank_ShouldAggregateAll() {
        // Arrange
        String userId = "user123";
        String jwtToken = "Bearer valid-token";

        ApplicationDto app1 = createApplicationDto(1L, "PENDING", 10000000.0);
        ApplicationDto app2 = createApplicationDto(2L, "APPROVED", 20000000.0);
        ApplicationDto app3 = createApplicationDto(3L, "UNDER_REVIEW", 15000000.0);

        when(responseSpec.onStatus(any(Predicate.class), any(Function.class))).thenReturn(responseSpec);
        when(responseSpec.bodyToFlux(ApplicationDto.class))
                .thenReturn(Flux.just(app1, app2)) // Bancolombia: 2 aplicaciones
                .thenReturn(Flux.just(app3)) // Davivienda: 1 aplicación
                .thenReturn(Flux.empty()); // Coltefinanciera: 0 aplicaciones

        // Act & Assert
        StepVerifier.create(service.getAllApplicationsFromBanks(userId, jwtToken))
                .assertNext(applications -> {
                    assertThat(applications).hasSize(3);

                    long bancolombiaCount = applications.stream()
                            .filter(app -> app.getBankName().equals("Bancolombia"))
                            .count();
                    assertThat(bancolombiaCount).isEqualTo(2);

                    long daviviendaCount = applications.stream()
                            .filter(app -> app.getBankName().equals("Davivienda"))
                            .count();
                    assertThat(daviviendaCount).isEqualTo(1);
                })
                .verifyComplete();
    }

    @Test
    void fetchFromBank_WithServerError_ShouldReturnEmptyList() {
        // Arrange
        String userId = "user123";
        String jwtToken = "Bearer valid-token";

        // Simular error en el primer banco, éxito en los otros
        ApplicationDto app = createApplicationDto(1L, "APPROVED", 10000000.0);

        when(responseSpec.onStatus(any(Predicate.class), any(Function.class))).thenReturn(responseSpec);
        when(responseSpec.bodyToFlux(ApplicationDto.class))
                .thenReturn(Flux.error(new RuntimeException("Connection timeout")))
                .thenReturn(Flux.just(app))
                .thenReturn(Flux.empty());

        // Act & Assert
        StepVerifier.create(service.getAllApplicationsFromBanks(userId, jwtToken))
                .assertNext(applications -> {
                    // Solo debe retornar la aplicación del banco que funcionó
                    assertThat(applications).hasSize(1);
                    assertThat(applications.get(0).getBankName()).isEqualTo("Davivienda");
                })
                .verifyComplete();
    }

    @Test
    void fetchFromBank_WithAllBanksFailing_ShouldReturnEmptyList() {
        // Arrange
        String userId = "user123";
        String jwtToken = "Bearer valid-token";

        when(responseSpec.onStatus(any(Predicate.class), any(Function.class))).thenReturn(responseSpec);
        when(responseSpec.bodyToFlux(ApplicationDto.class))
                .thenReturn(Flux.error(new RuntimeException("Service unavailable")))
                .thenReturn(Flux.error(new RuntimeException("Service unavailable")))
                .thenReturn(Flux.error(new RuntimeException("Service unavailable")));

        // Act & Assert
        StepVerifier.create(service.getAllApplicationsFromBanks(userId, jwtToken))
                .assertNext(applications -> {
                    assertThat(applications).isEmpty();
                })
                .verifyComplete();
    }

    @Test
    void fetchFromBank_WithPartialFailures_ShouldReturnSuccessfulResults() {
        // Arrange
        String userId = "user123";
        String jwtToken = "Bearer valid-token";

        ApplicationDto app1 = createApplicationDto(1L, "PENDING", 10000000.0);
        ApplicationDto app2 = createApplicationDto(2L, "APPROVED", 20000000.0);

        when(responseSpec.onStatus(any(Predicate.class), any(Function.class))).thenReturn(responseSpec);
        when(responseSpec.bodyToFlux(ApplicationDto.class))
                .thenReturn(Flux.just(app1)) // Bancolombia: éxito
                .thenReturn(Flux.error(new RuntimeException("Database error"))) // Davivienda: falla
                .thenReturn(Flux.just(app2)); // Coltefinanciera: éxito

        // Act & Assert
        StepVerifier.create(service.getAllApplicationsFromBanks(userId, jwtToken))
                .assertNext(applications -> {
                    assertThat(applications).hasSize(2);
                    assertThat(applications).extracting(BankApplicationDto::getBankName)
                            .containsExactlyInAnyOrder("Bancolombia", "Coltefinanciera");
                    assertThat(applications).extracting(BankApplicationDto::getStatus)
                            .containsExactlyInAnyOrder("PENDING", "APPROVED");
                })
                .verifyComplete();
    }

    @Test
    void fetchFromBank_WithDifferentStatuses_ShouldMapCorrectly() {
        // Arrange
        String userId = "user123";
        String jwtToken = "Bearer valid-token";

        ApplicationDto pending = createApplicationDto(1L, "PENDING", 5000000.0);
        ApplicationDto approved = createApplicationDto(2L, "APPROVED", 10000000.0);
        ApplicationDto rejected = createApplicationDto(3L, "REJECTED", 15000000.0);

        when(responseSpec.onStatus(any(Predicate.class), any(Function.class))).thenReturn(responseSpec);
        when(responseSpec.bodyToFlux(ApplicationDto.class))
                .thenReturn(Flux.just(pending))
                .thenReturn(Flux.just(approved))
                .thenReturn(Flux.just(rejected));

        // Act & Assert
        StepVerifier.create(service.getAllApplicationsFromBanks(userId, jwtToken))
                .assertNext(applications -> {
                    assertThat(applications).hasSize(3);
                    assertThat(applications).extracting(BankApplicationDto::getStatus)
                            .containsExactlyInAnyOrder("PENDING", "APPROVED", "REJECTED");

                    // Verificar que cada aplicación tenga el código de banco correcto
                    assertThat(applications).extracting(BankApplicationDto::getBankCode)
                            .containsExactlyInAnyOrder("BCO", "DAVI", "COLT");
                })
                .verifyComplete();
    }

    @Test
    void fetchFromBank_With4xxClientError_ShouldReturnEmptyListForThatBank() {
        // Arrange
        String userId = "user123";
        String jwtToken = "Bearer valid-token";

        ApplicationDto app = createApplicationDto(1L, "APPROVED", 10000000.0);

        when(responseSpec.onStatus(any(Predicate.class), any(Function.class))).thenReturn(responseSpec);
        when(responseSpec.bodyToFlux(ApplicationDto.class))
                .thenReturn(Flux.error(
                        new WebClientResponseException(404, "Not Found", null, "User not found".getBytes(), null))) // Bancolombia:
                                                                                                                    // 404
                .thenReturn(Flux.just(app)) // Davivienda: éxito
                .thenReturn(Flux.empty()); // Coltefinanciera: vacío

        // Act & Assert
        StepVerifier.create(service.getAllApplicationsFromBanks(userId, jwtToken))
                .assertNext(applications -> {
                    // Solo debe retornar la aplicación del banco exitoso (Davivienda)
                    assertThat(applications).hasSize(1);
                    assertThat(applications.get(0).getBankName()).isEqualTo("Davivienda");
                })
                .verifyComplete();
    }

    @Test
    void fetchFromBank_With5xxServerError_ShouldReturnEmptyListForThatBank() {
        // Arrange
        String userId = "user123";
        String jwtToken = "Bearer valid-token";

        ApplicationDto app1 = createApplicationDto(1L, "PENDING", 5000000.0);
        ApplicationDto app2 = createApplicationDto(2L, "APPROVED", 15000000.0);

        when(responseSpec.onStatus(any(Predicate.class), any(Function.class))).thenReturn(responseSpec);
        when(responseSpec.bodyToFlux(ApplicationDto.class))
                .thenReturn(Flux.just(app1)) // Bancolombia: éxito
                .thenReturn(Flux.error(new WebClientResponseException(500, "Internal Server Error", null, null, null))) // Davivienda:
                                                                                                                        // 500
                .thenReturn(Flux.just(app2)); // Coltefinanciera: éxito

        // Act & Assert
        StepVerifier.create(service.getAllApplicationsFromBanks(userId, jwtToken))
                .assertNext(applications -> {
                    assertThat(applications).hasSize(2);
                    assertThat(applications).extracting(BankApplicationDto::getBankName)
                            .containsExactlyInAnyOrder("Bancolombia", "Coltefinanciera");
                })
                .verifyComplete();
    }

    @Test
    void fetchFromBank_With503ServiceUnavailable_ShouldReturnEmptyListForThatBank() {
        // Arrange
        String userId = "user123";
        String jwtToken = "Bearer valid-token";

        ApplicationDto app = createApplicationDto(1L, "UNDER_REVIEW", 12000000.0);

        when(responseSpec.onStatus(any(Predicate.class), any(Function.class))).thenReturn(responseSpec);
        when(responseSpec.bodyToFlux(ApplicationDto.class))
                .thenReturn(Flux.error(new WebClientResponseException(503, "Service Unavailable", null,
                        "Service temporarily unavailable".getBytes(), null)))
                .thenReturn(Flux.error(new WebClientResponseException(503, "Service Unavailable", null, null, null)))
                .thenReturn(Flux.just(app)); // Solo Coltefinanciera funciona

        // Act & Assert
        StepVerifier.create(service.getAllApplicationsFromBanks(userId, jwtToken))
                .assertNext(applications -> {
                    assertThat(applications).hasSize(1);
                    assertThat(applications.get(0).getBankName()).isEqualTo("Coltefinanciera");
                    assertThat(applications.get(0).getStatus()).isEqualTo("UNDER_REVIEW");
                })
                .verifyComplete();
    }

    @Test
    void fetchFromBank_With401Unauthorized_ShouldReturnEmptyListForThatBank() {
        // Arrange
        String userId = "user123";
        String jwtToken = "Bearer invalid-token";

        ApplicationDto app1 = createApplicationDto(1L, "PENDING", 8000000.0);
        ApplicationDto app2 = createApplicationDto(2L, "APPROVED", 18000000.0);

        when(responseSpec.onStatus(any(Predicate.class), any(Function.class))).thenReturn(responseSpec);
        when(responseSpec.bodyToFlux(ApplicationDto.class))
                .thenReturn(Flux.just(app1)) // Bancolombia: éxito
                .thenReturn(Flux.error(
                        new WebClientResponseException(401, "Unauthorized", null, "Invalid token".getBytes(), null))) // Davivienda:
                                                                                                                      // 401
                .thenReturn(Flux.just(app2)); // Coltefinanciera: éxito

        // Act & Assert
        StepVerifier.create(service.getAllApplicationsFromBanks(userId, jwtToken))
                .assertNext(applications -> {
                    assertThat(applications).hasSize(2);
                    assertThat(applications).extracting(BankApplicationDto::getBankName)
                            .containsExactlyInAnyOrder("Bancolombia", "Coltefinanciera");
                    assertThat(applications).extracting(BankApplicationDto::getAmount)
                            .containsExactlyInAnyOrder(8000000.0, 18000000.0);
                })
                .verifyComplete();
    }

    @Test
    void fetchFromBank_WithMixedHttpErrors_ShouldHandleGracefully() {
        // Arrange
        String userId = "user123";
        String jwtToken = "Bearer valid-token";

        when(responseSpec.onStatus(any(Predicate.class), any(Function.class))).thenReturn(responseSpec);
        when(responseSpec.bodyToFlux(ApplicationDto.class))
                .thenReturn(Flux.error(
                        new WebClientResponseException(404, "Not Found", null, "Resource not found".getBytes(), null))) // Bancolombia:
                                                                                                                        // 404
                .thenReturn(Flux.error(new WebClientResponseException(500, "Internal Server Error", null,
                        "Database error".getBytes(), null))) // Davivienda: 500
                .thenReturn(Flux.error(new WebClientResponseException(503, "Service Unavailable", null, null, null))); // Coltefinanciera:
                                                                                                                       // 503

        // Act & Assert - Todos los bancos fallan, debe retornar lista vacía
        StepVerifier.create(service.getAllApplicationsFromBanks(userId, jwtToken))
                .assertNext(applications -> {
                    assertThat(applications).isEmpty();
                })
                .verifyComplete();
    }

    // Método helper para crear ApplicationDto
    private ApplicationDto createApplicationDto(Long id, String status, Double amount) {
        ApplicationDto dto = new ApplicationDto();
        dto.setId(id);
        dto.setStatus(Application.ApplicationStatus.valueOf(status));
        dto.setUserId("user123");
        dto.setAmount(amount);
        dto.setApplicationDate(LocalDateTime.now());
        dto.setCreditOfferId(100L + id);
        dto.setUserFullName("Test User");
        dto.setCreditOfferDescription("Test Credit Offer");
        return dto;
    }
}

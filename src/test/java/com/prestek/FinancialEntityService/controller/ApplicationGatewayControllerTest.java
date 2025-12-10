package com.prestek.FinancialEntityService.controller;

import com.prestek.FinancialEntityService.dto.BankApplicationDto;
import com.prestek.FinancialEntityService.service.BankAggregationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ApplicationGatewayControllerTest {

    @Mock
    private BankAggregationService aggregationService;

    @InjectMocks
    private ApplicationGatewayController applicationGatewayController;

    private List<BankApplicationDto> mockApplications;

    @BeforeEach
    void setUp() {
        BankApplicationDto app1 = new BankApplicationDto();
        app1.setId(1L);
        app1.setUserId("user123");
        app1.setAmount(10000000.0);
        app1.setStatus("PENDING");
        app1.setApplicationDate(LocalDateTime.now());
        app1.setBankName("Bancolombia");
        app1.setBankCode("BCO");

        BankApplicationDto app2 = new BankApplicationDto();
        app2.setId(2L);
        app2.setUserId("user123");
        app2.setAmount(15000000.0);
        app2.setStatus("APPROVED");
        app2.setApplicationDate(LocalDateTime.now());
        app2.setBankName("Davivienda");
        app2.setBankCode("DAVI");

        mockApplications = Arrays.asList(app1, app2);
    }

    @Test
    void getApplicationsByUser_WithValidUserIdAndToken_ShouldReturnApplications() {
        // Arrange
        String userId = "user123";
        String jwtToken = "Bearer valid-token";
        when(aggregationService.getAllApplicationsFromBanks(anyString(), anyString()))
                .thenReturn(Mono.just(mockApplications));

        // Act & Assert
        StepVerifier.create(applicationGatewayController.getApplicationsByUser(userId, jwtToken))
                .expectNext(mockApplications)
                .verifyComplete();
    }

    @Test
    void getApplicationsByUser_WithoutToken_ShouldStillWork() {
        // Arrange
        String userId = "user123";
        when(aggregationService.getAllApplicationsFromBanks(eq("user123"), isNull()))
                .thenReturn(Mono.just(mockApplications));

        // Act & Assert
        StepVerifier.create(applicationGatewayController.getApplicationsByUser(userId, null))
                .expectNext(mockApplications)
                .verifyComplete();
    }

    @Test
    void getApplicationsByUser_WhenNoApplications_ShouldReturnEmptyList() {
        // Arrange
        String userId = "user456";
        String jwtToken = "Bearer valid-token";
        when(aggregationService.getAllApplicationsFromBanks(anyString(), anyString()))
                .thenReturn(Mono.just(Collections.emptyList()));

        // Act & Assert
        StepVerifier.create(applicationGatewayController.getApplicationsByUser(userId, jwtToken))
                .expectNext(Collections.emptyList())
                .verifyComplete();
    }

    @Test
    void getApplicationsByUser_ServiceError_ShouldPropagateError() {
        // Arrange
        String userId = "user123";
        String jwtToken = "Bearer valid-token";
        when(aggregationService.getAllApplicationsFromBanks(anyString(), anyString()))
                .thenReturn(Mono.error(new RuntimeException("Service unavailable")));

        // Act & Assert
        StepVerifier.create(applicationGatewayController.getApplicationsByUser(userId, jwtToken))
                .expectError(RuntimeException.class)
                .verify();
    }
}

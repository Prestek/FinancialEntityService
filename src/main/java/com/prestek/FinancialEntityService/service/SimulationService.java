package com.prestek.FinancialEntityService.service;

import com.prestek.FinancialEntityService.dto.SimulationResponse;
import com.prestek.FinancialEntityService.dto.SimulationRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

@Service
@RequiredArgsConstructor
@Slf4j
public class SimulationService {

    private final WebClient.Builder webClientBuilder;

    @Value("${N8N_SIMULATION_URL:http://localhost:5678/webhook-test/simulate-credit}")
    private String n8nSimulationUrl;

    /**
     * Envía la solicitud de simulación a n8n que:
     * 1. Valida el input
     * 2. Obtiene historial crediticio
     * 3. Valida políticas de cada banco
     * 4. Llama a quotes de cada banco
     * 5. Compara ofertas y retorna las mejores
     * 
     * @param request             Datos de la simulación
     * @param authorizationHeader JWT token en formato "Bearer {token}"
     * @return Respuesta con las ofertas de los bancos
     */
    public Mono<SimulationResponse> simulateLoan(SimulationRequest request, String authorizationHeader) {

        // Validación básica
        if (!validateRequest(request)) {
            String validationError = getValidationError(request);
            log.warn("❌ Validation failed for user {}: {}", request.getUserId(), validationError);
            return Mono.error(new IllegalArgumentException(validationError));
        }

        log.info("📤 Sending simulation request to N8N");
        log.info("   URL: {}", n8nSimulationUrl);
        log.info("   User: {}", request.getUserId());
        log.info("   Amount: ${}", String.format("%,.0f", request.getAmount()));
        log.info("   Term: {} months", request.getTermMonths());
        log.info("   Income: ${}", String.format("%,.0f", request.getMonthlyIncome()));
        log.info("   Authorization: {}", authorizationHeader != null ? "Present" : "Missing");

        // Validar que el token esté presente
        if (authorizationHeader == null || authorizationHeader.isBlank()) {
            log.error("❌ Authorization header is required");
            return Mono.error(new IllegalArgumentException("Authorization token is required"));
        }

        // Enviar a n8n para procesamiento completo
        return webClientBuilder.build()
                .post()
                .uri(n8nSimulationUrl)
                .header("Authorization", authorizationHeader)
                .bodyValue(request)
                .retrieve()
                .onStatus(
                        status -> status.is4xxClientError() || status.is5xxServerError(),
                        clientResponse -> clientResponse.bodyToMono(String.class)
                                .flatMap(errorBody -> {
                                    log.error("❌ N8N returned error status {}: {}",
                                            clientResponse.statusCode(), errorBody);
                                    return Mono.error(new RuntimeException(
                                            "N8N error: " + clientResponse.statusCode() + " - " + errorBody));
                                }))
                .bodyToMono(SimulationResponse.class)
                .doOnSuccess(response -> {
                    if (response != null && response.getRecommendation() != null) {
                        String bestBank = response.getRecommendation().getBestOption();
                        String riskLevel = response.getRecommendation().getRiskAssessment();

                        log.info("✅ Simulation successful for user {}:", request.getUserId());
                        log.info("   Best option: {}", bestBank);
                        log.info("   Risk assessment: {}", riskLevel);

                        if (response.getAnalysis() != null) {
                            log.info("   Banks analyzed: Bancolombia, Coltefinanciera, Davivienda");
                        }
                    } else {
                        log.warn("⚠️  Simulation returned null response for user {}", request.getUserId());
                    }
                })
                .onErrorResume(error -> {
                    log.error("❌ Error calling N8N simulation webhook");
                    log.error("   URL: {}", n8nSimulationUrl);
                    log.error("   Error: {} - {}", error.getClass().getSimpleName(), error.getMessage());
                    return Mono.error(error);
                });
    }

    private boolean validateRequest(SimulationRequest request) {
        if (request.getUserId() == null || request.getUserId().isBlank()) {
            return false;
        }
        if (request.getAmount() == null || request.getAmount() < 1000000 || request.getAmount() > 50000000) {
            return false;
        }
        if (request.getTermMonths() == null || request.getTermMonths() < 6 || request.getTermMonths() > 60) {
            return false;
        }
        if (request.getMonthlyIncome() == null || request.getMonthlyIncome() <= 0) {
            return false;
        }
        return true;
    }

    private String getValidationError(SimulationRequest request) {
        if (request.getUserId() == null || request.getUserId().isBlank()) {
            return "userId is required";
        }
        if (request.getAmount() == null) {
            return "amount is required";
        }
        if (request.getAmount() < 1000000 || request.getAmount() > 50000000) {
            return "Amount out of range (1M - 50M)";
        }
        if (request.getTermMonths() == null) {
            return "termMonths is required";
        }
        if (request.getTermMonths() < 6 || request.getTermMonths() > 60) {
            return "Term out of range (6-60 months)";
        }
        if (request.getMonthlyIncome() == null) {
            return "monthlyIncome is required";
        }
        if (request.getMonthlyIncome() <= 0) {
            return "monthlyIncome must be greater than zero";
        }
        return "Invalid request";
    }
}

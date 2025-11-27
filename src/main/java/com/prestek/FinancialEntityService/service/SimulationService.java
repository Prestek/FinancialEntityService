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

    @Value("${N8N_SIMULATION_URL:http://localhost:5678/webhook/credit-simulation}")
    private String n8nSimulationUrl;

    /**
     * Envía la solicitud de simulación a n8n que:
     * 1. Valida el input
     * 2. Obtiene historial crediticio
     * 3. Valida políticas de cada banco
     * 4. Llama a quotes de cada banco
     * 5. Compara ofertas y retorna las mejores
     */
    public Mono<SimulationResponse> simulateLoan(SimulationRequest request) {

        // Validación básica
        if (!validateRequest(request)) {
            return Mono.just(SimulationResponse.builder()
                    .success(false)
                    .message("Invalid request parameters")
                    .reason(getValidationError(request))
                    .build());
        }

        log.info("Sending simulation request to n8n for user: {}", request.getUserId());

        // Enviar a n8n para procesamiento completo
        return webClientBuilder.build()
                .post()
                .uri(n8nSimulationUrl)
                .bodyValue(request)
                .retrieve()
                .bodyToMono(SimulationResponse.class)
                .doOnSuccess(response -> {
                    if (response.getSuccess()) {
                        log.info("Simulation successful for user {}: {} offers, best from {}",
                                request.getUserId(),
                                response.getOffersCount(),
                                response.getBestOffer() != null ? response.getBestOffer().getEntity() : "none");
                    } else {
                        log.warn("Simulation rejected for user {}: {}",
                                request.getUserId(),
                                response.getMessage());
                    }
                })
                .onErrorResume(error -> {
                    log.error("Error calling n8n simulation webhook: {}", error.getMessage());
                    return Mono.just(SimulationResponse.builder()
                            .success(false)
                            .message("Service temporarily unavailable")
                            .reason("Unable to process simulation request")
                            .build());
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
        return "Invalid request";
    }
}

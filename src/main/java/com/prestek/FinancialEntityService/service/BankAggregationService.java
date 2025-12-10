package com.prestek.FinancialEntityService.service;

import java.util.Collections;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import com.prestek.FinancialEntityService.config.BankConstants;
import com.prestek.FinancialEntityService.dto.BankApplicationDto;
import com.prestek.FinancialEntityCore.dto.ApplicationDto;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Service
public class BankAggregationService {

    private static final Logger logger = LoggerFactory.getLogger(BankAggregationService.class);

    private final WebClient webClient;

    public BankAggregationService(WebClient.Builder webClientBuilder) {
        this.webClient = webClientBuilder.build();
    }

    public Mono<List<BankApplicationDto>> getAllApplicationsFromBanks(
            String userId,
            String jwtToken) {

        return Flux.fromArray(BankConstants.BankService.values())
                .flatMap(bank -> fetchFromBank(bank, userId, jwtToken))
                .flatMap(Flux::fromIterable)
                .collectList();
    }

    private Mono<List<BankApplicationDto>> fetchFromBank(
            BankConstants.BankService bank,
            String userId,
            String jwtToken) {
        String relativePath = BankConstants.BankPaths.GET_APPLICATIONS_BY_USER.format(userId);
        String url = bank.buildUri(relativePath);

        logger.info("📞 Fetching from {}: {}", bank.bankName(), url);
        logger.debug("   JWT Token: {}",
                jwtToken != null ? jwtToken.substring(0, Math.min(20, jwtToken.length())) + "..." : "NULL");

        return webClient.get()
                .uri(url)
                .header(bank.authHeader(), jwtToken != null ? jwtToken : "")
                .retrieve()
                .onStatus(
                        status -> status.is4xxClientError() || status.is5xxServerError(),
                        clientResponse -> {
                            int statusCode = clientResponse.statusCode().value();
                            return clientResponse.bodyToMono(String.class)
                                    .defaultIfEmpty("No error body")
                                    .flatMap(errorBody -> {
                                        logger.error("❌ {} returned {} - Error body: {}",
                                                bank.bankName(), statusCode, errorBody);
                                        return Mono.error(new RuntimeException(
                                                String.format("%s failed with %d: %s",
                                                        bank.bankName(), statusCode, errorBody)));
                                    });
                        })
                .bodyToFlux(ApplicationDto.class)
                .map(app -> BankApplicationDto.from(app, bank.bankName(), bank.bankCode()))
                .collectList()
                .doOnSuccess(apps -> logger.info("✓ {} returned {} applications", bank.bankName(), apps.size()))
                .onErrorResume(error -> {
                    logger.error("⚠️  {} completely failed - Error type: {} - Message: {}",
                            bank.bankName(),
                            error.getClass().getSimpleName(),
                            error.getMessage());
                    logger.error("   Stack trace: ", error);
                    return Mono.just(Collections.emptyList());
                });
    }
}

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

        return webClient.get()
                .uri(url)
                // propagate JWT to the bank
                .header(bank.authHeader(), jwtToken)
                .retrieve()
                .bodyToFlux(ApplicationDto.class)
                .map(app -> BankApplicationDto.from(app, bank.bankName(), bank.bankCode()))
                .collectList()
                .doOnSuccess(apps -> logger.info("✓ {} returned {} applications", bank.bankName(), apps.size()))
                .onErrorResume(error -> {
                    logger.warn("⚠️  {} failed: {}", bank.bankName(), error.getMessage());
                    return Mono.just(Collections.emptyList());
                });
    }
}

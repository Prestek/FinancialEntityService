package com.prestek.FinancialEntityService.controller;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.prestek.FinancialEntityService.dto.BankApplicationDto;
import com.prestek.FinancialEntityService.service.BankAggregationService;

import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/api/applications")
@RequiredArgsConstructor
public class ApplicationGatewayController {

    private final BankAggregationService aggregationService;

    @GetMapping("/user/{userId}")
    public Mono<List<BankApplicationDto>> getApplicationsByUser(
            @PathVariable String userId,
            @RequestHeader(value = "Authorization", required = false) String jwtToken) {

        return aggregationService.getAllApplicationsFromBanks(userId, jwtToken);
    }
}

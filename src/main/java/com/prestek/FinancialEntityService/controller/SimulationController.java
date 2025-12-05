package com.prestek.FinancialEntityService.controller;

import com.prestek.FinancialEntityService.dto.SimulationResponse;
import com.prestek.FinancialEntityService.dto.SimulationRequest;
import com.prestek.FinancialEntityService.service.SimulationService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/api/simulation")
@RequiredArgsConstructor
public class SimulationController {

    private final SimulationService simulationService;

    @PostMapping
    public Mono<SimulationResponse> simulateLoan(
            @RequestBody SimulationRequest request,
            @RequestHeader(value = "Authorization", required = false) String authorizationHeader) {
        System.out.println("Authorization Header: " + authorizationHeader);
        return simulationService.simulateLoan(request, authorizationHeader);
    }
}

package com.prestek.FinancialEntityService.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SimulationResponse {
    private Boolean success;
    private String message;
    private Integer creditScore;
    private Double requestedAmount;
    private Integer termMonths;
    private Double monthlyIncome;
    private OfferDto bestOffer;
    private List<OfferDto> allOffers;
    private Integer savings;
    private Integer offersCount;
    private String reason; // Para cuando success = false
}

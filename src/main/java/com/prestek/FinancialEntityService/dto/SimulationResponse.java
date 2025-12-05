package com.prestek.FinancialEntityService.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SimulationResponse {
    @JsonProperty("analysis")
    private BankComparisonDto analysis;

    private RecommendationDto recommendation;
}

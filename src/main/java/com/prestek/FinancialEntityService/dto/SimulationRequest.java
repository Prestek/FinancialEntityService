package com.prestek.FinancialEntityService.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SimulationRequest {
    private String userId;
    private Double amount;
    private Integer termMonths;
    private Double monthlyIncome;
}

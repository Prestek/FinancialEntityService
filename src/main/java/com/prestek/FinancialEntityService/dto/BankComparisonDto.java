package com.prestek.FinancialEntityService.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import com.fasterxml.jackson.annotation.JsonProperty;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BankComparisonDto {
    @JsonProperty("bancolombia")
    private BankAnalysisDto bancolombia;

    @JsonProperty("coltefinanciera")
    private BankAnalysisDto coltefinanciera;

    @JsonProperty("davivienda")
    private BankAnalysisDto davivienda;
}

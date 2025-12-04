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
public class BankAnalysisDto {
    private List<String> positives;
    private List<String> negatives;
    private Long monthlyPaymentAvg;
    private Long totalCost;
    private Long totalInterest;
    private Double paymentToIncomeRatio;
}

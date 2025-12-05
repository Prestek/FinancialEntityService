package com.prestek.FinancialEntityService.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OfferDto {
    private String entity;
    private Boolean approved;
    private Double effectiveAnnualRate;
    private Double fees;
    private Integer monthlyPayment;
    private Integer totalPayments;
    private Integer totalCost;
    private Integer totalInterest;
    private String reason;
    private Boolean policyRejected;
    private String codCausal;
}

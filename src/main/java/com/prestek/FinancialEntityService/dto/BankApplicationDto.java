package com.prestek.FinancialEntityService.dto;

import com.prestek.FinancialEntityCore.dto.ApplicationDto;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class BankApplicationDto {

    // Información del banco
    private String bankName;
    private String bankCode;

    // Datos de la aplicación (del ApplicationDto original)
    private Long id;
    private String status;
    private LocalDateTime applicationDate;
    private LocalDateTime reviewDate;
    private LocalDateTime approvalDate;
    private String notes;
    private String rejectionReason;
    private Double amount;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private String userId;
    private Long creditOfferId;
    private String userFullName;
    private String creditOfferDescription;

    // Constructor desde ApplicationDto
    public static BankApplicationDto from(ApplicationDto app, String bankName, String bankCode) {
        BankApplicationDto dto = new BankApplicationDto();
        dto.setBankName(bankName);
        dto.setBankCode(bankCode);
        dto.setId(app.getId());
        dto.setStatus(app.getStatus() != null ? app.getStatus().name() : null);
        dto.setApplicationDate(app.getApplicationDate());
        dto.setReviewDate(app.getReviewDate());
        dto.setApprovalDate(app.getApprovalDate());
        dto.setNotes(app.getNotes());
        dto.setRejectionReason(app.getRejectionReason());
        dto.setAmount(app.getAmount());
        dto.setCreatedAt(app.getCreatedAt());
        dto.setUpdatedAt(app.getUpdatedAt());
        dto.setUserId(app.getUserId());
        dto.setCreditOfferId(app.getCreditOfferId());
        dto.setUserFullName(app.getUserFullName());
        dto.setCreditOfferDescription(app.getCreditOfferDescription());
        return dto;
    }
}

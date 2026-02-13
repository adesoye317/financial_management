package com.financal.mgt.Financal.Management.dto.request.investment;


import com.financal.mgt.Financal.Management.enums.investment.RiskLevel;
import jakarta.validation.constraints.*;
import lombok.*;

import java.math.BigDecimal;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor
public class CreatePoolRequest {

    @NotBlank(message = "Pool name is required")
    private String poolName;

    private String description;

    @NotNull(message = "Risk level is required")
    private RiskLevel riskLevel;

    @NotNull @DecimalMin("0.01")
    private BigDecimal annualReturnPct;

    @NotNull @DecimalMin("1.00")
    private BigDecimal minInvestment;

    @NotNull @DecimalMin("1.00")
    private BigDecimal maxInvestment;

    @NotNull @DecimalMin("1.00")
    private BigDecimal totalPoolSize;

    @NotNull @Min(1)
    private Integer durationMonths;

    private String terms;

    private BigDecimal earlyWithdrawalPenaltyPct;
}

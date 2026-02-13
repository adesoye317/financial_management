package com.financal.mgt.Financal.Management.dto.request.investment;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.math.BigDecimal;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor
public class InvestRequest {

    @NotNull(message = "Pool ID is required")
    private Long poolId;

    @NotNull(message = "Amount is required")
    @DecimalMin(value = "1.00", message = "Minimum investment is ₦1")
    private BigDecimal amount;

    private boolean autoReinvest;
}

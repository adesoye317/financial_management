package com.financal.mgt.Financal.Management.dto.request.wallet;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import java.math.BigDecimal;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor
public class FundWalletRequest {
    @NotNull(message = "Amount is required")
    @DecimalMin(value = "1.00", message = "Minimum funding amount is 1.00")
    private BigDecimal amount;
    private String description;
    @NotBlank(message = "Idempotency key is required")
    private String idempotencyKey;
}

package com.financal.mgt.Financal.Management.dto.response.investment;


import lombok.*;

import java.math.BigDecimal;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class WithdrawResultResponse {
    private String investmentRef;
    private BigDecimal amountInvested;
    private BigDecimal returnsEarned;
    private BigDecimal penalty;
    private BigDecimal totalPayout;
    private String status;
    private BigDecimal walletBalanceAfter;
}


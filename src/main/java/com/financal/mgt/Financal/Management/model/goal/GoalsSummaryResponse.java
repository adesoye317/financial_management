package com.financal.mgt.Financal.Management.model.goal;



import lombok.*;

import java.math.BigDecimal;
import java.util.List;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class GoalsSummaryResponse {
    private int activeGoals;
    private BigDecimal totalSaved;
    private int goalsAchieved;
    private BigDecimal totalTarget;
    private BigDecimal overallProgressPercentage;
    private List<GoalResponse> goals;
}

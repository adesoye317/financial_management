package com.financal.mgt.Financal.Management.dto.response.insight;

import lombok.*;
import java.math.BigDecimal;
import java.util.List;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class GoalInsightResponse {
    private int totalGoals;
    private int activeGoals;
    private int achievedGoals;
    private BigDecimal overallProgress;
    private BigDecimal totalSaved;
    private BigDecimal totalTarget;
    private int onTrackGoals;
    private int behindScheduleGoals;
    private List<GoalDetail> goalDetails;

    @Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
    public static class GoalDetail {
        private String goalName;
        private BigDecimal progress;
        private String status; // ON_TRACK, BEHIND_SCHEDULE
        private BigDecimal monthlyRequired;
        private BigDecimal currentMonthlySaving;
        private BigDecimal shortfallOrSurplus;
        private String projectedCompletion;
    }
}

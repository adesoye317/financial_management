package com.financal.mgt.Financal.Management.dto.response.insight;


import lombok.*;
import java.util.List;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class RecommendationResponse {
    private List<Recommendation> recommendations;

    @Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
    public static class Recommendation {
        private int id;
        private String type;     // SAVINGS, SPENDING, INVESTMENT, GOAL
        private String priority; // HIGH, MEDIUM, LOW
        private String title;
        private String message;
        private boolean actionable;
        private String action;   // ADJUST_GOAL, VIEW_POOLS, FUND_GOAL, etc.
    }
}
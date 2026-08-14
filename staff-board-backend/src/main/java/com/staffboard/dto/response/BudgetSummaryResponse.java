package com.staffboard.dto.response;

import lombok.*;
import java.math.BigDecimal;
import java.util.List;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class BudgetSummaryResponse {
    private long totalEmployees;
    private long whitelistCount;
    private BigDecimal totalCost;
    private List<DepartmentCost> departmentCosts;

    @Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
    public static class DepartmentCost {
        private String department;
        private BigDecimal actualCost;
        private BigDecimal budgetAmount;
        private BigDecimal remaining;
    }
}

package com.staffboard.controller;

import com.staffboard.dto.response.BudgetSummaryResponse;
import com.staffboard.service.BudgetService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/budget")
@RequiredArgsConstructor
public class BudgetController {

    private final BudgetService budgetService;

    @GetMapping("/summary")
    public ResponseEntity<BudgetSummaryResponse> summary() {
        return ResponseEntity.ok(budgetService.getSummary());
    }

    @GetMapping("/department/{dept}")
    public ResponseEntity<List<BudgetSummaryResponse.DepartmentCost>> departmentBudget(
            @PathVariable String dept) {
        return ResponseEntity.ok(budgetService.getDepartmentBudget(dept));
    }

    @GetMapping("/trend")
    public ResponseEntity<BudgetSummaryResponse> trend() {
        return ResponseEntity.ok(budgetService.getSummary());
    }
}

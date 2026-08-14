package com.staffboard.service;

import com.staffboard.dto.response.BudgetSummaryResponse;
import com.staffboard.entity.DepartmentBudget;
import com.staffboard.repository.DepartmentBudgetRepository;
import com.staffboard.repository.EmployeeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class BudgetService {

    private final EmployeeRepository employeeRepository;
    private final DepartmentBudgetRepository budgetRepository;

    @Transactional(readOnly = true)
    public BudgetSummaryResponse getSummary() {
        List<Object[]> deptCosts = employeeRepository.sumCostByDepartment();
        long totalEmployees = employeeRepository.count();
        long whitelistCount = employeeRepository.countWhitelistEmployees();

        BigDecimal totalCost = deptCosts.stream()
            .map(row -> (BigDecimal) row[1])
            .reduce(BigDecimal.ZERO, BigDecimal::add);

        int currentYear = LocalDate.now().getYear();
        int currentQuarter = (LocalDate.now().getMonthValue() - 1) / 3 + 1;

        List<BudgetSummaryResponse.DepartmentCost> departmentCostList = new ArrayList<>();
        for (Object[] row : deptCosts) {
            String dept = (String) row[0];
            BigDecimal actual = (BigDecimal) row[1];
            DepartmentBudget budget = budgetRepository
                .findByDepartmentAndYearAndQuarter(dept, currentYear, currentQuarter)
                .orElse(null);
            BigDecimal budgetAmt = budget != null ? budget.getBudgetAmount() : BigDecimal.ZERO;
            departmentCostList.add(BudgetSummaryResponse.DepartmentCost.builder()
                .department(dept)
                .actualCost(actual)
                .budgetAmount(budgetAmt)
                .remaining(budgetAmt.subtract(actual))
                .build());
        }

        return BudgetSummaryResponse.builder()
            .totalEmployees(totalEmployees)
            .whitelistCount(whitelistCount)
            .totalCost(totalCost)
            .departmentCosts(departmentCostList)
            .build();
    }

    @Transactional(readOnly = true)
    public List<BudgetSummaryResponse.DepartmentCost> getDepartmentBudget(String department) {
        List<DepartmentBudget> budgets = budgetRepository.findByDepartment(department);
        return budgets.stream().map(b -> BudgetSummaryResponse.DepartmentCost.builder()
            .department(b.getDepartment())
            .actualCost(b.getUsedAmount())
            .budgetAmount(b.getBudgetAmount())
            .remaining(b.getBudgetAmount().subtract(b.getUsedAmount()))
            .build()).toList();
    }
}

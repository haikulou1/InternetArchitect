package com.staffboard.entity;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "department_budgets",
       uniqueConstraints = @UniqueConstraint(
           name = "uk_dept_year_quarter",
           columnNames = {"department", "year", "quarter"}))
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class DepartmentBudget {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String department;

    @Column(nullable = false)
    private Integer year;

    @Column(nullable = false)
    private Integer quarter;

    @Column(name = "budget_amount", nullable = false, precision = 15, scale = 2)
    @Builder.Default
    private BigDecimal budgetAmount = BigDecimal.ZERO;

    @Column(name = "used_amount", nullable = false, precision = 15, scale = 2)
    @Builder.Default
    private BigDecimal usedAmount = BigDecimal.ZERO;

    @Column(name = "created_at", nullable = false, updatable = false)
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "updated_at", nullable = false)
    @Builder.Default
    private LocalDateTime updatedAt = LocalDateTime.now();

    @PreUpdate
    public void preUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}

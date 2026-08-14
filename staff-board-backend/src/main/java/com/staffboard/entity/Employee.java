package com.staffboard.entity;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "employees")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Employee {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "employee_no", nullable = false, unique = true, length = 50)
    private String employeeNo;

    @Column(nullable = false, length = 100)
    private String name;

    @Column
    private Integer gender;

    @Column(length = 20)
    private String phone;

    @Column(length = 100)
    private String email;

    @Column(length = 100)
    private String department;

    @Column(length = 100)
    private String position;

    @Column(name = "hire_date")
    private LocalDate hireDate;

    @Column(length = 255)
    private String avatar;

    @Column(length = 50)
    private String education;

    @Column(length = 100)
    private String hometown;

    @Column(length = 500)
    private String skills;

    @Column(columnDefinition = "TEXT")
    private String remarks;

    @Column(nullable = false, length = 20)
    @Builder.Default
    private String status = "PROBATION";

    @Column(name = "contract_start")
    private LocalDate contractStart;

    @Column(name = "contract_end")
    private LocalDate contractEnd;

    @Column(name = "emergency_contact", length = 100)
    private String emergencyContact;

    @Column(name = "emergency_phone", length = 20)
    private String emergencyPhone;

    @Column(name = "base_salary", precision = 12, scale = 2)
    @Builder.Default
    private BigDecimal baseSalary = BigDecimal.ZERO;

    @Column(name = "social_security", precision = 12, scale = 2)
    @Builder.Default
    private BigDecimal socialSecurity = BigDecimal.ZERO;

    @Column(name = "housing_fund", precision = 12, scale = 2)
    @Builder.Default
    private BigDecimal housingFund = BigDecimal.ZERO;

    @Column(precision = 12, scale = 2)
    @Builder.Default
    private BigDecimal allowance = BigDecimal.ZERO;

    @Column(name = "annual_budget", precision = 12, scale = 2)
    @Builder.Default
    private BigDecimal annualBudget = BigDecimal.ZERO;

    @Column(name = "is_whitelist", nullable = false)
    @Builder.Default
    private Integer isWhitelist = 0;

    @Column(name = "whitelist_type", length = 50)
    private String whitelistType;

    @Column(nullable = false)
    @Builder.Default
    private Integer deleted = 0;

    @Column(name = "created_at", nullable = false, updatable = false)
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "updated_at", nullable = false)
    @Builder.Default
    private LocalDateTime updatedAt = LocalDateTime.now();

    @Column(name = "created_by")
    private Long createdBy;

    @Column(name = "updated_by")
    private Long updatedBy;

    @PreUpdate
    public void preUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}

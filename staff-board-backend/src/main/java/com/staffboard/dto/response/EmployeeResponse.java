package com.staffboard.dto.response;

import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class EmployeeResponse {
    private Long id;
    private String employeeNo;
    private String name;
    private Integer gender;
    private String phone;
    private String email;
    private String department;
    private String position;
    private LocalDate hireDate;
    private String avatar;
    private String education;
    private String hometown;
    private String skills;
    private String remarks;
    private String status;
    private LocalDate contractStart;
    private LocalDate contractEnd;
    private String emergencyContact;
    private String emergencyPhone;
    private BigDecimal baseSalary;
    private BigDecimal socialSecurity;
    private BigDecimal housingFund;
    private BigDecimal allowance;
    private BigDecimal annualBudget;
    private Integer isWhitelist;
    private String whitelistType;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}

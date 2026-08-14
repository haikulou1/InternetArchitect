package com.staffboard.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;
import java.math.BigDecimal;
import java.time.LocalDate;

@Getter @Setter
public class EmployeeRequest {
    @NotBlank(message = "工号不能为空")
    @Size(max = 50)
    private String employeeNo;

    @NotBlank(message = "姓名不能为空")
    @Size(max = 100)
    private String name;

    private Integer gender;

    @Size(max = 20)
    private String phone;

    @Size(max = 100)
    private String email;

    @Size(max = 100)
    private String department;

    @Size(max = 100)
    private String position;

    private LocalDate hireDate;
    private String avatar;

    @Size(max = 50)
    private String education;

    @Size(max = 100)
    private String hometown;

    @Size(max = 500)
    private String skills;

    private String remarks;

    private String status;
    private LocalDate contractStart;
    private LocalDate contractEnd;

    @Size(max = 100)
    private String emergencyContact;

    @Size(max = 20)
    private String emergencyPhone;

    private BigDecimal baseSalary;
    private BigDecimal socialSecurity;
    private BigDecimal housingFund;
    private BigDecimal allowance;
    private BigDecimal annualBudget;

    private Integer isWhitelist;
    private String whitelistType;
}

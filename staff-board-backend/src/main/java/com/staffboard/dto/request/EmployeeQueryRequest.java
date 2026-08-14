package com.staffboard.dto.request;

import lombok.Getter;
import lombok.Setter;

@Getter @Setter
public class EmployeeQueryRequest {
    private String department;
    private String status;
    private Integer whitelist;
    private String keyword;
    private int page = 0;
    private int size = 10;
    private String sortBy = "createdAt";
    private String sortDir = "desc";
}

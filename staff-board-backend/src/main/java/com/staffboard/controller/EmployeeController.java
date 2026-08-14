package com.staffboard.controller;

import com.staffboard.dto.request.EmployeeRequest;
import com.staffboard.dto.request.WhitelistRequest;
import com.staffboard.dto.response.EmployeeResponse;
import com.staffboard.dto.response.ImportResultResponse;
import com.staffboard.dto.response.PageResponse;
import com.staffboard.service.EmployeeService;
import com.staffboard.service.ImportService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/employees")
@RequiredArgsConstructor
public class EmployeeController {

    private final EmployeeService employeeService;
    private final ImportService importService;

    @GetMapping
    public ResponseEntity<PageResponse<EmployeeResponse>> list(
            @RequestParam(required = false) String department,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) Integer whitelist,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return ResponseEntity.ok(
            employeeService.list(department, status, whitelist, page, size));
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<EmployeeResponse> create(@Valid @RequestBody EmployeeRequest request) {
        return ResponseEntity.ok(employeeService.create(request));
    }

    @GetMapping("/{id}")
    public ResponseEntity<EmployeeResponse> getById(@PathVariable Long id) {
        return ResponseEntity.ok(employeeService.getById(id));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<EmployeeResponse> update(@PathVariable Long id,
            @Valid @RequestBody EmployeeRequest request) {
        return ResponseEntity.ok(employeeService.update(id, request));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        employeeService.delete(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/import")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ImportResultResponse> importEmployees(
            @RequestParam("file") MultipartFile file) {
        return ResponseEntity.ok(importService.importExcel(file));
    }

    @GetMapping("/template")
    public ResponseEntity<InputStreamResource> downloadTemplate() throws Exception {
        InputStreamResource resource = importService.getTemplate();
        return ResponseEntity.ok()
            .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=employee_template.xlsx")
            .contentType(MediaType.APPLICATION_OCTET_STREAM)
            .body(resource);
    }

    @PutMapping("/whitelist")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> batchSetWhitelist(@Valid @RequestBody WhitelistRequest request) {
        employeeService.batchSetWhitelist(
            request.getEmployeeIds(),
            request.getIsWhitelist(),
            request.getWhitelistType());
        return ResponseEntity.ok().build();
    }
}

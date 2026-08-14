package com.staffboard.service;

import com.staffboard.dto.request.EmployeeRequest;
import com.staffboard.dto.response.EmployeeResponse;
import com.staffboard.dto.response.PageResponse;
import com.staffboard.entity.Employee;
import com.staffboard.exception.BusinessException;
import com.staffboard.exception.ErrorCode;
import com.staffboard.mapper.EmployeeMapper;
import com.staffboard.repository.EmployeeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class EmployeeService {

    private final EmployeeRepository employeeRepository;
    private final EmployeeMapper employeeMapper;

    @Transactional
    public EmployeeResponse create(EmployeeRequest request) {
        if (employeeRepository.existsByEmployeeNo(request.getEmployeeNo())) {
            throw new BusinessException(ErrorCode.EMPLOYEE_NO_DUPLICATE);
        }
        Employee entity = employeeMapper.toEntity(request);
        entity.setDeleted(0);
        Employee saved = employeeRepository.save(entity);
        return employeeMapper.toResponse(saved);
    }

    @Transactional(readOnly = true)
    public EmployeeResponse getById(Long id) {
        Employee employee = findActiveEmployee(id);
        return employeeMapper.toResponse(employee);
    }

    @Transactional
    public EmployeeResponse update(Long id, EmployeeRequest request) {
        Employee employee = findActiveEmployee(id);
        employeeMapper.updateEntityFromRequest(request, employee);
        Employee saved = employeeRepository.save(employee);
        return employeeMapper.toResponse(saved);
    }

    @Transactional
    public void delete(Long id) {
        Employee employee = findActiveEmployee(id);
        employee.setDeleted(1);
        employeeRepository.save(employee);
    }

    @Transactional(readOnly = true)
    public PageResponse<EmployeeResponse> list(String department, String status,
            Integer whitelist, int page, int size) {
        PageRequest pageRequest = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<Employee> employeePage = employeeRepository.findByFilters(
            department, status, whitelist, pageRequest);
        Page<EmployeeResponse> responsePage = employeePage.map(employeeMapper::toResponse);
        return PageResponse.from(responsePage);
    }

    @Transactional
    public void batchSetWhitelist(List<Long> employeeIds, boolean isWhitelist, String whitelistType) {
        List<Employee> employees = employeeRepository.findAllById(employeeIds);
        for (Employee emp : employees) {
            emp.setIsWhitelist(isWhitelist ? 1 : 0);
            emp.setWhitelistType(isWhitelist ? whitelistType : null);
        }
        employeeRepository.saveAll(employees);
    }

    private Employee findActiveEmployee(Long id) {
        Employee employee = employeeRepository.findById(id)
            .orElseThrow(() -> new BusinessException(ErrorCode.EMPLOYEE_NOT_FOUND));
        if (employee.getDeleted() == 1) {
            throw new BusinessException(ErrorCode.EMPLOYEE_NOT_FOUND);
        }
        return employee;
    }
}

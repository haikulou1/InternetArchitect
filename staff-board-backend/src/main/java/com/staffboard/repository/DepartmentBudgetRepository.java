package com.staffboard.repository;

import com.staffboard.entity.DepartmentBudget;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface DepartmentBudgetRepository extends JpaRepository<DepartmentBudget, Long> {
    List<DepartmentBudget> findByDepartment(String department);
    List<DepartmentBudget> findByYearAndQuarter(Integer year, Integer quarter);
    Optional<DepartmentBudget> findByDepartmentAndYearAndQuarter(
        String department, Integer year, Integer quarter);
}

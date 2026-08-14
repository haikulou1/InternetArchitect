package com.staffboard.repository;

import com.staffboard.entity.Employee;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;
import java.util.Optional;

public interface EmployeeRepository extends JpaRepository<Employee, Long> {
    Optional<Employee> findByEmployeeNo(String employeeNo);
    boolean existsByEmployeeNo(String employeeNo);

    @Query("SELECT e FROM Employee e WHERE e.deleted = 0 " +
           "AND (:department IS NULL OR e.department = :department) " +
           "AND (:status IS NULL OR e.status = :status) " +
           "AND (:whitelist IS NULL OR e.isWhitelist = :whitelist)")
    Page<Employee> findByFilters(
        @Param("department") String department,
        @Param("status") String status,
        @Param("whitelist") Integer whitelist,
        Pageable pageable);

    @Query("SELECT e FROM Employee e WHERE e.deleted = 0")
    Page<Employee> findActiveEmployees(Pageable pageable);

    @Query("SELECT e.department, SUM(e.baseSalary + e.socialSecurity + e.housingFund + e.allowance) " +
           "FROM Employee e WHERE e.deleted = 0 GROUP BY e.department")
    List<Object[]> sumCostByDepartment();

    @Query("SELECT COUNT(e) FROM Employee e WHERE e.deleted = 0 AND e.isWhitelist = 1")
    long countWhitelistEmployees();

    List<Employee> findByEmployeeNoIn(List<String> employeeNos);
}

package com.staffboard.service;

import com.staffboard.dto.response.ImportResultResponse;
import com.staffboard.entity.Employee;
import com.staffboard.exception.BusinessException;
import com.staffboard.exception.ErrorCode;
import com.staffboard.repository.EmployeeRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.core.io.ByteArrayInputStream;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.InputStreamResource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class ImportService {

    private final EmployeeRepository employeeRepository;

    private static final long MAX_FILE_SIZE = 10 * 1024 * 1024; // 10MB

    @Transactional
    public ImportResultResponse importExcel(MultipartFile file) {
        validateFile(file);

        List<String> errors = new ArrayList<>();
        int successCount = 0;
        int totalCount = 0;

        try (InputStream is = file.getInputStream();
             Workbook workbook = WorkbookFactory.create(is)) {

            Sheet sheet = workbook.getSheetAt(0);
            int lastRow = sheet.getLastRowNum();

            for (int i = 1; i <= lastRow; i++) {
                totalCount++;
                Row row = sheet.getRow(i);
                if (row == null) continue;

                try {
                    Employee emp = parseRow(row, i);
                    if (employeeRepository.existsByEmployeeNo(emp.getEmployeeNo())) {
                        errors.add("第" + (i + 1) + "行: 工号 " + emp.getEmployeeNo() + " 已存在");
                        continue;
                    }
                    if (emp.getName() == null || emp.getName().isBlank()) {
                        errors.add("第" + (i + 1) + "行: 姓名不能为空");
                        continue;
                    }
                    emp.setDeleted(0);
                    employeeRepository.save(emp);
                    successCount++;
                } catch (Exception e) {
                    errors.add("第" + (i + 1) + "行: " + e.getMessage());
                }
            }
        } catch (IOException e) {
            throw new BusinessException(ErrorCode.IMPORT_FORMAT_ERROR, "文件解析失败: " + e.getMessage());
        }

        return ImportResultResponse.builder()
            .totalCount(totalCount)
            .successCount(successCount)
            .failCount(errors.size())
            .errors(errors)
            .build();
    }

    public InputStreamResource getTemplate() throws IOException {
        ClassPathResource resource = new ClassPathResource("templates/employee_template.xlsx");
        if (resource.exists()) {
            return new InputStreamResource(resource.getInputStream());
        }
        // Generate template on the fly
        Workbook workbook = new XSSFWorkbook();
        Sheet sheet = workbook.createSheet("员工导入模板");
        Row header = sheet.createRow(0);
        String[] headers = {"工号", "姓名", "性别(1男/2女)", "部门", "职位", "手机号",
            "邮箱", "入职日期", "学历", "状态(REGULAR/PROBATION)", "基本工资"};
        for (int i = 0; i < headers.length; i++) {
            Cell cell = header.createCell(i);
            cell.setCellValue(headers[i]);
            CellStyle style = workbook.createCellStyle();
            Font font = workbook.createFont();
            font.setBold(true);
            style.setFont(font);
            cell.setCellStyle(style);
        }
        // Example row
        Row example = sheet.createRow(1);
        example.createCell(0).setCellValue("EMP001");
        example.createCell(1).setCellValue("张三");
        example.createCell(2).setCellValue(1);
        example.createCell(3).setCellValue("技术部");
        example.createCell(4).setCellValue("工程师");
        example.createCell(5).setCellValue("13800138000");
        example.createCell(6).setCellValue("zhangsan@example.com");
        example.createCell(7).setCellValue("2024-01-15");
        example.createCell(8).setCellValue("本科");
        example.createCell(9).setCellValue("REGULAR");
        example.createCell(10).setCellValue(15000);

        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        workbook.write(bos);
        workbook.close();
        return new InputStreamResource(new ByteArrayInputStream(bos.toByteArray()));
    }

    private void validateFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BusinessException(ErrorCode.IMPORT_FILE_EMPTY);
        }
        if (file.getSize() > MAX_FILE_SIZE) {
            throw new BusinessException(ErrorCode.IMPORT_FILE_TOO_LARGE);
        }
        String filename = file.getOriginalFilename();
        if (filename == null || (!filename.endsWith(".xlsx") && !filename.endsWith(".xls")
                && !filename.endsWith(".csv"))) {
            throw new BusinessException(ErrorCode.IMPORT_FORMAT_ERROR, "仅支持 .xlsx/.xls/.csv 格式");
        }
    }

    private Employee parseRow(Row row, int rowIndex) {
        Employee emp = new Employee();
        emp.setEmployeeNo(getCellStringValue(row, 0));
        emp.setName(getCellStringValue(row, 1));

        Cell genderCell = row.getCell(2);
        if (genderCell != null && genderCell.getCellType() == CellType.NUMERIC) {
            emp.setGender((int) genderCell.getNumericCellValue());
        }

        emp.setDepartment(getCellStringValue(row, 3));
        emp.setPosition(getCellStringValue(row, 4));
        emp.setPhone(getCellStringValue(row, 5));
        emp.setEmail(getCellStringValue(row, 6));

        String hireDateStr = getCellStringValue(row, 7);
        if (hireDateStr != null && !hireDateStr.isBlank()) {
            emp.setHireDate(java.time.LocalDate.parse(hireDateStr));
        }

        emp.setEducation(getCellStringValue(row, 8));

        String statusStr = getCellStringValue(row, 9);
        emp.setStatus(statusStr != null && !statusStr.isBlank() ? statusStr : "PROBATION");

        Cell salaryCell = row.getCell(10);
        if (salaryCell != null && salaryCell.getCellType() == CellType.NUMERIC) {
            emp.setBaseSalary(BigDecimal.valueOf(salaryCell.getNumericCellValue()));
        }

        return emp;
    }

    private String getCellStringValue(Row row, int colIndex) {
        Cell cell = row.getCell(colIndex);
        if (cell == null) return null;
        return switch (cell.getCellType()) {
            case STRING -> cell.getStringCellValue().trim();
            case NUMERIC -> String.valueOf((long) cell.getNumericCellValue());
            case BOOLEAN -> String.valueOf(cell.getBooleanCellValue());
            default -> null;
        };
    }
}

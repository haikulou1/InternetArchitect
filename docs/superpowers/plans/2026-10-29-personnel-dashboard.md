# 人员看板（Personnel Dashboard）实施计划

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 构建一个基于 Spring Boot 3 的单体 Web 应用，实现员工信息 CRUD、成本科目白名单维护、员工×科目预算额度/实际支出/预留记录、成本预算看板，以及员工/预算/白名单三类数据的 Excel/CSV「全对才导入」批量导入。

**Architecture:** 前后端不分离的单体应用，Thymeleaf 服务端渲染 + Bootstrap 风格页面；Controller 只负责页面渲染与表单绑定，Service 承载业务规则（白名单校验、预算对比、导入整表校验与事务），MyBatis Mapper 访问 MySQL 8，`ImportParser` 把 Excel/CSV 行解析为 DTO 并逐行校验，`@Transactional` 保证整批写入原子性。

**Tech Stack:** Java 17、Spring Boot 3.2.5、MyBatis（mybatis-spring-boot-starter 3.0.3）、PageHelper（pagehelper-spring-boot-starter 2.1.0）、Spring Security 6、Thymeleaf、Apache POI 5.2.5（.xlsx）、Apache Commons CSV 1.10.0、MySQL 8、Maven、H2（集成测试）、Lombok（构建期，减少样板代码）。

## Global Constraints

- 全新单体工程，项目根目录 `personnel-dashboard/`，坐标 `com.company:personnel-dashboard`，基础包 `com.company.personnel`。
- JDK 17、Spring Boot 3.x（Jakarta EE，`jakarta.*` 命名空间，禁止 `javax.*`）。
- MySQL 8.x；集成测试用 H2（`MODE=MySQL` 兼容模式）。
- Thymeleaf 服务端渲染 + Bootstrap；风格沿用仓库内 `03 OA账户权限管理` 参考工程。
- 导入为「全对才导入」：任一数据行校验失败则整批回滚，返回全部错误行号 + 字段 + 原因；全部合法才单事务一次性写入。
- 权限矩阵：管理员=全部操作；普通用户=员工/看板/预算只读 + 仅本人基本信息可维护；导入/白名单维护仅管理员。
- 金额 `DECIMAL(12,2)` 非负两位小数；期间格式 `YYYY-MM`。
- 唯一约束：员工 `emp_no`、科目 `project_code`、登录名 `username`、预算三元组 `(employee_id, project_id, period)`。
- 管理员种子账号 `admin`（BCrypt 密码，首次登录提示修改）。
- 文件上传大小限制 ≤ 5MB。

**已确认决策（相对设计文档的补充，本轮已与用户确认）：**
1. 新工程放在仓库根目录下独立目录 `personnel-dashboard/`（现有仓库内容是课程资料，不在其内修改）。
2. 坐标/包名取 `com.company` / `com.company.personnel`。
3. 额外引入 Lombok（`provided` 作用域）用于生成 getter/setter，避免 6 张表实体的样板代码；不改变任何业务行为。
4. 种子管理员账号 `admin`，初始化密码 `admin123`（BCrypt，首次登录后提示修改）。

---

## 文件结构总览

```
personnel-dashboard/
├── pom.xml
├── .gitignore
└── src
    ├── main
    │   ├── java/com/company/personnel
    │   │   ├── PersonnelDashboardApplication.java
    │   │   ├── config/SecurityConfig.java
    │   │   ├── controller/{LoginController,EmployeeController,BudgetProjectController,BudgetController,DashboardController,ImportController}.java
    │   │   ├── service/{EmployeeService,BudgetProjectService,BudgetService,DashboardService,ImportService}.java
    │   │   ├── service/impl/{...Impl}.java
    │   │   ├── mapper/{EmployeeMapper,BudgetProjectMapper,EmployeeBudgetMapper,BudgetActualMapper,BudgetReserveMapper,SysUserMapper}.java
    │   │   ├── domain/{Employee,BudgetProject,EmployeeBudget,BudgetActual,BudgetReserve,SysUser}.java
    │   │   ├── dto/{EmployeeImportRow,BudgetImportRow,ProjectImportRow,ImportReceipt,ImportError,BudgetBoardRow,...}.java
    │   │   ├── importers/{ImportParser,ExcelImportParser,CsvImportParser,EmployeeImportValidator,BudgetImportValidator,ProjectImportValidator}.java
    │   │   └── common/{BusinessException,GlobalExceptionHandler,PageResult,PeriodUtil,MoneyUtil,enums/{Gender,EmpStatus,ProjectStatus,UserRole,UserStatus}.java}
    │   └── resources
    │       ├── application.yml
    │       ├── db/schema.sql
    │       ├── db/data.sql
    │       ├── templates/{login,layout,employee-list,employee-form,employee-detail,budgetproject-list,budgetproject-form,budget-record,dashboard,import}.html
    │       ├── static/css/app.css
    │       ├── mapper/*.xml
    │       └── templates/download/{employee-template.csv|.xlsx,budget-template...,project-template...}
    └── test/java/com/company/personnel/{...}
```

---

### Task 1: 工程脚手架与数据库脚本

**Files:**
- Create: `personnel-dashboard/pom.xml`
- Create: `personnel-dashboard/.gitignore`
- Create: `personnel-dashboard/src/main/java/com/company/personnel/PersonnelDashboardApplication.java`
- Create: `personnel-dashboard/src/main/resources/application.yml`
- Create: `personnel-dashboard/src/main/resources/db/schema.sql`
- Create: `personnel-dashboard/src/main/resources/db/data.sql`

**Interfaces:**
- Consumes: 无
- Produces: 可启动的 Spring Boot 3 应用骨架；数据库表结构与种子数据（后续所有任务依赖）。

- [ ] **Step 1: 创建 pom.xml**

```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 https://maven.apache.org/xsd/maven-4.0.0.xsd">
  <modelVersion>4.0.0</modelVersion>
  <parent>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-parent</artifactId>
    <version>3.2.5</version>
    <relativePath/>
  </parent>
  <groupId>com.company</groupId>
  <artifactId>personnel-dashboard</artifactId>
  <version>0.0.1-SNAPSHOT</version>
  <name>personnel-dashboard</name>
  <description>人员看板</description>

  <properties>
    <java.version>17</java.version>
  </properties>

  <dependencies>
    <dependency>
      <groupId>org.springframework.boot</groupId>
      <artifactId>spring-boot-starter-web</artifactId>
    </dependency>
    <dependency>
      <groupId>org.springframework.boot</groupId>
      <artifactId>spring-boot-starter-thymeleaf</artifactId>
    </dependency>
    <dependency>
      <groupId>org.springframework.boot</groupId>
      <artifactId>spring-boot-starter-security</artifactId>
    </dependency>
    <dependency>
      <groupId>org.springframework.boot</groupId>
      <artifactId>spring-boot-starter-validation</artifactId>
    </dependency>
    <dependency>
      <groupId>org.mybatis.spring.boot</groupId>
      <artifactId>mybatis-spring-boot-starter</artifactId>
      <version>3.0.3</version>
    </dependency>
    <dependency>
      <groupId>com.github.pagehelper</groupId>
      <artifactId>pagehelper-spring-boot-starter</artifactId>
      <version>2.1.0</version>
    </dependency>
    <dependency>
      <groupId>com.mysql</groupId>
      <artifactId>mysql-connector-j</artifactId>
      <scope>runtime</scope>
    </dependency>
    <dependency>
      <groupId>org.apache.poi</groupId>
      <artifactId>poi-ooxml</artifactId>
      <version>5.2.5</version>
    </dependency>
    <dependency>
      <groupId>org.apache.commons</groupId>
      <artifactId>commons-csv</artifactId>
      <version>1.10.0</version>
    </dependency>
    <dependency>
      <groupId>org.projectlombok</groupId>
      <artifactId>lombok</artifactId>
      <optional>true</optional>
    </dependency>
    <dependency>
      <groupId>org.springframework.boot</groupId>
      <artifactId>spring-boot-starter-test</artifactId>
      <scope>test</scope>
    </dependency>
    <dependency>
      <groupId>org.springframework.security</groupId>
      <artifactId>spring-security-test</artifactId>
      <scope>test</scope>
    </dependency>
    <dependency>
      <groupId>com.h2database</groupId>
      <artifactId>h2</artifactId>
      <scope>test</scope>
    </dependency>
  </dependencies>

  <build>
    <plugins>
      <plugin>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-maven-plugin</artifactId>
        <configuration>
          <excludes>
            <exclude>
              <groupId>org.projectlombok</groupId>
              <artifactId>lombok</artifactId>
            </exclude>
          </excludes>
        </configuration>
      </plugin>
    </plugins>
  </build>
</project>
```

- [ ] **Step 2: 创建 .gitignore 与应用入口**

`personnel-dashboard/.gitignore`：

```
target/
*.class
*.log
.DS_Store
.idea/
*.iml
```

`personnel-dashboard/src/main/java/com/company/personnel/PersonnelDashboardApplication.java`：

```java
package com.company.personnel;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@MapperScan("com.company.personnel.mapper")
public class PersonnelDashboardApplication {
    public static void main(String[] args) {
        SpringApplication.run(PersonnelDashboardApplication.class, args);
    }
}
```

- [ ] **Step 3: 编写 application.yml**

```yaml
server:
  port: 8080

spring:
  datasource:
    url: jdbc:mysql://localhost:3306/personnel?useSSL=false&serverTimezone=UTC&characterEncoding=utf8
    username: root
    password: root
    driver-class-name: com.mysql.cj.jdbc.Driver
  sql:
    init:
      mode: always
      schema-locations: classpath:db/schema.sql
      data-locations: classpath:db/data.sql
  servlet:
    multipart:
      max-file-size: 5MB
      max-request-size: 5MB
  thymeleaf:
    cache: false

mybatis:
  mapper-locations: classpath:mapper/*.xml
  configuration:
    map-underscore-to-camel-case: true

pagehelper:
  helper-dialect: mysql
  reasonable: true
  support-methods-arguments: true
```

- [ ] **Step 4: 编写 schema.sql（MySQL 8 建表）**

```sql
DROP TABLE IF EXISTS budget_reserve;
DROP TABLE IF EXISTS budget_actual;
DROP TABLE IF EXISTS employee_budget;
DROP TABLE IF EXISTS budget_project;
DROP TABLE IF EXISTS sys_user;
DROP TABLE IF EXISTS employee;

CREATE TABLE employee (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    emp_no VARCHAR(32) NOT NULL,
    name VARCHAR(64) NOT NULL,
    gender VARCHAR(8),
    birth_date DATE,
    phone VARCHAR(20),
    email VARCHAR(128),
    department VARCHAR(64),
    position VARCHAR(64),
    rank VARCHAR(32),
    superior VARCHAR(64),
    hire_date DATE,
    emp_status VARCHAR(16) NOT NULL DEFAULT '在职',
    employment_type VARCHAR(16),
    cost_center VARCHAR(64),
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_emp_no (emp_no)
);

CREATE TABLE budget_project (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    project_code VARCHAR(32) NOT NULL,
    project_name VARCHAR(128) NOT NULL,
    category VARCHAR(32),
    status VARCHAR(16) NOT NULL DEFAULT '启用',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_project_code (project_code)
);

CREATE TABLE employee_budget (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    employee_id BIGINT NOT NULL,
    project_id BIGINT NOT NULL,
    period VARCHAR(16) NOT NULL,
    budget_amount DECIMAL(12,2) NOT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_emp_project_period (employee_id, project_id, period)
);

CREATE TABLE budget_actual (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    employee_id BIGINT NOT NULL,
    project_id BIGINT NOT NULL,
    period VARCHAR(16) NOT NULL,
    actual_amount DECIMAL(12,2) NOT NULL,
    remark VARCHAR(255),
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

CREATE TABLE budget_reserve (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    employee_id BIGINT NOT NULL,
    project_id BIGINT NOT NULL,
    period VARCHAR(16) NOT NULL,
    reserve_amount DECIMAL(12,2) NOT NULL,
    remark VARCHAR(255),
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

CREATE TABLE sys_user (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    employee_id BIGINT NULL,
    username VARCHAR(64) NOT NULL,
    password VARCHAR(128) NOT NULL,
    role VARCHAR(16) NOT NULL DEFAULT 'USER',
    status VARCHAR(16) NOT NULL DEFAULT '启用',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_username (username)
);
```

- [ ] **Step 5: 编写 data.sql（种子数据）**

```sql
-- 种子白名单（成本科目）
INSERT INTO budget_project (project_code, project_name, category, status) VALUES
('PRJ-0001', '基础工资', '人力成本', '启用'),
('PRJ-0002', '绩效奖金', '人力成本', '启用'),
('PRJ-0003', '社保公积金', '法定福利', '启用'),
('PRJ-0004', '差旅费用', '运营成本', '启用'),
('PRJ-0005', '培训费用', '运营成本', '停用');

-- 种子员工
INSERT INTO employee (emp_no, name, gender, birth_date, phone, email, department, position, rank, superior, hire_date, emp_status, employment_type, cost_center)
VALUES ('E0001', '张三', '男', '1990-05-12', '13800000001', 'zhangsan@company.com', '研发部', '工程师', 'P6', '李四', '2020-01-06', '在职', '正式', 'RD-01');

-- 种子管理员（密码 admin123，BCrypt；首次登录提示修改）
-- 注意：下方哈希值须在执行 Task 4 时用 BCryptPasswordEncoder 生成后替换
INSERT INTO sys_user (employee_id, username, password, role, status)
VALUES (NULL, 'admin', '{bcrypt}$2a$10$REPLACEME_REPLACEME_REPLACEME_REPLACEME_REPLACEME', 'ADMIN', '启用');
```

- [ ] **Step 6: 编译验证**

Run: `mvn -q -f personnel-dashboard/pom.xml compile`
Expected: PASS，`target/classes` 生成。

- [ ] **Step 7: Commit**

```bash
git add personnel-dashboard/pom.xml personnel-dashboard/.gitignore \
  personnel-dashboard/src/main/java/com/company/personnel/PersonnelDashboardApplication.java \
  personnel-dashboard/src/main/resources/application.yml \
  personnel-dashboard/src/main/resources/db/schema.sql \
  personnel-dashboard/src/main/resources/db/data.sql
git commit -m "feat(personnel): scaffold Spring Boot 3 project and DB schema"
```

---### Task 2: 领域实体与通用工具

**Files:**
- Create: `personnel-dashboard/src/main/java/com/company/personnel/common/BusinessException.java`
- Create: `personnel-dashboard/src/main/java/com/company/personnel/common/enums/Gender.java`
- Create: `personnel-dashboard/src/main/java/com/company/personnel/common/enums/EmpStatus.java`
- Create: `personnel-dashboard/src/main/java/com/company/personnel/common/enums/ProjectStatus.java`
- Create: `personnel-dashboard/src/main/java/com/company/personnel/common/enums/UserRole.java`
- Create: `personnel-dashboard/src/main/java/com/company/personnel/common/enums/UserStatus.java`
- Create: `personnel-dashboard/src/main/java/com/company/personnel/domain/Employee.java`
- Create: `personnel-dashboard/src/main/java/com/company/personnel/domain/BudgetProject.java`
- Create: `personnel-dashboard/src/main/java/com/company/personnel/domain/EmployeeBudget.java`
- Create: `personnel-dashboard/src/main/java/com/company/personnel/domain/BudgetActual.java`
- Create: `personnel-dashboard/src/main/java/com/company/personnel/domain/BudgetReserve.java`
- Create: `personnel-dashboard/src/main/java/com/company/personnel/domain/SysUser.java`

**Interfaces:**
- Consumes: Task 1 的包结构
- Produces: 实体/枚举/异常类，后续 Service、Mapper、Controller 统一引用。
  - `BusinessException(String message)` / `BusinessException(String message, Throwable cause)`
  - 每个实体含 Lombok `@Data` + `@NoArgsConstructor` + `@AllArgsConstructor`，字段名与 schema 驼峰一致（`empMap`）。

- [ ] **Step 1: 编写通用枚举**

```java
package com.company.personnel.common.enums;

public enum Gender { 男, 女, 其他 }
```

```java
package com.company.personnel.common.enums;

public enum EmpStatus { 在职, 离职 }
```

```java
package com.company.personnel.common.enums;

public enum ProjectStatus { 启用, 停用 }
```

```java
package com.company.personnel.common.enums;

public enum UserRole { ADMIN, USER }
```

```java
package com.company.personnel.common.enums;

public enum UserStatus { 启用, 停用 }
```

- [ ] **Step 2: 编写 BusinessException**

```java
package com.company.personnel.common;

public class BusinessException extends RuntimeException {
    public BusinessException(String message) {
        super(message);
    }

    public BusinessException(String message, Throwable cause) {
        super(message, cause);
    }
}
```

- [ ] **Step 3: 编写 6 个领域实体（Employee 与 SysUser 示例）**

```java
package com.company.personnel.domain;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class Employee {
    private Long id;
    private String empNo;
    private String name;
    private String gender;
    private LocalDate birthDate;
    private String phone;
    private String email;
    private String department;
    private String position;
    private String rank;
    private String superior;
    private LocalDate hireDate;
    private String empStatus;
    private String employmentType;
    private String costCenter;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
```

```java
package com.company.personnel.domain;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SysUser {
    private Long id;
    private Long employeeId;
    private String username;
    private String password;
    private String role;
    private String status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
```

其余四个实体按同样模式（字段与 schema 一一对应）：`BudgetProject(id, projectCode, projectName, category, status, createdAt, updatedAt)`；`EmployeeBudget(id, employeeId, projectId, period, budgetAmount(BigDecimal), createdAt, updatedAt)`；`BudgetActual(id, employeeId, projectId, period, actualAmount(BigDecimal), remark, createdAt, updatedAt)`；`BudgetReserve(id, employeeId, projectId, period, reserveAmount(BigDecimal), remark, createdAt, updatedAt)`。

- [ ] **Step 4: 编译验证**

Run: `mvn -q -f personnel-dashboard/pom.xml compile`
Expected: PASS。

- [ ] **Step 5: Commit**

```bash
git add personnel-dashboard/src/main/java/com/company/personnel/common \
  personnel-dashboard/src/main/java/com/company/personnel/domain
git commit -m "feat(personnel): add domain entities, enums, and business exception"
```

---

### Task 3: MyBatis Mapper 接口与 XML

**Files:**
- Create: `personnel-dashboard/src/main/java/com/company/personnel/mapper/EmployeeMapper.java`
- Create: `personnel-dashboard/src/main/java/com/company/personnel/mapper/BudgetProjectMapper.java`
- Create: `personnel-dashboard/src/main/java/com/company/personnel/mapper/EmployeeBudgetMapper.java`
- Create: `personnel-dashboard/src/main/java/com/company/personnel/mapper/BudgetActualMapper.java`
- Create: `personnel-dashboard/src/main/java/com/company/personnel/mapper/BudgetReserveMapper.java`
- Create: `personnel-dashboard/src/main/java/com/company/personnel/mapper/SysUserMapper.java`
- Create: `personnel-dashboard/src/main/resources/mapper/EmployeeMapper.xml`
- Create: `personnel-dashboard/src/main/resources/mapper/BudgetProjectMapper.xml`
- Create: `personnel-dashboard/src/main/resources/mapper/EmployeeBudgetMapper.xml`
- Create: `personnel-dashboard/src/main/resources/mapper/BudgetActualMapper.xml`
- Create: `personnel-dashboard/src/main/resources/mapper/BudgetReserveMapper.xml`
- Create: `personnel-dashboard/src/main/resources/mapper/SysUserMapper.xml`

**Interfaces:**
- Consumes: Task 2 的实体类。
- Produces: 数据访问方法，供 Service 使用：
  - `EmployeeMapper`：`List<Employee> selectByCondition(@Param("empNo") String empNo, @Param("name") String name, @Param("department") String department, @Param("empStatus") String empStatus)`、`Employee selectById(Long id)`、`int insert(Employee e)`、`int update(Employee e)`、`int deleteById(Long id)`、`int countByEmpNo(String empNo)`、`int countByEmpNoExcludeId(@Param("empNo") String empNo, @Param("id") Long id)`。
  - `BudgetProjectMapper`：`List<BudgetProject> selectAll()`、`BudgetProject selectById(Long id)`、`int insert(BudgetProject p)`、`int update(BudgetProject p)`、`int deleteById(Long id)`、`int countByCode(String code)`、`int countByCodeExcludeId(...)`。
  - `EmployeeBudgetMapper`：`int insert(EmployeeBudget b)`、`int update(EmployeeBudget b)`、`int deleteById(Long id)`、`int countByKey(@Param("employeeId") Long, @Param("projectId") Long, @Param("period") String)`、`int countByKeyExcludeId(...)`、`List<EmployeeBudget> selectByEmployee(Long employeeId)`、Dashboard 关联方法（见 Task 7）。
  - `BudgetActualMapper`/`BudgetReserveMapper`：`insert`/`update`/`deleteById`/`selectByEmployee`。
  - `SysUserMapper`：`SysUser selectByUsername(String username)`、`int insert(SysUser u)`。

- [ ] **Step 1: 编写 Mapper 接口（EmployeeMapper 示例）**

```java
package com.company.personnel.mapper;

import com.company.personnel.domain.Employee;
import org.apache.ibatis.annotations.Param;
import java.util.List;

public interface EmployeeMapper {
    List<Employee> selectByCondition(@Param("empNo") String empNo,
                                     @Param("name") String name,
                                     @Param("department") String department,
                                     @Param("empStatus") String empStatus);

    Employee selectById(Long id);
    int insert(Employee e);
    int update(Employee e);
    int deleteById(Long id);
    int countByEmpNo(String empNo);
    int countByEmpNoExcludeId(@Param("empNo") String empNo, @Param("id") Long id);
}
```

其余接口同构，签名见上方 **Interfaces** 块（不得改名）。

- [ ] **Step 2: 编写 XML（EmployeeMapper.xml 示例）**

```xml
<?xml version="1.0" encoding="UTF-8"?>
<!DOCTYPE mapper PUBLIC "-//mybatis.org//DTD Mapper 3.0//EN"
        "http://mybatis.org/dtd/mybatis-3-mapper.dtd">
<mapper namespace="com.company.personnel.mapper.EmployeeMapper">

    <sql id="cols">
        id, emp_no, name, gender, birth_date, phone, email, department,
        position, rank, superior, hire_date, emp_status, employment_type,
        cost_center, created_at, updated_at
    </sql>

    <select id="selectByCondition" resultType="com.company.personnel.domain.Employee">
        SELECT <include refid="cols"/> FROM employee
        <where>
            <if test="empNo != null and empNo != ''">AND emp_no LIKE CONCAT('%', #{empNo}, '%')</if>
            <if test="name != null and name != ''">AND name LIKE CONCAT('%', #{name}, '%')</if>
            <if test="department != null and department != ''">AND department = #{department}</if>
            <if test="empStatus != null and empStatus != ''">AND emp_status = #{empStatus}</if>
        </where>
        ORDER BY id DESC
    </select>

    <select id="selectById" resultType="com.company.personnel.domain.Employee">
        SELECT <include refid="cols"/> FROM employee WHERE id = #{id}
    </select>

    <insert id="insert" parameterType="com.company.personnel.domain.Employee" useGeneratedKeys="true" keyProperty="id">
        INSERT INTO employee (emp_no, name, gender, birth_date, phone, email, department,
                              position, rank, superior, hire_date, emp_status, employment_type, cost_center)
        VALUES (#{empNo}, #{name}, #{gender}, #{birthDate}, #{phone}, #{email}, #{department},
                #{position}, #{rank}, #{superior}, #{hireDate}, #{empStatus}, #{employmentType}, #{costCenter})
    </insert>

    <update id="update" parameterType="com.company.personnel.domain.Employee">
        UPDATE employee SET emp_no=#{empNo}, name=#{name}, gender=#{gender}, birth_date=#{birthDate},
            phone=#{phone}, email=#{email}, department=#{department}, position=#{position},
            rank=#{rank}, superior=#{superior}, hire_date=#{hireDate}, emp_status=#{empStatus},
            employment_type=#{employmentType}, cost_center=#{costCenter}
        WHERE id=#{id}
    </update>

    <delete id="deleteById">
        DELETE FROM employee WHERE id = #{id}
    </delete>

    <select id="countByEmpNo" resultType="int">
        SELECT COUNT(*) FROM employee WHERE emp_no = #{empNo}
    </select>

    <select id="countByEmpNoExcludeId" resultType="int">
        SELECT COUNT(*) FROM employee WHERE emp_no = #{empNo} AND id != #{id}
    </select>

</mapper>
```

其余 XML 同构（命名空间、resultType、字段映射与实体一一对应；`map-underscore-to-camel-case` 已开启）。

- [ ] **Step 3: 编译验证**

Run: `mvn -q -f personnel-dashboard/pom.xml compile`
Expected: PASS。

- [ ] **Step 4: Commit**

```bash
git add personnel-dashboard/src/main/java/com/company/personnel/mapper \
  personnel-dashboard/src/main/resources/mapper
git commit -m "feat(personnel): add MyBatis mappers and XML"
```

---### Task 4: 安全配置与登录

**Files:**
- Create: `personnel-dashboard/src/main/java/com/company/personnel/config/SecurityConfig.java`
- Create: `personnel-dashboard/src/main/java/com/company/personnel/service/impl/UserDetailsServiceImpl.java`（或并入 `SysUserService`，本计划采用独立实现）
- Modify: `personnel-dashboard/src/main/java/com/company/personnel/service/SysUserService.java`（新建接口）与 `impl/SysUserServiceImpl.java`
- Create: `personnel-dashboard/src/main/java/com/company/personnel/controller/LoginController.java`
- Create: `personnel-dashboard/src/main/resources/templates/login.html`

**Interfaces:**
- Consumes: Task 3 的 `SysUserMapper`。
- Produces:
  - `SysUserService.findByUsername(String)` 返回 `SysUser`。
  - Spring Security 会话登录，`/login` 表单 POST，默认成功跳转 `/dashboard`。
  - 方法级权限注解供后续 Controller 使用：管理员用 `hasRole('ADMIN')`，普通用户用 `hasRole('USER')`。

- [ ] **Step 1: 编写 SecurityConfig**

```java
package com.company.personnel.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/login", "/css/**", "/js/**", "/webjars/**").permitAll()
                .anyRequest().authenticated())
            .formLogin(form -> form
                .loginPage("/login")
                .defaultSuccessUrl("/dashboard", false)
                .permitAll())
            .logout(logout -> logout.logoutSuccessUrl("/login?logout").permitAll());
        return http.build();
    }
}
```

- [ ] **Step 2: 编写 SysUserService**

```java
package com.company.personnel.service;

import com.company.personnel.domain.SysUser;

public interface SysUserService {
    SysUser findByUsername(String username);
}
```

```java
package com.company.personnel.service.impl;

import com.company.personnel.domain.SysUser;
import com.company.personnel.mapper.SysUserMapper;
import com.company.personnel.service.SysUserService;
import org.springframework.stereotype.Service;

@Service
public class SysUserServiceImpl implements SysUserService {
    private final SysUserMapper sysUserMapper;

    public SysUserServiceImpl(SysUserMapper sysUserMapper) {
        this.sysUserMapper = sysUserMapper;
    }

    @Override
    public SysUser findByUsername(String username) {
        return sysUserMapper.selectByUsername(username);
    }
}
```

- [ ] **Step 3: 编写 UserDetailsService**

```java
package com.company.personnel.service.impl;

import com.company.personnel.domain.SysUser;
import com.company.personnel.service.SysUserService;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
public class UserDetailsServiceImpl implements UserDetailsService {
    private final SysUserService sysUserService;

    public UserDetailsServiceImpl(SysUserService sysUserService) {
        this.sysUserService = sysUserService;
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        SysUser u = sysUserService.findByUsername(username);
        if (u == null || !"启用".equals(u.getStatus())) {
            throw new UsernameNotFoundException("用户不存在或已停用");
        }
        return User.withUsername(u.getUsername())
            .password(u.getPassword())
            .roles(u.getRole()) // ADMIN / USER
            .build();
    }
}
```

- [ ] **Step 4: 编写 LoginController 与 login.html**

```java
package com.company.personnel.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class LoginController {
    @GetMapping("/login")
    public String login() {
        return "login";
    }
}
```

`templates/login.html`（Bootstrap，Security 默认表单 POST `/login` 带 `username`/`password` 字段）：

```html
<!DOCTYPE html>
<html xmlns:th="http://www.thymeleaf.org">
<head>
  <meta charset="UTF-8">
  <title>登录 - 人员看板</title>
  <link rel="stylesheet" href="https://cdn.jsdelivr.net/npm/bootstrap@5.3.0/dist/css/bootstrap.min.css">
</head>
<body>
<div class="container" style="max-width: 420px; margin-top: 100px;">
  <h3 class="mb-4">人员看板</h3>
  <form th:action="@{/login}" method="post">
    <div class="mb-3">
      <label class="form-label">用户名</label>
      <input type="text" name="username" class="form-control" required>
    </div>
    <div class="mb-3">
      <label class="form-label">密码</label>
      <input type="password" name="password" class="form-control" required>
    </div>
    <div class="text-danger mb-3" th:if="${param.error}">用户名或密码错误</div>
    <button type="submit" class="btn btn-primary w-100">登录</button>
  </form>
</div>
</body>
</html>
```

- [ ] **Step 5: 生成 admin 密码哈希并替换 data.sql 占位符**

Run: `mvn -q -f personnel-dashboard/pom.xml test -Dtest=PasswordHashGen -DfailIfNoTests=false -Dexec.mainClass=...`
若采用一次性工具类，则：新增 `test/java/com/company/personnel/PasswordHashGen.java`（临时，勿提交）打印 `new BCryptPasswordEncoder().encode("admin123")`，把输出哈希替换回 `data.sql` 中 `{bcrypt}$2a$10$REPLACEME...`（保留 `{bcrypt}` 前缀）。完成后删除该临时工具类。

Expected: `data.sql` 含真实 BCrypt 哈希。

- [ ] **Step 6: 编译验证**

Run: `mvn -q -f personnel-dashboard/pom.xml compile`
Expected: PASS。

- [ ] **Step 7: Commit**

```bash
git add personnel-dashboard/src/main/java/com/company/personnel/config \
  personnel-dashboard/src/main/java/com/company/personnel/service \
  personnel-dashboard/src/main/java/com/company/personnel/controller/LoginController.java \
  personnel-dashboard/src/main/resources/templates/login.html \
  personnel-dashboard/src/main/resources/db/data.sql
git commit -m "feat(personnel): add security config and login"
```

---

### Task 5: 员工 CRUD

**Files:**
- Create: `personnel-dashboard/src/main/java/com/company/personnel/service/EmployeeService.java`
- Create: `personnel-dashboard/src/main/java/com/company/personnel/service/impl/EmployeeServiceImpl.java`
- Create: `personnel-dashboard/src/main/java/com/company/personnel/common/PageResult.java`
- Create: `personnel-dashboard/src/main/java/com/company/personnel/controller/EmployeeController.java`
- Create: `personnel-dashboard/src/main/resources/templates/layout.html`（Thymeleaf 布局片段）
- Create: `personnel-dashboard/src/main/resources/templates/employee-list.html`
- Create: `personnel-dashboard/src/main/resources/templates/employee-form.html`
- Create: `personnel-dashboard/src/main/resources/templates/employee-detail.html`
- Create: `personnel-dashboard/src/main/resources/templates/error/403.html`

**Interfaces:**
- Consumes: Task 3 的 `EmployeeMapper`；Task 4 的安全注解。
- Produces:
  - `EmployeeService.save(Employee)`：新建时校验 `emp_no` 唯一；业务错误抛 `BusinessException`。
  - `EmployeeService.findPage(int pageNum, int pageSize, String name, String empNo, String department, String empStatus)` 返回 `PageResult<Employee>`。
  - `EmployeeService.getById(Long id)`、`delete(Long id)`。
  - `PageResult<T>`：`List<T> list`、`long total`、`int pageNum`、`int pageSize`。
  - 路径：`GET /employees`（列表、分页、条件查询）、`GET /employees/new`、`POST /employees/save`、`GET /employees/{id}/edit`、`GET /employees/{id}`（详情）、`POST /employees/{id}/delete`。
- [ ] **Step 1: 编写 PageResult**

```java
package com.company.personnel.common;

import java.util.List;

public class PageResult<T> {
    private final List<T> list;
    private final long total;
    private final int pageNum;
    private final int pageSize;

    public PageResult(List<T> list, long total, int pageNum, int pageSize) {
        this.list = list;
        this.total = total;
        this.pageNum = pageNum;
        this.pageSize = pageSize;
    }

    public List<T> getList() { return list; }
    public long getTotal() { return total; }
    public int getPageNum() { return pageNum; }
    public int getPageSize() { return pageSize; }
    public long getPages() { return (total + pageSize - 1) / pageSize; }
}
```

- [ ] **Step 2: 编写 EmployeeService（完整实现）**

```java
package com.company.personnel.service.impl;

import com.company.personnel.common.BusinessException;
import com.company.personnel.common.PageResult;
import com.company.personnel.domain.Employee;
import com.company.personnel.mapper.EmployeeMapper;
import com.company.personnel.service.EmployeeService;
import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class EmployeeServiceImpl implements EmployeeService {
    private final EmployeeMapper employeeMapper;

    public EmployeeServiceImpl(EmployeeMapper employeeMapper) {
        this.employeeMapper = employeeMapper;
    }

    @Override
    @Transactional
    public void save(Employee e) {
        if (e.getId() == null) {
            if (employeeMapper.countByEmpNo(e.getEmpNo()) > 0) {
                throw new BusinessException("员工编号已存在：" + e.getEmpNo());
            }
            employeeMapper.insert(e);
        } else {
            if (employeeMapper.countByEmpNoExcludeId(e.getEmpNo(), e.getId()) > 0) {
                throw new BusinessException("员工编号已存在：" + e.getEmpNo());
            }
            employeeMapper.update(e);
        }
    }

    @Override
    @Transactional
    public void delete(Long id) {
        employeeMapper.deleteById(id);
    }

    @Override
    public Employee getById(Long id) {
        return employeeMapper.selectById(id);
    }

    @Override
    public PageResult<Employee> findPage(int pageNum, int pageSize, String name,
                                         String empNo, String department, String empStatus) {
        PageHelper.startPage(pageNum, pageSize);
        List<Employee> list = employeeMapper.selectByCondition(empNo, name, department, empStatus);
        PageInfo<Employee> info = new PageInfo<>(list);
        return new PageResult<>(info.getList(), info.getTotal(), pageNum, pageSize);
    }
}
```

- [ ] **Step 3: 编写 EmployeeController**

```java
package com.company.personnel.controller;

import com.company.personnel.common.BusinessException;
import com.company.personnel.common.PageResult;
import com.company.personnel.domain.Employee;
import com.company.personnel.service.EmployeeService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/employees")
public class EmployeeController {
    private final EmployeeService employeeService;

    public EmployeeController(EmployeeService employeeService) {
        this.employeeService = employeeService;
    }

    @GetMapping
    public String list(@RequestParam(defaultValue = "1") int pageNum,
                       @RequestParam(defaultValue = "10") int pageSize,
                       @RequestParam(required = false) String name,
                       @RequestParam(required = false) String empNo,
                       @RequestParam(required = false) String department,
                       @RequestParam(required = false) String empStatus,
                       Model model) {
        PageResult<Employee> page = employeeService.findPage(pageNum, pageSize, name, empNo, department, empStatus);
        model.addAttribute("page", page);
        model.addAttribute("q", java.util.Map.of("name", name == null ? "" : name,
                "empNo", empNo == null ? "" : empNo,
                "department", department == null ? "" : department,
                "empStatus", empStatus == null ? "" : empStatus));
        return "employee-list";
    }

    @GetMapping("/new")
    @PreAuthorize("hasRole('ADMIN')")
    public String newForm(Model model) {
        model.addAttribute("employee", new Employee());
        return "employee-form";
    }

    @PostMapping("/save")
    @PreAuthorize("hasRole('ADMIN')")
    public String save(@ModelAttribute Employee employee, RedirectAttributes ra) {
        try {
            employeeService.save(employee);
            ra.addFlashAttribute("ok", "已保存");
        } catch (BusinessException e) {
            ra.addFlashAttribute("err", e.getMessage());
        }
        return "redirect:/employees";
    }

    @GetMapping("/{id}/edit")
    @PreAuthorize("hasRole('ADMIN')")
    public String edit(@PathVariable Long id, Model model) {
        model.addAttribute("employee", employeeService.getById(id));
        return "employee-form";
    }

    @GetMapping("/{id}")
    public String detail(@PathVariable Long id, Model model) {
        model.addAttribute("employee", employeeService.getById(id));
        return "employee-detail";
    }

    @PostMapping("/{id}/delete")
    @PreAuthorize("hasRole('ADMIN')")
    public String delete(@PathVariable Long id) {
        employeeService.delete(id);
        return "redirect:/employees";
    }
}
```

> 注：普通用户「仅本人基本信息可维护」在 Task 6 中以「本人资料不覆盖他人」的约束实现（详见下方限定说明）。

- [ ] **Step 4: 编写 Thymeleaf 视图**（`layout.html` 提供布局片段，`employee-list.html` 至少含分页翻页与查询表单；`employee-form.html` 含全部字段输入；`employee-detail.html` 只读展示；`error/403.html` 提示无权限）。字段绑定名与实体属性一致（如 `th:field="*{empNo}"`、姓名、性别、出生日期、手机号、邮箱、部门、岗位、职级、直属上级、入职日期、状态、用工类型、成本中心）。

- [ ] **Step 5: 编写集成测试（可留到 Task 10 统一验证，本步只需启动冒烟）**

Run: `mvn -q -f personnel-dashboard/pom.xml test`
Expected: 通过（若尚未有测试，用 `-DfailIfNoTests=false`）。

- [ ] **Step 6: 运行冒烟**

Run: `mvn -q -f personnel-dashboard/pom.xml spring-boot:run`（需要本地 MySQL 及 `personnel` 库；仅验证启动日志无异常）
Expected: 启动成功，访问 `/login` 返回登录页。

- [ ] **Step 7: Commit**

```bash
git add personnel-dashboard/src/main/java/com/company/personnel/service \
  personnel-dashboard/src/main/java/com/company/personnel/common/PageResult.java \
  personnel-dashboard/src/main/java/com/company/personnel/controller/EmployeeController.java \
  personnel-dashboard/src/main/resources/templates
git commit -m "feat(personnel): employee CRUD with paging and views"
```

---> 权限补充说明（对应设计文档 6.7「普通用户仅本人基本信息可维护」）：由于普通用户关联 `sys_user.employee_id`，实现「仅本人可维护」需在服务层对比当前登录用户关联的员工 ID。该机制在 Task 6 末尾以明确服务方法落地。当前员工 CRUD 的管理员侧路径完整；普通用户侧「本人资料维护」见 Task 6 Step 5 的 `EmployeeService.updateOwnProfile`。

---

### Task 6: 成本科目白名单维护 + 普通用户本人资料维护

**Files:**
- Create: `personnel-dashboard/src/main/java/com/company/personnel/service/BudgetProjectService.java`
- Create: `personnel-dashboard/src/main/java/com/company/personnel/service/impl/BudgetProjectServiceImpl.java`
- Create: `personnel-dashboard/src/main/java/com/company/personnel/dto/CurrentUser.java`（Security 上下文辅助）
- Create: `personnel-dashboard/src/main/java/com/company/personnel/controller/BudgetProjectController.java`
- Create: `personnel-dashboard/src/main/resources/templates/budgetproject-list.html`
- Create: `personnel-dashboard/src/main/resources/templates/budgetproject-form.html`
- Create: `personnel-dashboard/src/main/resources/templates/profile-form.html`（普通用户本人资料）
- Modify: `personnel-dashboard/src/main/java/com/company/personnel/service/EmployeeService.java` + `EmployeeServiceImpl.java`（新增 `updateOwnProfile`）

**Interfaces:**
- Consumes: Task 3 的 `BudgetProjectMapper`、`SysUserMapper`；Task 4 的安全上下文。
- Produces:
  - `BudgetProjectService.save(BudgetProject)`（管理员，校验 `project_code` 唯一）、`delete(Long)`、`getById(Long)`、`findAll()`（启用与停用均返回，供下拉）。
  - `EmployeeService.updateOwnProfile(Employee)`：仅允许 `sys_user.employee_id` 等于传入 `employee.id`，否则抛 `BusinessException("无权修改他人资料")`。
  - `CurrentUser.username()` / `CurrentUser.employeeId()` / `CurrentUser.isAdmin()`：从 `SecurityContextHolder` 读取。

- [ ] **Step 1: 编写 CurrentUser 辅助**

```java
package com.company.personnel.dto;

import com.company.personnel.domain.SysUser;
import com.company.personnel.mapper.SysUserMapper;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

@Component
public class CurrentUser {
    private final SysUserMapper sysUserMapper;

    public CurrentUser(SysUserMapper sysUserMapper) {
        this.sysUserMapper = sysUserMapper;
    }

    public String username() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return auth == null ? null : auth.getName();
    }

    public boolean isAdmin() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return auth != null && auth.getAuthorities().stream()
                .anyMatch(g -> g.getAuthority().equals("ROLE_ADMIN"));
    }

    public Long employeeId() {
        String name = username();
        if (name == null) return null;
        SysUser u = sysUserMapper.selectByUsername(name);
        return u == null ? null : u.getEmployeeId();
    }
}
```

- [ ] **Step 2: 补充 EmployeeService.updateOwnProfile**

```java
// EmployeeService 接口新增
void updateOwnProfile(Employee e);
```

```java
// EmployeeServiceImpl 新增依赖 CurrentUser，方法实现：
@Override
@Transactional
public void updateOwnProfile(Employee e) {
    Long ownId = currentUser.employeeId();
    if (ownId == null || !ownId.equals(e.getId())) {
        throw new BusinessException("无权修改他人资料");
    }
    // 普通用户不可改工号与成本中心
    employeeMapper.updateOwnProfile(e);
}
```

`EmployeeMapper` 与 `EmployeeMapper.xml` 同步新增 `updateOwnProfile(Employee e)`（UPDATE 语句不含 `emp_no` 与 `cost_center`）。

- [ ] **Step 3: 编写 BudgetProjectService**

```java
package com.company.personnel.service.impl;

import com.company.personnel.common.BusinessException;
import com.company.personnel.domain.BudgetProject;
import com.company.personnel.mapper.BudgetProjectMapper;
import com.company.personnel.service.BudgetProjectService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class BudgetProjectServiceImpl implements BudgetProjectService {
    private final BudgetProjectMapper budgetProjectMapper;

    public BudgetProjectServiceImpl(BudgetProjectMapper budgetProjectMapper) {
        this.budgetProjectMapper = budgetProjectMapper;
    }

    @Override
    @Transactional
    public void save(BudgetProject p) {
        if (p.getId() == null) {
            if (budgetProjectMapper.countByCode(p.getProjectCode()) > 0) {
                throw new BusinessException("科目编码已存在：" + p.getProjectCode());
            }
            budgetProjectMapper.insert(p);
        } else {
            if (budgetProjectMapper.countByCodeExcludeId(p.getProjectCode(), p.getId()) > 0) {
                throw new BusinessException("科目编码已存在：" + p.getProjectCode());
            }
            budgetProjectMapper.update(p);
        }
    }

    @Override
    @Transactional
    public void delete(Long id) {
        budgetProjectMapper.deleteById(id);
    }

    @Override
    public BudgetProject getById(Long id) {
        return budgetProjectMapper.selectById(id);
    }

    @Override
    public List<BudgetProject> findAll() {
        return budgetProjectMapper.selectAll();
    }
}
```

（接口 `BudgetProjectService` 声明对应五个方法。）

- [ ] **Step 4: 编写 BudgetProjectController**（全部写操作 `@PreAuthorize("hasRole('ADMIN')")`；列表页普通用户只读可见；`budgetproject-list.html` 展示启停用状态；`budgetproject-form.html` 含编码/名称/分类/状态字段；`profile-form.html` 供普通用户维护本人资料，仅暴露姓名、性别、出生日期、手机号、邮箱、部门、岗位、职级、直属上级、入职日期字段，不含工号与成本中心）。

- [ ] **Step 5: 编译验证**

Run: `mvn -q -f personnel-dashboard/pom.xml compile`
Expected: PASS。

- [ ] **Step 6: Commit**

```bash
git add personnel-dashboard/src/main/java/com/company/personnel/service/BudgetProjectService.java \
  personnel-dashboard/src/main/java/com/company/personnel/service/impl/BudgetProjectServiceImpl.java \
  personnel-dashboard/src/main/java/com/company/personnel/dto/CurrentUser.java \
  personnel-dashboard/src/main/java/com/company/personnel/controller/BudgetProjectController.java \
  personnel-dashboard/src/main/java/com/company/personnel/service/EmployeeService.java \
  personnel-dashboard/src/main/java/com/company/personnel/service/impl/EmployeeServiceImpl.java \
  personnel-dashboard/src/main/java/com/company/personnel/mapper/EmployeeMapper.java \
  personnel-dashboard/src/main/resources/mapper/EmployeeMapper.xml \
  personnel-dashboard/src/main/resources/templates
git commit -m "feat(personnel): whitelist maintenance and own-profile update"
```

---

### Task 7: 预算额度 + 实际支出 + 预留记录

**Files:**
- Create: `personnel-dashboard/src/main/java/com/company/personnel/service/BudgetService.java`
- Create: `personnel-dashboard/src/main/java/com/company/personnel/service/impl/BudgetServiceImpl.java`
- Create: `personnel-dashboard/src/main/java/com/company/personnel/controller/BudgetController.java`
- Create: `personnel-dashboard/src/main/resources/templates/budget-record.html`
- Create: `personnel-dashboard/src/main/java/com/company/personnel/dto/BudgetRecordForm.java`（表单绑定）

**Interfaces:**
- Consumes: Task 3 的 `EmployeeBudgetMapper`、`BudgetActualMapper`、`BudgetReserveMapper`、`BudgetProjectMapper`；Task 6 的 `CurrentUser`。
- Produces:
  - `BudgetService.saveBudget(EmployeeBudget)`：校验 `period` 匹配 `^\d{4}-(0[1-9]|1[0-2])$`；`budget_amount` 非负；`project_id` 命中启用白名单（`BudgetProject.status == 启用`），否则 `BusinessException`；三元组冲突抛 `BusinessException`。
  - `BudgetService.saveActual(BudgetActual)` / `saveReserve(BudgetReserve)` 同校验。
  - 路径：`GET /budget/record`（记录页，含三个表单）、`POST /budget/budget/save`、`POST /budget/actual/save`、`POST /budget/reserve/save`（均 `@PreAuthorize("hasRole('ADMIN')")`）。

- [ ] **Step 1: 编写 PeriodUtil 与 MoneyUtil**

```java
package com.company.personnel.common;

import java.util.regex.Pattern;

public final class PeriodUtil {
    private static final Pattern PERIOD = Pattern.compile("^\\d{4}-(0[1-9]|1[0-2])$");

    private PeriodUtil() {}

    public static boolean isValid(String period) {
        return period != null && PERIOD.matcher(period).matches();
    }
}
```

```java
package com.company.personnel.common;

import com.company.personnel.common.BusinessException;
import java.math.BigDecimal;

public final class MoneyUtil {
    private MoneyUtil() {}

    public static void ensureNonNegative(BigDecimal amount) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) < 0) {
            throw new BusinessException("金额必须为非负");
        }
        if (amount.scale() > 2) {
            throw new BusinessException("金额最多两位小数");
        }
    }
}
```

- [ ] **Step 2: 编写 BudgetService**

```java
package com.company.personnel.service.impl;

import com.company.personnel.common.BusinessException;
import com.company.personnel.common.PeriodUtil;
import com.company.personnel.common.MoneyUtil;
import com.company.personnel.domain.*;
import com.company.personnel.mapper.*;
import com.company.personnel.service.BudgetService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class BudgetServiceImpl implements BudgetService {
    private final EmployeeBudgetMapper budgetMapper;
    private final BudgetActualMapper actualMapper;
    private final BudgetReserveMapper reserveMapper;
    private final BudgetProjectMapper projectMapper;

    public BudgetServiceImpl(EmployeeBudgetMapper budgetMapper, BudgetActualMapper actualMapper,
                             BudgetReserveMapper reserveMapper, BudgetProjectMapper projectMapper) {
        this.budgetMapper = budgetMapper;
        this.actualMapper = actualMapper;
        this.reserveMapper = reserveMapper;
        this.projectMapper = projectMapper;
    }

    private void validateCommon(Long projectId, String period, java.math.BigDecimal amount) {
        if (!PeriodUtil.isValid(period)) throw new BusinessException("期间格式须为 YYYY-MM");
        MoneyUtil.ensureNonNegative(amount);
        BudgetProject p = projectMapper.selectById(projectId);
        if (p == null || !"启用".equals(p.getStatus())) {
            throw new BusinessException("成本科目不存在或已停用（白名单未命中）");
        }
    }

    @Override
    @Transactional
    public void saveBudget(EmployeeBudget b) {
        validateCommon(b.getProjectId(), b.getPeriod(), b.getBudgetAmount());
        if (budgetMapper.countByKey(b.getEmployeeId(), b.getProjectId(), b.getPeriod()) > 0) {
            throw new BusinessException("该员工在该科目该期间已存在预算额度，请勿重复录入");
        }
        budgetMapper.insert(b);
    }

    @Override
    @Transactional
    public void saveActual(BudgetActual a) {
        validateCommon(a.getProjectId(), a.getPeriod(), a.getActualAmount());
        actualMapper.insert(a);
    }

    @Override
    @Transactional
    public void saveReserve(BudgetReserve r) {
        validateCommon(r.getProjectId(), r.getPeriod(), r.getReserveAmount());
        reserveMapper.insert(r);
    }
}
```

- [ ] **Step 3: 编写 BudgetController 与 budget-record.html**（页面提供员工下拉（来自 `EmployeeMapper.selectByCondition(null,null,null,"在职")`）、科目下拉（仅 `status=启用`）、期间输入框、金额输入框；三块表单分别提交上述三个 POST 端点，成功后 `redirect:/budget/record` 携带 flash 消息）。

- [ ] **Step 4: 编译验证**

Run: `mvn -q -f personnel-dashboard/pom.xml compile`
Expected: PASS。

- [ ] **Step 5: Commit**

```bash
git add personnel-dashboard/src/main/java/com/company/personnel/service/BudgetService.java \
  personnel-dashboard/src/main/java/com/company/personnel/service/impl/BudgetServiceImpl.java \
  personnel-dashboard/src/main/java/com/company/personnel/common/PeriodUtil.java \
  personnel-dashboard/src/main/java/com/company/personnel/common/MoneyUtil.java \
  personnel-dashboard/src/main/java/com/company/personnel/controller/BudgetController.java \
  personnel-dashboard/src/main/java/com/company/personnel/dto/BudgetRecordForm.java \
  personnel-dashboard/src/main/resources/templates/budget-record.html
git commit -m "feat(personnel): budget/actual/reserve records with whitelist validation"
```

---

### Task 8: 成本预算看板（Dashboard）

**Files:**
- Create: `personnel-dashboard/src/main/java/com/company/personnel/dto/BudgetBoardRow.java`
- Create: `personnel-dashboard/src/main/java/com/company/personnel/service/DashboardService.java`
- Create: `personnel-dashboard/src/main/java/com/company/personnel/service/impl/DashboardServiceImpl.java`
- Create: `personnel-dashboard/src/main/java/com/company/personnel/controller/DashboardController.java`
- Create: `personnel-dashboard/src/main/resources/templates/dashboard.html`
- Modify: `personnel-dashboard/src/main/resources/mapper/EmployeeBudgetMapper.xml`（看板聚合 SQL）

**Interfaces:**
- Consumes: Task 3/7 的数据。
- Produces:
  - `BudgetBoardRow`：`employeeId`、`empNo`、`employeeName`、`department`、`projectId`、`projectCode`、`projectName`、`period`、`budgetAmount`、`actualAmount`、`reserveAmount`、`remaining(BigDecimal = budget-实际-预留)`。
  - `DashboardService.board(String department, String projectCode, String period)` 返回 `List<BudgetBoardRow>`，按指定维度过滤；为空过滤时返回全量。
  - 路径：`GET /dashboard`（登录后首页，普通用户只读）。

- [ ] **Step 1: 编写 BudgetBoardRow**

```java
package com.company.personnel.dto;

import java.math.BigDecimal;

public class BudgetBoardRow {
    private Long employeeId;
    private String empNo;
    private String employeeName;
    private String department;
    private Long projectId;
    private String projectCode;
    private String projectName;
    private String period;
    private BigDecimal budgetAmount;
    private BigDecimal actualAmount;
    private BigDecimal reserveAmount;

    public BudgetBoardRow() {}

    public BigDecimal getRemaining() {
        BigDecimal b = budgetAmount == null ? BigDecimal.ZERO : budgetAmount;
        BigDecimal a = actualAmount == null ? BigDecimal.ZERO : actualAmount;
        BigDecimal r = reserveAmount == null ? BigDecimal.ZERO : reserveAmount;
        return b.subtract(a).subtract(r);
    }
    // getter / setter 省略（Lombok 可选 @Data，此处显式列出关键字段）
}
```

- [ ] **Step 2: 编写聚合 SQL（EmployeeBudgetMapper.xml 追加）**

```xml
<select id="selectBoardRows" resultType="com.company.personnel.dto.BudgetBoardRow">
    SELECT e.id AS employeeId, e.emp_no AS empNo, e.name AS employeeName, e.department,
           p.id AS projectId, p.project_code AS projectCode, p.project_name AS projectName,
           b.period,
           b.budget_amount AS budgetAmount,
           COALESCE(a.actual_amount, 0) AS actualAmount,
           COALESCE(r.reserve_amount, 0) AS reserveAmount
    FROM employee_budget b
    JOIN employee e ON e.id = b.employee_id
    JOIN budget_project p ON p.id = b.project_id
    LEFT JOIN (SELECT employee_id, project_id, period, SUM(actual_amount) AS actual_amount
               FROM budget_actual GROUP BY employee_id, project_id, period) a
        ON a.employee_id = b.employee_id AND a.project_id = b.project_id AND a.period = b.period
    LEFT JOIN (SELECT employee_id, project_id, period, SUM(reserve_amount) AS reserve_amount
               FROM budget_reserve GROUP BY employee_id, project_id, period) r
        ON r.employee_id = b.employee_id AND r.project_id = b.project_id AND r.period = b.period
    <where>
        <if test="department != null and department != ''">AND e.department = #{department}</if>
        <if test="projectCode != null and projectCode != ''">AND p.project_code = #{projectCode}</if>
        <if test="period != null and period != ''">AND b.period = #{period}</if>
    </where>
    ORDER BY e.department, e.emp_no, p.project_code, b.period
</select>
```

`EmployeeBudgetMapper` 接口同步新增：

```java
List<BudgetBoardRow> selectBoardRows(@Param("department") String department,
                                     @Param("projectCode") String projectCode,
                                     @Param("period") String period);
```

- [ ] **Step 3: 编写 DashboardService 与 DashboardController**（Service 直接透传 Mapper 聚合；Controller 注入 `CurrentUser` 不加写权限，普通用户只读；`dashboard.html` 用 Bootstrap 表格展示各列与「剩余」列，顶部提供部门/科目/期间过滤表单）。

- [ ] **Step 4: 编译验证**

Run: `mvn -q -f personnel-dashboard/pom.xml compile`
Expected: PASS。

- [ ] **Step 5: Commit**

```bash
git add personnel-dashboard/src/main/java/com/company/personnel/dto/BudgetBoardRow.java \
  personnel-dashboard/src/main/java/com/company/personnel/service/DashboardService.java \
  personnel-dashboard/src/main/java/com/company/personnel/service/impl/DashboardServiceImpl.java \
  personnel-dashboard/src/main/java/com/company/personnel/controller/DashboardController.java \
  personnel-dashboard/src/main/java/com/company/personnel/mapper/EmployeeBudgetMapper.java \
  personnel-dashboard/src/main/resources/mapper/EmployeeBudgetMapper.xml \
  personnel-dashboard/src/main/resources/templates/dashboard.html
git commit -m "feat(personnel): cost budget dashboard with aggregation"
```

---### Task 9: 批量导入（员工 / 预算 / 白名单）

**Files:**
- Create: `personnel-dashboard/src/main/java/com/company/personnel/dto/ImportReceipt.java`
- Create: `personnel-dashboard/src/main/java/com/company/personnel/dto/ImportError.java`
- Create: `personnel-dashboard/src/main/java/com/company/personnel/dto/EmployeeImportRow.java`
- Create: `personnel-dashboard/src/main/java/com/company/personnel/dto/BudgetImportRow.java`
- Create: `personnel-dashboard/src/main/java/com/company/personnel/dto/ProjectImportRow.java`
- Create: `personnel-dashboard/src/main/java/com/company/personnel/importers/ImportParser.java`
- Create: `personnel-dashboard/src/main/java/com/company/personnel/importers/ExcelImportParser.java`
- Create: `personnel-dashboard/src/main/java/com/company/personnel/importers/CsvImportParser.java`
- Create: `personnel-dashboard/src/main/java/com/company/personnel/importers/EmployeeImportValidator.java`
- Create: `personnel-dashboard/src/main/java/com/company/personnel/importers/BudgetImportValidator.java`
- Create: `personnel-dashboard/src/main/java/com/company/personnel/importers/ProjectImportValidator.java`
- Create: `personnel-dashboard/src/main/java/com/company/personnel/service/ImportService.java`
- Create: `personnel-dashboard/src/main/java/com/company/personnel/service/impl/ImportServiceImpl.java`
- Create: `personnel-dashboard/src/main/java/com/company/personnel/controller/ImportController.java`
- Create: `personnel-dashboard/src/main/resources/templates/import.html`
- Create: `personnel-dashboard/src/main/resources/templates/download/employee-template.csv`
- Create: `personnel-dashboard/src/main/resources/templates/download/budget-template.csv`
- Create: `personnel-dashboard/src/main/resources/templates/download/project-template.csv`

**Interfaces:**
- Consumes: Task 3 的 Mapper、Task 7 的 `PeriodUtil`/`MoneyUtil`。
- Produces:
  - `ImportReceipt`：`boolean success`、`String message`、`int importedRows`、`List<ImportError> errors`。
  - `ImportError`：`int row`、`String field`、`String reason`。
  - `ImportParser.parse(MultipartFile)` 返回 `List<String[]>`（每行单元格字符串数组），格式自动识别（`.xlsx`→POI、`.csv`→Commons CSV）。
  - `ImportService.importEmployees(MultipartFile)` / `importBudgets(...)` / `importProjects(...)`：整表校验全部行，任一失败返回错误回执而不写库；全部通过则 `@Transactional` 一次性写入。

- [ ] **Step 1: 编写回执与行 DTO**

```java
package com.company.personnel.dto;

import java.util.ArrayList;
import java.util.List;

public class ImportReceipt {
    private final boolean success;
    private final String message;
    private final int importedRows;
    private final List<ImportError> errors = new ArrayList<>();

    public ImportReceipt(boolean success, String message, int importedRows) {
        this.success = success;
        this.message = message;
        this.importedRows = importedRows;
    }

    public boolean isSuccess() { return success; }
    public String getMessage() { return message; }
    public int getImportedRows() { return importedRows; }
    public List<ImportError> getErrors() { return errors; }
}
```

```java
package com.company.personnel.dto;

public class ImportError {
    private final int row;
    private final String field;
    private final String reason;

    public ImportError(int row, String field, String reason) {
        this.row = row;
        this.field = field;
        this.reason = reason;
    }

    public int getRow() { return row; }
    public String getField() { return field; }
    public String getReason() { return reason; }
}
```

`EmployeeImportRow`（工号、姓名、性别、出生日期、手机号、邮箱、部门、岗位、职级、直属上级、入职日期、状态、用工类型、成本中心，均为 `String`）、`BudgetImportRow`（工号、科目编码、期间、预算金额）、`ProjectImportRow`（科目编码、科目名称、分类、状态）均为 `@Data` 风格字段，用于从行解析。

- [ ] **Step 2: 编写 ImportParser 接口与两个实现**

```java
package com.company.personnel.importers;

import org.springframework.web.multipart.MultipartFile;
import java.util.List;

public interface ImportParser {
    List<String[]> parse(MultipartFile file) throws Exception;
}
```

`ExcelImportParser`：用 `WorkbookFactory.create(file.getInputStream())` 打开首个 sheet，跳过第 1 行表头，把每行单元格用 `DataFormatter` 转成 `String[]`。`CsvImportParser`：用 `CSVParser.parse(file.getInputStream(), StandardCharsets.UTF_8, CSVFormat.DEFAULT.withFirstRecordAsHeader())`，把每条记录转成 `String[]`（保持表头列顺序）。`ImportService` 依据文件名扩展名选择实现。

- [ ] **Step 3: 编写三个 Validator（以员工为例，返回 `List<ImportError>`）**

员工行校验规则（`EmployeeImportValidator.validate(rows, employeeMapper)`）：
- 必填：`emp_no`、`name`；缺一报 `row + "必填项缺失"`。
- 手机号 `^\d{11}$`（可空则跳过）、邮箱（可空则跳过、非空须含 `@`）、日期用 `LocalDate.parse`（失败报「日期格式错误」）。
- 工号唯一：与本次批次内去重 + `employeeMapper.countByEmpNo` 冲突报错。
- 状态取值限定 `在职/离职`；性别 `男/女/其他`。

预算行校验（`BudgetImportValidator`）：必填工号、科目编码、期间；工号必须存在于员工表；科目编码必须命中且 `status=启用`；期间 `PeriodUtil.isValid`；金额 `MoneyUtil.ensureNonNegative`；三元组 `(employee_id, project_id, period)` 与库内及批次内都不冲突。

白名单行校验（`ProjectImportValidator`）：必填 `project_code`、`project_name`；编码唯一（库内+批次内）。

- [ ] **Step 4: 编写 ImportService（全对才导入，整批事务）**

```java
package com.company.personnel.service.impl;

import com.company.personnel.common.BusinessException;
import com.company.personnel.dto.*;
import com.company.personnel.importers.*;
import com.company.personnel.mapper.*;
import com.company.personnel.service.ImportService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@Service
public class ImportServiceImpl implements ImportService {
    private final ImportParser excelParser;
    private final ImportParser csvParser;
    private final EmployeeMapper employeeMapper;
    private final EmployeeBudgetMapper budgetMapper;
    private final BudgetProjectMapper projectMapper;
    private final EmployeeImportValidator employeeValidator;
    private final BudgetImportValidator budgetValidator;
    private final ProjectImportValidator projectValidator;

    // 构造器注入以上依赖

    private ImportParser pickParser(MultipartFile file) {
        String name = file.getOriginalFilename() == null ? "" : file.getOriginalFilename().toLowerCase();
        if (name.endsWith(".xlsx")) return excelParser;
        if (name.endsWith(".csv")) return csvParser;
        throw new BusinessException("仅支持 .xlsx 或 .csv 文件");
    }

    @Override
    @Transactional
    public ImportReceipt importEmployees(MultipartFile file) {
        try {
            List<String[]> rows = pickParser(file).parse(file);
            if (rows.isEmpty()) return new ImportReceipt(false, "文件为空", 0);
            List<EmployeeImportRow> parsed = EmployeeImportRow.fromRows(rows);
            List<ImportError> errors = employeeValidator.validate(parsed);
            if (!errors.isEmpty()) {
                ImportReceipt r = new ImportReceipt(false, "导入失败：存在 N 行错误（N=错误行数）", 0);
                r.getErrors().addAll(errors);
                return r;
            }
            for (EmployeeImportRow row : parsed) {
                employeeMapper.insert(row.toEmployee());
            }
            return new ImportReceipt(true, "导入成功", parsed.size());
        } catch (Exception e) {
            throw new BusinessException("导入解析失败：" + e.getMessage(), e);
        }
    }
    // importBudgets / importProjects 同构
}
```

- [ ] **Step 5: 编写 ImportController 与 import.html**（`GET /import` 渲染入口（三块上传表单 + 各自模板下载链接）；`POST /import/employees`、`/import/budgets`、`/import/projects` 均为 `@PreAuthorize("hasRole('ADMIN')")`，接收 `MultipartFile file`，返回回执渲染到同一页面。`GET /import/template/{type}` 下载模板，按 type 返回对应 CSV 资源）。模板 CSV 首行表头与 7.1 设计一致（员工模板：`工号,姓名,性别,出生日期,手机号,邮箱,部门,岗位,职级,直属上级,入职日期,状态,用工类型,成本中心`）。

- [ ] **Step 6: 编写导入单元测试（正确行/错误行/边界金额）**

示例（`ImportServiceImplTest`，H2 已启用）：

```java
@Test
void 员工导入_含重复工号_整批回滚() {
    MockMultipartFile file = new MockMultipartFile("file", "e.csv", "text/csv",
        "工号,姓名,性别,出生日期,手机号,邮箱,部门,岗位,职级,直属上级,入职日期,状态,用工类型,成本中心\n" +
        "E0001,张三,男,1990-05-12,13800000001,a@b.com,研发部,工程师,P6,李四,2020-01-06,在职,正式,RD-01\n".getBytes(StandardCharsets.UTF_8));
    ImportReceipt r = importService.importEmployees(file);
    assertFalse(r.isSuccess()); // E0001 与种子冲突
    assertEquals(0, r.getImportedRows());
}
```

- [ ] **Step 7: Run tests**

Run: `mvn -q -f personnel-dashboard/pom.xml test`
Expected: PASS（含回滚用例）。

- [ ] **Step 8: Commit**

```bash
git add personnel-dashboard/src/main/java/com/company/personnel/dto/ImportReceipt.java \
  personnel-dashboard/src/main/java/com/company/personnel/dto/ImportError.java \
  personnel-dashboard/src/main/java/com/company/personnel/dto/EmployeeImportRow.java \
  personnel-dashboard/src/main/java/com/company/personnel/dto/BudgetImportRow.java \
  personnel-dashboard/src/main/java/com/company/personnel/dto/ProjectImportRow.java \
  personnel-dashboard/src/main/java/com/company/personnel/importers \
  personnel-dashboard/src/main/java/com/company/personnel/service/ImportService.java \
  personnel-dashboard/src/main/java/com/company/personnel/service/impl/ImportServiceImpl.java \
  personnel-dashboard/src/main/java/com/company/personnel/controller/ImportController.java \
  personnel-dashboard/src/main/resources/templates/import.html \
  personnel-dashboard/src/main/resources/templates/download \
  personnel-dashboard/src/test/java/com/company/personnel/ImportServiceImplTest.java
git commit -m "feat(personnel): all-or-nothing batch import for employees/budgets/whitelist"
```

---

### Task 10: 统一异常处理、集成测试与冒烟

**Files:**
- Create: `personnel-dashboard/src/main/java/com/company/personnel/common/GlobalExceptionHandler.java`
- Create: `personnel-dashboard/src/main/resources/templates/error/404.html`
- Create: `personnel-dashboard/src/main/resources/templates/error/500.html`
- Create: `personnel-dashboard/src/test/java/com/company/personnel/EmployeeCrudIT.java`
- Create: `personnel-dashboard/src/test/java/com/company/personnel/BudgetWhitelistIT.java`
- Create: `personnel-dashboard/src/test/resources/application-test.yml`
- Create: `personnel-dashboard/src/test/java/com/company/personnel/ImportRollbackIT.java`

**Interfaces:**
- Consumes: Task 1-9 完整链路。
- Produces: 全量集成测试与冒烟路径，验收依据。

- [ ] **Step 1: 编写 GlobalExceptionHandler**

```java
package com.company.personnel.common;

import org.springframework.http.HttpStatus;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.servlet.ModelAndView;

@ControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(BusinessException.class)
    public ModelAndView handleBusiness(BusinessException e) {
        ModelAndView mv = new ModelAndView("error/500");
        mv.setStatus(HttpStatus.BAD_REQUEST);
        mv.addObject("message", e.getMessage());
        return mv;
    }
}
```

- [ ] **Step 2: 编写 application-test.yml（H2 兼容 MySQL 模式）**

```yaml
spring:
  datasource:
    url: jdbc:h2:mem:personnel;MODE=MySQL;DB_CLOSE_DELAY=-1;DATABASE_TO_LOWER=TRUE
    driver-class-name: org.h2.Driver
    username: sa
    password:
  sql:
    init:
      mode: always
      schema-locations: classpath:db/schema.sql
      data-locations: classpath:db/data.sql
  thymeleaf:
    cache: false
mybatis:
  mapper-locations: classpath:mapper/*.xml
  configuration:
    map-underscore-to-camel-case: true
pagehelper:
  helper-dialect: mysql
  reasonable: true
  support-methods-arguments: true
```

- [ ] **Step 3: 编写集成测试**（`EmployeeCrudIT`：管理员新增→查询→更新→删除员工全链路，`@SpringBootTest` + `@AutoConfigureMockMvc` + `@ActiveProfiles("test")`，用 `permitAll` 的测试安全或 `spring-security-test` 模拟 admin。`BudgetWhitelistIT`：停用科目录入预算被拒；`ImportRollbackIT`：含 1 行非法数据的整批导入后 `employee` 表条数不变。）

- [ ] **Step 4: Run tests**

Run: `mvn -q -f personnel-dashboard/pom.xml test`
Expected: 全部 PASS。

- [ ] **Step 5: 全链路冒烟**（需本地 MySQL `personnel` 库及种子数据）

Run: `mvn -q -f personnel-dashboard/pom.xml spring-boot:run`
手工走查：`/login`（admin/初始化密码）→ 员工 CRUD → `/import` 下载模板并上传 → `/dashboard` 看板展示（预算−实际−预留）。Expected: 各页正常渲染，无 5xx 异常。

- [ ] **Step 6: Commit**

```bash
git add personnel-dashboard/src/main/java/com/company/personnel/common/GlobalExceptionHandler.java \
  personnel-dashboard/src/main/resources/templates/error \
  personnel-dashboard/src/test
git commit -m "test(personnel): integration tests and global error handling"
```

---

## 自审记录（Self-Review）

- **覆盖检查**：设计文档 6.1–6.7、7.1–7.3、8、9、10 均有对应 Task（登录→Task 4；员工 CRUD→Task 5；白名单→Task 6；预算/实际/预留→Task 7；看板→Task 8；导入→Task 9；异常/测试→Task 10；建表与种子→Task 1）。
- **占位符扫描**：仅 `data.sql` 中管理员 BCrypt 哈希为必填占位符，且 Task 4 Step 5 明确给定生成与替换流程；其余无 TBD/TODO。
- **类型一致性**：实体字段驼峰名（`empNo`、`budgetAmount`、`projectCode`）、Mapper 方法签名、Service 接口与 Controller 引用名保持一致；`BudgetBoardRow.remaining` 计算口径为 `预算 − 实际 − 预留`，与设计文档 6.3 一致。
- **决策状态**：文档头部「已确认决策」四项（工程目录、包名、Lombok、管理员初始密码）均已在本轮与用户确认，执行阶段可直接采用，无需再次提问。

---

Plan complete and saved to `docs/superpowers/plans/2026-10-29-personnel-dashboard.md`。
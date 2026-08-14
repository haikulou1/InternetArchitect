# 三接口 + 埋点报表 + 导出 全栈功能 — 实施计划

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 在 leecode 仓库新建 Spring Boot 后端模块提供 5 个 REST 接口（HelloWorld、哈希、排序、埋点、导出），在 haikulou1.github.io 仓库新增 Vue SPA 前端页面（三 Tab + ECharts 图表 + 导出按钮），实现全链路打通。

**Architecture:** 后端模块 `api-demo` 部署于 leecode 仓库，Spring Boot 2.2.2 + 内存 H2 存储埋点数据；前端 SPA 部署于 haikulou1.github.io 的 `/api-demo/` 路径，通过 Axios 调用后端 REST API，使用 ECharts 渲染折线图/饼图/柱状图。

**Tech Stack:**
- 后端：Spring Boot 2.2.2.RELEASE, Java 8, Maven, H2 (内存数据库), Apache POI (xlsx)
- 前端：Vue 3 (CDN), ECharts 5 (CDN), Axios (CDN) — 纯静态 SPA，零构建工具
- 跨域：后端 CORS 全局放行

---

## Global Constraints

- Spring Boot 版本固定 2.2.2.RELEASE，groupId `com.mashibing`
- Java 版本 1.8
- 冒泡排序使用固定数组 `{3, 9, -1, 10, -2}`（复用 leecode 现有算法）
- 哈希算法：MD5 和 SHA-256，输入为字符串，返回十六进制摘要
- 埋点维度：人员类型、人员层级、人员部门 — 由前端请求参数传入
- 导出格式：CSV 和 Excel (.xlsx) 双格式
- 前端不引入构建工具（webpack/vite），使用 CDN 引入 Vue 3 + ECharts + Axios
- 所有接口统一前缀 `/api/`

---

## File Structure

```
leecode-master/
└── api-demo/                          # 新建 Spring Boot 模块
    ├── pom.xml
    └── src/main/
        ├── java/com/mashibing/apidemo/
        │   ├── ApiDemoApplication.java          # 启动类
        │   ├── config/
        │   │   └── CorsConfig.java              # CORS 全局配置
        │   ├── controller/
        │   │   ├── HelloWorldController.java    # GET /api/helloworld
        │   │   ├── HashController.java          # POST /api/hash
        │   │   ├── SortController.java          # GET /api/bubblesort
        │   │   ├── TrackingController.java      # POST /api/tracking, GET /api/stats
        │   │   └── ExportController.java        # GET /api/export/{type}
        │   ├── service/
        │   │   ├── HashService.java             # MD5/SHA256 计算
        │   │   ├── SortService.java             # 冒泡排序（复用算法）
        │   │   ├── TrackingService.java         # 埋点存储 + 聚合查询
        │   │   └── ExportService.java           # CSV/Excel 生成
        │   ├── entity/
        │   │   └── TrackingRecord.java          # 埋点记录实体
        │   └── aop/
        │       └── TrackingAspect.java          # AOP 切面自动埋点
        └── resources/
            ├── application.properties            # H2 + 端口配置
            └── schema.sql                        # H2 建表 DDL

haikulou1.github.io-master/
└── api-demo/
    ├── index.html                  # 主页面：三 Tab + 图表报表 + 导出按钮
    ├── app.js                      # Vue 3 应用逻辑
    └── style.css                   # 页面样式
```

---

## Task 1: 创建 Spring Boot 后端模块骨架

**Files:**
- Create: `leecode-master/api-demo/pom.xml`
- Create: `leecode-master/api-demo/src/main/java/com/mashibing/apidemo/ApiDemoApplication.java`
- Create: `leecode-master/api-demo/src/main/resources/application.properties`
- Create: `leecode-master/api-demo/src/main/resources/schema.sql`
- Create: `leecode-master/api-demo/src/main/java/com/mashibing/apidemo/config/CorsConfig.java`

**Interfaces:**
- Produces: `ApiDemoApplication` — Spring Boot 入口，端口 8088
- Produces: `CorsConfig` — 全局 CORS 放行，允许所有来源

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
        <version>2.2.2.RELEASE</version>
        <relativePath/>
    </parent>
    <groupId>com.mashibing</groupId>
    <artifactId>api-demo</artifactId>
    <version>0.0.1-SNAPSHOT</version>
    <name>api-demo</name>
    <description>三接口 + 埋点 + 导出 API 模块</description>

    <properties>
        <java.version>1.8</java.version>
    </properties>

    <dependencies>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-web</artifactId>
        </dependency>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-aop</artifactId>
        </dependency>
        <dependency>
            <groupId>com.h2database</groupId>
            <artifactId>h2</artifactId>
            <scope>runtime</scope>
        </dependency>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-jdbc</artifactId>
        </dependency>
        <dependency>
            <groupId>org.apache.poi</groupId>
            <artifactId>poi-ooxml</artifactId>
            <version>4.1.2</version>
        </dependency>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-test</artifactId>
            <scope>test</scope>
            <exclusions>
                <exclusion>
                    <groupId>org.junit.vintage</groupId>
                    <artifactId>junit-vintage-engine</artifactId>
                </exclusion>
            </exclusions>
        </dependency>
    </dependencies>

    <build>
        <plugins>
            <plugin>
                <groupId>org.springframework.boot</groupId>
                <artifactId>spring-boot-maven-plugin</artifactId>
            </plugin>
        </plugins>
    </build>
</project>
```

- [ ] **Step 2: 创建 ApiDemoApplication.java**

```java
package com.mashibing.apidemo;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class ApiDemoApplication {
    public static void main(String[] args) {
        SpringApplication.run(ApiDemoApplication.class, args);
    }
}
```

- [ ] **Step 3: 创建 application.properties**

```properties
server.port=8088
spring.datasource.url=jdbc:h2:mem:trackingdb;DB_CLOSE_DELAY=-1
spring.datasource.driver-class-name=org.h2.Driver
spring.datasource.username=sa
spring.datasource.password=
spring.h2.console.enabled=true
spring.h2.console.path=/h2-console
```

- [ ] **Step 4: 创建 schema.sql（H2 建表 DDL）**

```sql
CREATE TABLE IF NOT EXISTS tracking_record (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    api_name VARCHAR(100) NOT NULL,
    caller VARCHAR(100) NOT NULL DEFAULT 'anonymous',
    personnel_type VARCHAR(50),
    personnel_level VARCHAR(50),
    personnel_department VARCHAR(100),
    call_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
```

- [ ] **Step 5: 创建 CorsConfig.java**

```java
package com.mashibing.apidemo.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.CorsFilter;

@Configuration
public class CorsConfig {

    @Bean
    public CorsFilter corsFilter() {
        CorsConfiguration config = new CorsConfiguration();
        config.addAllowedOrigin("*");
        config.addAllowedHeader("*");
        config.addAllowedMethod("*");
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return new CorsFilter(source);
    }
}
```

- [ ] **Step 6: 编译验证**

在 `leecode-master/api-demo/` 下执行：
```bash
mvn compile -q
```
预期：BUILD SUCCESS

---

## Task 2: 实现 HelloWorld 接口

**Files:**
- Create: `leecode-master/api-demo/src/main/java/com/mashibing/apidemo/controller/HelloWorldController.java`

**Interfaces:**
- Produces: `GET /api/helloworld` → `{"message": "Hello World", "timestamp": "..."}`

- [ ] **Step 1: 创建 HelloWorldController.java**

```java
package com.mashibing.apidemo.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api")
public class HelloWorldController {

    @GetMapping("/helloworld")
    public Map<String, Object> helloWorld() {
        Map<String, Object> result = new HashMap<>();
        result.put("message", "Hello World");
        result.put("timestamp", LocalDateTime.now().toString());
        return result;
    }
}
```

- [ ] **Step 2: 编译验证**

```bash
cd leecode-master/api-demo && mvn compile -q
```
预期：BUILD SUCCESS

---

## Task 3: 实现哈希接口

**Files:**
- Create: `leecode-master/api-demo/src/main/java/com/mashibing/apidemo/service/HashService.java`
- Create: `leecode-master/api-demo/src/main/java/com/mashibing/apidemo/controller/HashController.java`

**Interfaces:**
- Consumes: 无
- Produces: `POST /api/hash` — 请求体 `{"input": "string"}` → `{"md5": "...", "sha256": "..."}`

- [ ] **Step 1: 创建 HashService.java**

```java
package com.mashibing.apidemo.service;

import org.springframework.stereotype.Service;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

@Service
public class HashService {

    public String md5(String input) {
        return digest(input, "MD5");
    }

    public String sha256(String input) {
        return digest(input, "SHA-256");
    }

    private String digest(String input, String algorithm) {
        try {
            MessageDigest md = MessageDigest.getInstance(algorithm);
            byte[] hashBytes = md.digest(input.getBytes());
            StringBuilder sb = new StringBuilder();
            for (byte b : hashBytes) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("Algorithm not found: " + algorithm, e);
        }
    }
}
```

- [ ] **Step 2: 创建 HashController.java**

```java
package com.mashibing.apidemo.controller;

import com.mashibing.apidemo.service.HashService;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api")
public class HashController {

    private final HashService hashService;

    public HashController(HashService hashService) {
        this.hashService = hashService;
    }

    @PostMapping("/hash")
    public Map<String, Object> hash(@RequestBody Map<String, String> body) {
        String input = body.getOrDefault("input", "");
        Map<String, Object> result = new HashMap<>();
        result.put("input", input);
        result.put("md5", hashService.md5(input));
        result.put("sha256", hashService.sha256(input));
        return result;
    }
}
```

- [ ] **Step 3: 编译验证**

```bash
cd leecode-master/api-demo && mvn compile -q
```
预期：BUILD SUCCESS

---

## Task 4: 实现冒泡排序接口

**Files:**
- Create: `leecode-master/api-demo/src/main/java/com/mashibing/apidemo/service/SortService.java`
- Create: `leecode-master/api-demo/src/main/java/com/mashibing/apidemo/controller/SortController.java`

**Interfaces:**
- Consumes: 复用 `leecode-master/datastruct/src/排序/冒泡排序.java` 的 sort 算法
- Produces: `GET /api/bubblesort` → `{"original": [3,9,-1,10,-2], "sorted": [-2,-1,3,9,10], "steps": [...]}`

- [ ] **Step 1: 创建 SortService.java（含完整排序步骤记录）**

```java
package com.mashibing.apidemo.service;

import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@Service
public class SortService {

    public SortResult bubbleSort() {
        int[] original = {3, 9, -1, 10, -2};
        int[] arr = Arrays.copyOf(original, original.length);
        List<String> steps = new ArrayList<>();

        steps.add("初始: " + Arrays.toString(arr));

        for (int i = 0; i < arr.length; i++) {
            for (int j = 0; j < arr.length - i - 1; j++) {
                if (arr[j] > arr[j + 1]) {
                    int temp = arr[j];
                    arr[j] = arr[j + 1];
                    arr[j + 1] = temp;
                    steps.add("第" + (i + 1) + "轮第" + (j + 1) + "次交换: " + Arrays.toString(arr));
                }
            }
        }

        return new SortResult(original, arr, steps);
    }

    public static class SortResult {
        private final int[] original;
        private final int[] sorted;
        private final List<String> steps;

        public SortResult(int[] original, int[] sorted, List<String> steps) {
            this.original = original;
            this.sorted = sorted;
            this.steps = steps;
        }

        public int[] getOriginal() { return original; }
        public int[] getSorted() { return sorted; }
        public List<String> getSteps() { return steps; }
    }
}
```

- [ ] **Step 2: 创建 SortController.java**

```java
package com.mashibing.apidemo.controller;

import com.mashibing.apidemo.service.SortService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api")
public class SortController {

    private final SortService sortService;

    public SortController(SortService sortService) {
        this.sortService = sortService;
    }

    @GetMapping("/bubblesort")
    public Map<String, Object> bubbleSort() {
        SortService.SortResult result = sortService.bubbleSort();
        Map<String, Object> response = new HashMap<>();
        response.put("original", result.getOriginal());
        response.put("sorted", result.getSorted());
        response.put("steps", result.getSteps());
        return response;
    }
}
```

- [ ] **Step 3: 编译验证**

```bash
cd leecode-master/api-demo && mvn compile -q
```
预期：BUILD SUCCESS

---

## Task 5: 实现埋点存储与 AOP 切面

**Files:**
- Create: `leecode-master/api-demo/src/main/java/com/mashibing/apidemo/entity/TrackingRecord.java`
- Create: `leecode-master/api-demo/src/main/java/com/mashibing/apidemo/aop/TrackingAspect.java`
- Create: `leecode-master/api-demo/src/main/java/com/mashibing/apidemo/service/TrackingService.java`
- Create: `leecode-master/api-demo/src/main/java/com/mashibing/apidemo/controller/TrackingController.java`

**Interfaces:**
- Produces: `POST /api/tracking` — 手动埋点（请求体含维度参数）
- Produces: `GET /api/stats` — 聚合统计查询（支持 `groupBy` 参数：personnel_type / personnel_level / personnel_department）
- Produces: `GET /api/stats/timeline` — 按时间维度的调用次数时序数据
- AOP: 所有 `/api/*` 接口自动记录调用（从请求头 `X-Caller` / `X-Personnel-Type` / `X-Personnel-Level` / `X-Personnel-Department` 提取维度信息，无则默认 anonymous）

- [ ] **Step 1: 创建 TrackingRecord 实体**

```java
package com.mashibing.apidemo.entity;

import java.time.LocalDateTime;

public class TrackingRecord {
    private Long id;
    private String apiName;
    private String caller;
    private String personnelType;
    private String personnelLevel;
    private String personnelDepartment;
    private LocalDateTime callTime;

    public TrackingRecord() {}

    public TrackingRecord(String apiName, String caller, String personnelType,
                          String personnelLevel, String personnelDepartment) {
        this.apiName = apiName;
        this.caller = caller;
        this.personnelType = personnelType;
        this.personnelLevel = personnelLevel;
        this.personnelDepartment = personnelDepartment;
        this.callTime = LocalDateTime.now();
    }

    // Getters and setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getApiName() { return apiName; }
    public void setApiName(String apiName) { this.apiName = apiName; }
    public String getCaller() { return caller; }
    public void setCaller(String caller) { this.caller = caller; }
    public String getPersonnelType() { return personnelType; }
    public void setPersonnelType(String personnelType) { this.personnelType = personnelType; }
    public String getPersonnelLevel() { return personnelLevel; }
    public void setPersonnelLevel(String personnelLevel) { this.personnelLevel = personnelLevel; }
    public String getPersonnelDepartment() { return personnelDepartment; }
    public void setPersonnelDepartment(String personnelDepartment) { this.personnelDepartment = personnelDepartment; }
    public LocalDateTime getCallTime() { return callTime; }
    public void setCallTime(LocalDateTime callTime) { this.callTime = callTime; }
}
```

- [ ] **Step 2: 创建 TrackingService.java**

```java
package com.mashibing.apidemo.service;

import com.mashibing.apidemo.entity.TrackingRecord;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class TrackingService {

    private final JdbcTemplate jdbcTemplate;

    public TrackingService(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public void save(TrackingRecord record) {
        jdbcTemplate.update(
            "INSERT INTO tracking_record (api_name, caller, personnel_type, personnel_level, personnel_department, call_time) VALUES (?, ?, ?, ?, ?, ?)",
            record.getApiName(), record.getCaller(), record.getPersonnelType(),
            record.getPersonnelLevel(), record.getPersonnelDepartment(),
            record.getCallTime() != null ? record.getCallTime() : LocalDateTime.now()
        );
    }

    public List<Map<String, Object>> aggregate(String groupBy) {
        String column = sanitizeColumn(groupBy);
        String sql = "SELECT " + column + " as dimension, COUNT(*) as count FROM tracking_record GROUP BY " + column + " ORDER BY count DESC";
        return jdbcTemplate.queryForList(sql);
    }

    public List<Map<String, Object>> timeline() {
        String sql = "SELECT DATE_FORMAT(call_time, '%Y-%m-%d %H:00:00') as time, COUNT(*) as count " +
                     "FROM tracking_record GROUP BY DATE_FORMAT(call_time, '%Y-%m-%d %H:00:00') " +
                     "ORDER BY time";
        return jdbcTemplate.queryForList(sql);
    }

    public List<Map<String, Object>> totalByApi() {
        String sql = "SELECT api_name, COUNT(*) as count FROM tracking_record GROUP BY api_name";
        return jdbcTemplate.queryForList(sql);
    }

    private String sanitizeColumn(String groupBy) {
        switch (groupBy) {
            case "personnel_type": return "personnel_type";
            case "personnel_level": return "personnel_level";
            case "personnel_department": return "personnel_department";
            default: return "personnel_type";
        }
    }
}
```

- [ ] **Step 3: 创建 TrackingAspect.java（AOP 自动埋点）**

```java
package com.mashibing.apidemo.aop;

import com.mashibing.apidemo.entity.TrackingRecord;
import com.mashibing.apidemo.service.TrackingService;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.AfterReturning;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import javax.servlet.http.HttpServletRequest;

@Aspect
@Component
public class TrackingAspect {

    private final TrackingService trackingService;

    public TrackingAspect(TrackingService trackingService) {
        this.trackingService = trackingService;
    }

    @AfterReturning(pointcut = "execution(* com.mashibing.apidemo.controller..*(..))")
    public void trackApiCall(JoinPoint joinPoint) {
        ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attributes == null) return;

        HttpServletRequest request = attributes.getRequest();
        String apiName = request.getRequestURI();
        String caller = getHeaderOrDefault(request, "X-Caller", "anonymous");
        String personnelType = getHeaderOrDefault(request, "X-Personnel-Type", "unknown");
        String personnelLevel = getHeaderOrDefault(request, "X-Personnel-Level", "unknown");
        String personnelDepartment = getHeaderOrDefault(request, "X-Personnel-Department", "unknown");

        TrackingRecord record = new TrackingRecord(apiName, caller, personnelType, personnelLevel, personnelDepartment);
        trackingService.save(record);
    }

    private String getHeaderOrDefault(HttpServletRequest request, String header, String defaultValue) {
        String value = request.getHeader(header);
        return (value != null && !value.isEmpty()) ? value : defaultValue;
    }
}
```

- [ ] **Step 4: 创建 TrackingController.java**

```java
package com.mashibing.apidemo.controller;

import com.mashibing.apidemo.entity.TrackingRecord;
import com.mashibing.apidemo.service.TrackingService;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api")
public class TrackingController {

    private final TrackingService trackingService;

    public TrackingController(TrackingService trackingService) {
        this.trackingService = trackingService;
    }

    @PostMapping("/tracking")
    public Map<String, Object> track(@RequestBody Map<String, String> body) {
        TrackingRecord record = new TrackingRecord(
            body.getOrDefault("apiName", "/api/tracking"),
            body.getOrDefault("caller", "anonymous"),
            body.getOrDefault("personnelType", "unknown"),
            body.getOrDefault("personnelLevel", "unknown"),
            body.getOrDefault("personnelDepartment", "unknown")
        );
        trackingService.save(record);
        Map<String, Object> result = new HashMap<>();
        result.put("status", "ok");
        return result;
    }

    @GetMapping("/stats")
    public Map<String, Object> stats(@RequestParam(defaultValue = "personnel_type") String groupBy) {
        Map<String, Object> result = new HashMap<>();
        result.put("aggregation", trackingService.aggregate(groupBy));
        result.put("totalByApi", trackingService.totalByApi());
        return result;
    }

    @GetMapping("/stats/timeline")
    public Map<String, Object> timeline() {
        Map<String, Object> result = new HashMap<>();
        result.put("timeline", trackingService.timeline());
        return result;
    }
}
```

- [ ] **Step 5: 编译验证**

```bash
cd leecode-master/api-demo && mvn compile -q
```
预期：BUILD SUCCESS

---

## Task 6: 实现导出接口

**Files:**
- Create: `leecode-master/api-demo/src/main/java/com/mashibing/apidemo/service/ExportService.java`
- Create: `leecode-master/api-demo/src/main/java/com/mashibing/apidemo/controller/ExportController.java`

**Interfaces:**
- Consumes: `TrackingService` 聚合数据
- Produces: `GET /api/export/csv?groupBy=personnel_type` → 返回 CSV 文件下载
- Produces: `GET /api/export/xlsx?groupBy=personnel_type` → 返回 Excel 文件下载

- [ ] **Step 1: 创建 ExportService.java**

```java
package com.mashibing.apidemo.service;

import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;
import java.util.Map;

@Service
public class ExportService {

    private final TrackingService trackingService;

    public ExportService(TrackingService trackingService) {
        this.trackingService = trackingService;
    }

    public byte[] exportCsv(String groupBy) {
        List<Map<String, Object>> data = trackingService.aggregate(groupBy);
        StringBuilder sb = new StringBuilder();
        sb.append("维度,调用次数\n");
        for (Map<String, Object> row : data) {
            sb.append(row.get("dimension")).append(",").append(row.get("count")).append("\n");
        }
        return sb.toString().getBytes(java.nio.charset.StandardCharsets.UTF_8);
    }

    public byte[] exportXlsx(String groupBy) throws IOException {
        List<Map<String, Object>> data = trackingService.aggregate(groupBy);
        Workbook workbook = new XSSFWorkbook();
        Sheet sheet = workbook.createSheet("埋点统计");
        Row header = sheet.createRow(0);
        header.createCell(0).setCellValue("维度");
        header.createCell(1).setCellValue("调用次数");

        int rowIdx = 1;
        for (Map<String, Object> row : data) {
            Row r = sheet.createRow(rowIdx++);
            r.createCell(0).setCellValue(String.valueOf(row.get("dimension")));
            r.createCell(1).setCellValue(((Number) row.get("count")).intValue());
        }

        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        workbook.write(bos);
        workbook.close();
        return bos.toByteArray();
    }
}
```

- [ ] **Step 2: 创建 ExportController.java**

```java
package com.mashibing.apidemo.controller;

import com.mashibing.apidemo.service.ExportService;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;

@RestController
@RequestMapping("/api")
public class ExportController {

    private final ExportService exportService;

    public ExportController(ExportService exportService) {
        this.exportService = exportService;
    }

    @GetMapping("/export/csv")
    public ResponseEntity<byte[]> exportCsv(@RequestParam(defaultValue = "personnel_type") String groupBy) {
        byte[] content = exportService.exportCsv(groupBy);
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.parseMediaType("text/csv; charset=UTF-8"));
        headers.setContentDispositionFormData("attachment", "tracking_report.csv");
        return ResponseEntity.ok().headers(headers).body(content);
    }

    @GetMapping("/export/xlsx")
    public ResponseEntity<byte[]> exportXlsx(@RequestParam(defaultValue = "personnel_type") String groupBy) throws IOException {
        byte[] content = exportService.exportXlsx(groupBy);
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"));
        headers.setContentDispositionFormData("attachment", "tracking_report.xlsx");
        return ResponseEntity.ok().headers(headers).body(content);
    }
}
```

- [ ] **Step 3: 编译验证**

```bash
cd leecode-master/api-demo && mvn compile -q
```
预期：BUILD SUCCESS

---

## Task 7: 创建前端 SPA 页面

**Files:**
- Create: `haikulou1.github.io-master/api-demo/index.html`
- Create: `haikulou1.github.io-master/api-demo/app.js`
- Create: `haikulou1.github.io-master/api-demo/style.css`

**Interfaces:**
- Consumes: 后端所有 `/api/*` 接口，默认连接 `http://localhost:8088`
- Produces: 单页面应用，包含三 Tab + 图表报表 + 导出按钮

- [ ] **Step 1: 创建 index.html**

```html
<!DOCTYPE html>
<html lang="zh-CN">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>API Demo - 三接口 + 埋点报表</title>
    <script src="https://unpkg.com/vue@3/dist/vue.global.prod.js"></script>
    <script src="https://unpkg.com/axios/dist/axios.min.js"></script>
    <script src="https://unpkg.com/echarts@5/dist/echarts.min.js"></script>
    <link rel="stylesheet" href="style.css">
</head>
<body>
    <div id="app">
        <!-- 页面标题 -->
        <div class="header">
            <h1>API 演示平台</h1>
            <div class="api-base-url">
                <label>后端地址：</label>
                <input type="text" v-model="baseUrl" @change="onBaseUrlChange" placeholder="http://localhost:8088">
            </div>
        </div>

        <!-- 三个 Tab 页签 -->
        <div class="tabs">
            <button v-for="tab in tabs" :key="tab.id"
                    :class="['tab-btn', { active: activeTab === tab.id }]"
                    @click="switchTab(tab.id)">
                {{ tab.label }}
            </button>
        </div>

        <!-- Tab 内容区 -->
        <div class="tab-content">
            <!-- Tab 1: HelloWorld -->
            <div v-show="activeTab === 'helloworld'" class="tab-panel">
                <h2>HelloWorld 接口</h2>
                <button class="btn-primary" @click="callHelloWorld">调用接口</button>
                <div v-if="helloworldResult" class="result-box">
                    <pre>{{ JSON.stringify(helloworldResult, null, 2) }}</pre>
                </div>
                <div v-if="helloworldError" class="error-box">{{ helloworldError }}</div>
            </div>

            <!-- Tab 2: 哈希算法 -->
            <div v-show="activeTab === 'hash'" class="tab-panel">
                <h2>哈希算法 (MD5 / SHA-256)</h2>
                <div class="input-group">
                    <input type="text" v-model="hashInput" placeholder="输入要哈希的字符串" @keyup.enter="callHash">
                    <button class="btn-primary" @click="callHash">计算哈希</button>
                </div>
                <div v-if="hashResult" class="result-box">
                    <pre>{{ JSON.stringify(hashResult, null, 2) }}</pre>
                </div>
                <div v-if="hashError" class="error-box">{{ hashError }}</div>
            </div>

            <!-- Tab 3: 冒泡排序 -->
            <div v-show="activeTab === 'bubblesort'" class="tab-panel">
                <h2>冒泡排序</h2>
                <button class="btn-primary" @click="callBubbleSort">执行排序</button>
                <div v-if="sortResult" class="result-box">
                    <h3>原始数组</h3>
                    <pre>{{ JSON.stringify(sortResult.original) }}</pre>
                    <h3>排序后数组</h3>
                    <pre>{{ JSON.stringify(sortResult.sorted) }}</pre>
                    <h3>排序步骤</h3>
                    <div class="steps-list">
                        <div v-for="(step, idx) in sortResult.steps" :key="idx" class="step-item">{{ step }}</div>
                    </div>
                </div>
                <div v-if="sortError" class="error-box">{{ sortError }}</div>
            </div>
        </div>

        <hr class="divider">

        <!-- 导出按钮 -->
        <div class="export-section">
            <h2>导出埋点报表</h2>
            <div class="export-controls">
                <select v-model="exportGroupBy">
                    <option value="personnel_type">人员类型</option>
                    <option value="personnel_level">人员层级</option>
                    <option value="personnel_department">人员部门</option>
                </select>
                <button class="btn-primary" @click="exportCsv">导出 CSV</button>
                <button class="btn-primary" @click="exportXlsx">导出 Excel</button>
            </div>
        </div>

        <hr class="divider">

        <!-- 埋点报表可视化 -->
        <div class="chart-section">
            <h2>埋点调用报表</h2>
            <div class="chart-controls">
                <select v-model="chartGroupBy" @change="loadStats">
                    <option value="personnel_type">按人员类型</option>
                    <option value="personnel_level">按人员层级</option>
                    <option value="personnel_department">按人员部门</option>
                </select>
                <button class="btn-primary" @click="loadStats">刷新数据</button>
            </div>

            <!-- 饼图 -->
            <div class="chart-container">
                <h3>饼图 - 维度分布</h3>
                <div id="pieChart" class="chart"></div>
            </div>

            <!-- 柱状图 -->
            <div class="chart-container">
                <h3>柱状图 - 维度统计</h3>
                <div id="barChart" class="chart"></div>
            </div>

            <!-- 折线图 -->
            <div class="chart-container">
                <h3>折线图 - 调用时序</h3>
                <div id="lineChart" class="chart"></div>
            </div>
        </div>
    </div>

    <script src="app.js"></script>
</body>
</html>
```

- [ ] **Step 2: 创建 app.js**

```javascript
const { createApp } = Vue;

createApp({
    data() {
        return {
            baseUrl: 'http://localhost:8088',
            activeTab: 'helloworld',
            tabs: [
                { id: 'helloworld', label: 'HelloWorld' },
                { id: 'hash', label: '哈希算法' },
                { id: 'bubblesort', label: '冒泡排序' }
            ],

            // HelloWorld
            helloworldResult: null,
            helloworldError: null,

            // Hash
            hashInput: '',
            hashResult: null,
            hashError: null,

            // Sort
            sortResult: null,
            sortError: null,

            // Export
            exportGroupBy: 'personnel_type',

            // Charts
            chartGroupBy: 'personnel_type',
            pieChart: null,
            barChart: null,
            lineChart: null
        };
    },

    methods: {
        // --- 维度请求头 ---
        getHeaders() {
            return {
                'X-Caller': 'frontend-user',
                'X-Personnel-Type': '研发',
                'X-Personnel-Level': 'P6',
                'X-Personnel-Department': '基础架构部'
            };
        },

        // --- Tab 切换 ---
        switchTab(tabId) {
            this.activeTab = tabId;
        },

        onBaseUrlChange() {
            // 地址变更后刷新图表
            if (this.pieChart) this.loadStats();
        },

        // --- API 调用 ---
        async callHelloWorld() {
            this.helloworldError = null;
            this.helloworldResult = null;
            try {
                const res = await axios.get(`${this.baseUrl}/api/helloworld`, { headers: this.getHeaders() });
                this.helloworldResult = res.data;
            } catch (e) {
                this.helloworldError = '请求失败: ' + (e.response?.data?.message || e.message);
            }
        },

        async callHash() {
            this.hashError = null;
            this.hashResult = null;
            if (!this.hashInput.trim()) {
                this.hashError = '请输入要哈希的字符串';
                return;
            }
            try {
                const res = await axios.post(`${this.baseUrl}/api/hash`,
                    { input: this.hashInput },
                    { headers: { ...this.getHeaders(), 'Content-Type': 'application/json' } }
                );
                this.hashResult = res.data;
            } catch (e) {
                this.hashError = '请求失败: ' + (e.response?.data?.message || e.message);
            }
        },

        async callBubbleSort() {
            this.sortError = null;
            this.sortResult = null;
            try {
                const res = await axios.get(`${this.baseUrl}/api/bubblesort`, { headers: this.getHeaders() });
                this.sortResult = res.data;
            } catch (e) {
                this.sortError = '请求失败: ' + (e.response?.data?.message || e.message);
            }
        },

        // --- 导出 ---
        exportCsv() {
            window.open(`${this.baseUrl}/api/export/csv?groupBy=${this.exportGroupBy}`, '_blank');
        },

        exportXlsx() {
            window.open(`${this.baseUrl}/api/export/xlsx?groupBy=${this.exportGroupBy}`, '_blank');
        },

        // --- 图表 ---
        async loadStats() {
            try {
                const [aggRes, timelineRes] = await Promise.all([
                    axios.get(`${this.baseUrl}/api/stats?groupBy=${this.chartGroupBy}`, { headers: this.getHeaders() }),
                    axios.get(`${this.baseUrl}/api/stats/timeline`, { headers: this.getHeaders() })
                ]);

                const aggData = aggRes.data.aggregation || [];
                const timelineData = timelineRes.data.timeline || [];

                this.renderPieChart(aggData);
                this.renderBarChart(aggData);
                this.renderLineChart(timelineData);
            } catch (e) {
                console.error('加载统计数据失败:', e);
            }
        },

        renderPieChart(data) {
            if (!this.pieChart) {
                this.pieChart = echarts.init(document.getElementById('pieChart'));
            }
            this.pieChart.setOption({
                tooltip: { trigger: 'item' },
                legend: { orient: 'vertical', left: 'left' },
                series: [{
                    type: 'pie',
                    radius: '50%',
                    data: data.map(d => ({ name: d.dimension, value: d.count })),
                    emphasis: { itemStyle: { shadowBlur: 10, shadowOffsetX: 0, shadowColor: 'rgba(0,0,0,0.5)' } }
                }]
            });
        },

        renderBarChart(data) {
            if (!this.barChart) {
                this.barChart = echarts.init(document.getElementById('barChart'));
            }
            this.barChart.setOption({
                tooltip: { trigger: 'axis' },
                xAxis: { type: 'category', data: data.map(d => d.dimension) },
                yAxis: { type: 'value' },
                series: [{
                    type: 'bar',
                    data: data.map(d => d.count),
                    itemStyle: { color: '#5470c6' }
                }]
            });
        },

        renderLineChart(data) {
            if (!this.lineChart) {
                this.lineChart = echarts.init(document.getElementById('lineChart'));
            }
            this.lineChart.setOption({
                tooltip: { trigger: 'axis' },
                xAxis: { type: 'category', data: data.map(d => d.time), axisLabel: { rotate: 45 } },
                yAxis: { type: 'value' },
                series: [{
                    type: 'line',
                    data: data.map(d => d.count),
                    smooth: true,
                    itemStyle: { color: '#91cc75' }
                }]
            });
        }
    },

    mounted() {
        this.$nextTick(() => {
            // 初始化图表为空
            this.pieChart = echarts.init(document.getElementById('pieChart'));
            this.barChart = echarts.init(document.getElementById('barChart'));
            this.lineChart = echarts.init(document.getElementById('lineChart'));
            this.loadStats();
        });

        window.addEventListener('resize', () => {
            this.pieChart?.resize();
            this.barChart?.resize();
            this.lineChart?.resize();
        });
    }
}).mount('#app');
```

- [ ] **Step 3: 创建 style.css**

```css
* { margin: 0; padding: 0; box-sizing: border-box; }

body {
    font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif;
    background: #f5f7fa;
    color: #333;
    padding: 20px;
}

#app { max-width: 1200px; margin: 0 auto; }

.header {
    display: flex;
    justify-content: space-between;
    align-items: center;
    margin-bottom: 20px;
    padding: 20px;
    background: #fff;
    border-radius: 8px;
    box-shadow: 0 2px 8px rgba(0,0,0,0.1);
}
.header h1 { font-size: 24px; color: #1a73e8; }
.api-base-url { display: flex; align-items: center; gap: 8px; }
.api-base-url input {
    padding: 6px 12px; border: 1px solid #ddd; border-radius: 4px; width: 250px;
}

.tabs {
    display: flex; gap: 0;
    background: #fff; border-radius: 8px 8px 0 0; box-shadow: 0 2px 8px rgba(0,0,0,0.1);
    overflow: hidden;
}
.tab-btn {
    flex: 1; padding: 14px 20px; border: none; background: #f0f2f5;
    cursor: pointer; font-size: 16px; transition: all 0.2s;
    border-bottom: 3px solid transparent;
}
.tab-btn:hover { background: #e8eaed; }
.tab-btn.active { background: #fff; border-bottom-color: #1a73e8; color: #1a73e8; font-weight: 600; }

.tab-content { background: #fff; padding: 24px; border-radius: 0 0 8px 8px; box-shadow: 0 2px 8px rgba(0,0,0,0.1); }

.tab-panel h2 { margin-bottom: 16px; font-size: 20px; }

.btn-primary {
    padding: 10px 24px; background: #1a73e8; color: #fff; border: none;
    border-radius: 6px; cursor: pointer; font-size: 14px; transition: background 0.2s;
}
.btn-primary:hover { background: #1557b0; }

.input-group { display: flex; gap: 12px; margin-bottom: 16px; }
.input-group input {
    flex: 1; padding: 10px 14px; border: 1px solid #ddd; border-radius: 6px; font-size: 14px;
}

.result-box {
    margin-top: 16px; padding: 16px; background: #f8f9fa; border: 1px solid #e0e0e0;
    border-radius: 6px; max-height: 400px; overflow-y: auto;
}
.result-box pre { font-size: 13px; white-space: pre-wrap; word-break: break-all; }
.result-box h3 { margin: 12px 0 6px; font-size: 15px; color: #555; }
.steps-list { max-height: 250px; overflow-y: auto; }
.step-item { padding: 4px 0; font-size: 13px; font-family: monospace; border-bottom: 1px solid #eee; }

.error-box { margin-top: 16px; padding: 12px; background: #fdecea; color: #c62828; border-radius: 6px; font-size: 14px; }

.divider { margin: 30px 0; border: none; border-top: 1px solid #e0e0e0; }

.export-section, .chart-section {
    background: #fff; padding: 24px; border-radius: 8px; box-shadow: 0 2px 8px rgba(0,0,0,0.1);
    margin-bottom: 20px;
}
.export-section h2, .chart-section h2 { margin-bottom: 16px; font-size: 20px; }

.export-controls, .chart-controls {
    display: flex; gap: 12px; margin-bottom: 16px; align-items: center;
}
select {
    padding: 8px 12px; border: 1px solid #ddd; border-radius: 6px; font-size: 14px;
}

.chart-container { margin-bottom: 30px; }
.chart-container h3 { margin-bottom: 10px; font-size: 16px; color: #555; }
.chart { width: 100%; height: 350px; border: 1px solid #eee; border-radius: 6px; }
```

- [ ] **Step 4: 验证前端文件完整性**

确认三个文件已创建：
- `haikulou1.github.io-master/api-demo/index.html`
- `haikulou1.github.io-master/api-demo/app.js`
- `haikulou1.github.io-master/api-demo/style.css`

---

## Task 8: 端到端集成验证

**Files:** 无新建，验证已有

**Interfaces:**
- 验证: 后端所有接口可正常调用
- 验证: 前端页面可正常加载并调用后端

- [ ] **Step 1: 启动后端服务**

```bash
cd leecode-master/api-demo && mvn spring-boot:run &
```

等待服务启动（约 15-30 秒），观察日志输出 `Started ApiDemoApplication`。

- [ ] **Step 2: 验证 HelloWorld 接口**

```bash
curl -s http://localhost:8088/api/helloworld | python3 -m json.tool
```
预期：返回 `{"message": "Hello World", "timestamp": "..."}`。

- [ ] **Step 3: 验证哈希接口**

```bash
curl -s -X POST http://localhost:8088/api/hash \
  -H "Content-Type: application/json" \
  -d '{"input":"hello"}' | python3 -m json.tool
```
预期：返回 `md5` (5d41402abc4b2a76b9719d911017c592) 和 `sha256` 值。

- [ ] **Step 4: 验证冒泡排序接口**

```bash
curl -s http://localhost:8088/api/bubblesort | python3 -m json.tool
```
预期：返回 `original` / `sorted` / `steps` 三个字段。

- [ ] **Step 5: 验证埋点统计接口**

```bash
curl -s http://localhost:8088/api/stats?groupBy=personnel_type | python3 -m json.tool
```
预期：返回 `aggregation` 和 `totalByApi` 数据（含前几步调用产生的埋点记录）。

- [ ] **Step 6: 验证导出接口**

```bash
curl -s -o /tmp/test.csv http://localhost:8088/api/export/csv?groupBy=personnel_type && head /tmp/test.csv
curl -s -o /tmp/test.xlsx http://localhost:8088/api/export/xlsx?groupBy=personnel_type && file /tmp/test.xlsx
```
预期：CSV 文件含表头，xlsx 文件被识别为 Microsoft Excel 格式。

- [ ] **Step 7: 停止后端服务**

```bash
pkill -f "api-demo"
```

---

## Self-Review

### 1. Spec Coverage

| 需求 | 对应任务 |
|------|----------|
| HelloWorld 接口 | Task 2 |
| 哈希算法接口 (MD5/SHA256) | Task 3 |
| 冒泡排序接口 (固定数组) | Task 4 |
| 前端三 Tab 页面 | Task 7 |
| 导出按钮 + 后台导出接口 (CSV + Excel) | Task 6 |
| 埋点（调用次数+调用人） | Task 5 (AOP 自动 + 手动) |
| 前端可视化报表（折线图/饼图/柱状图） | Task 7 |
| 多维度（人员类型/层级/部门） | Task 5 + Task 7 |

### 2. Placeholder Scan

- 无 TBD / TODO / "implement later" / "fill in details"
- 无 "Add appropriate error handling" 等模糊描述
- 所有步骤包含完整代码或确切命令

### 3. Type Consistency

- `HashService.md5(String)` / `HashService.sha256(String)` → 返回 `String`（Task 3 定义，HashController 消费）
- `SortService.bubbleSort()` → 返回 `SortResult`（含 `original`/`sorted`/`steps`）（Task 4 定义，SortController 消费）
- `TrackingService.save(TrackingRecord)` / `aggregate(String)` / `timeline()` → Task 5 定义，ExportService 和 TrackingController 消费
- `ExportService.exportCsv(String)` / `exportXlsx(String)` → 返回 `byte[]`（Task 6 定义，ExportController 消费）
- 前端 `app.js` 中所有接口路径与后端 Controller 的 `@RequestMapping` 一致（`/api/helloworld`, `/api/hash`, `/api/bubblesort`, `/api/export/csv`, `/api/export/xlsx`, `/api/stats`, `/api/stats/timeline`）

### 4. 跨仓对齐点

| 对齐点 | 状态 |
|--------|------|
| 后端端口 (8088) 与前端 baseUrl 默认值一致 | ✅ |
| 请求头维度字段 (X-Caller / X-Personnel-Type / X-Personnel-Level / X-Personnel-Department) 前后端一致 | ✅ |
| 导出接口 groupBy 参数值 (personnel_type / personnel_level / personnel_department) 前后端一致 | ✅ |
| CORS 全局放行，前端可跨域调用 | ✅ |
| 冒泡排序算法复用 leecode 现有实现模式 | ✅ |
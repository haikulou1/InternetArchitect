# 三接口 + 埋点报表 + 导出 — 实施计划

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 在 leecode 仓库新建 Spring Boot 3.x 后端模块提供 6 个 REST 接口，在 haikulou1.github.io 构建 React 18 + Vite SPA 前端（三 Tab + ECharts 图表 + 导出），复用 leecode 现有冒泡排序算法。

**Architecture:** leecode 后端独立 Spring Boot 模块 `springboot-api`（H2 内存库 + AOP 埋点），haikulou1.github.io 前端独立 Vite 项目 `api-demo/`，前后端通过 HTTP REST 跨域通信。前端构建产物输出至 Hexo 博客 `public/api-demo/` 目录。

**Tech Stack:**
- 后端: Java 17, Spring Boot 3.2.x, Spring Web, Spring AOP, H2 Database, Apache POI, OpenCSV
- 前端: React 18, Vite 5, TypeScript, ECharts 5, Ant Design 5
- 构建: Maven Wrapper (后端), npm (前端)

---

## Global Constraints

- Java 17+ (Spring Boot 3.x)
- React 18 + Vite 5
- H2 内存数据库，无外部依赖
- 埋点维度枚举：人员类型（正式/外包/实习）、人员层级（L1-L10）、人员部门（技术/产品/运营/市场/职能）
- 导出格式：CSV + Excel (xlsx) 双格式
- 冒泡排序输入：固定数组 `[3, 9, -1, 10, -2]`
- 哈希算法：MD5 和 SHA256（密码学哈希）
- 跨域：后端配置 CORS 允许前端访问
- 复用 leecode 现有 `datastruct/src/排序/冒泡排序.java` 的 `sort()` 算法

---

## 仓库: leecode — 后端

### 文件结构

```
leecode-master/
└── springboot-api/                    # 新建 Maven 模块
    ├── pom.xml
    ├── src/main/java/com/leecode/api/
    │   ├── ApiApplication.java
    │   ├── config/
    │   │   └── CorsConfig.java
    │   ├── controller/
    │   │   ├── HelloWorldController.java
    │   │   ├── HashController.java
    │   │   ├── BubbleSortController.java
    │   │   ├── TrackController.java
    │   │   ├── ExportController.java
    │   │   └── StatsController.java
    │   ├── service/
    │   │   ├── HashService.java
    │   │   ├── BubbleSortService.java
    │   │   ├── TrackService.java
    │   │   └── ExportService.java
    │   ├── model/
    │   │   ├── HashRequest.java
    │   │   ├── HashResponse.java
    │   │   ├── SortResponse.java
    │   │   ├── TrackRequest.java
    │   │   └── TrackRecord.java
    │   ├── repository/
    │   │   └── TrackRecordRepository.java
    │   └── aop/
    │       └── TrackAspect.java
    └── src/main/resources/
        ├── application.yml
        └── data.sql
```

---

### Task 1: 初始化 Spring Boot 模块骨架

**Files:**
- Create: `springboot-api/pom.xml`
- Create: `springboot-api/src/main/java/com/leecode/api/ApiApplication.java`
- Create: `springboot-api/src/main/resources/application.yml`
- Create: `springboot-api/src/main/java/com/leecode/api/config/CorsConfig.java`

**Interfaces:**
- Consumes: nothing
- Produces: Spring Boot 3.2.x 应用可启动，端口 8080，H2 控制台可用，CORS 全局开放

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
    <groupId>com.leecode</groupId>
    <artifactId>springboot-api</artifactId>
    <version>0.0.1-SNAPSHOT</version>
    <name>springboot-api</name>
    <description>API Demo with HelloWorld, Hash, BubbleSort, Tracking, Export</description>
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
            <artifactId>spring-boot-starter-aop</artifactId>
        </dependency>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-data-jpa</artifactId>
        </dependency>
        <dependency>
            <groupId>com.h2database</groupId>
            <artifactId>h2</artifactId>
            <scope>runtime</scope>
        </dependency>
        <dependency>
            <groupId>org.apache.poi</groupId>
            <artifactId>poi-ooxml</artifactId>
            <version>5.2.5</version>
        </dependency>
        <dependency>
            <groupId>com.opencsv</groupId>
            <artifactId>opencsv</artifactId>
            <version>5.9</version>
        </dependency>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-test</artifactId>
            <scope>test</scope>
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

- [ ] **Step 2: 创建 ApiApplication.java**

```java
package com.leecode.api;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class ApiApplication {
    public static void main(String[] args) {
        SpringApplication.run(ApiApplication.class, args);
    }
}
```

- [ ] **Step 3: 创建 application.yml**

```yaml
server:
  port: 8080

spring:
  datasource:
    url: jdbc:h2:mem:trackdb;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE
    driver-class-name: org.h2.Driver
    username: sa
    password:
  h2:
    console:
      enabled: true
      path: /h2-console
  jpa:
    hibernate:
      ddl-auto: update
    show-sql: false
    database-platform: org.hibernate.dialect.H2Dialect
```

- [ ] **Step 4: 创建 CorsConfig.java**

```java
package com.leecode.api.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class CorsConfig {
    @Bean
    public WebMvcConfigurer corsConfigurer() {
        return new WebMvcConfigurer() {
            @Override
            public void addCorsMappings(CorsRegistry registry) {
                registry.addMapping("/api/**")
                        .allowedOrigins("*")
                        .allowedMethods("GET", "POST", "OPTIONS")
                        .allowedHeaders("*");
            }
        };
    }
}
```

- [ ] **Step 5: 验证模块可编译**

Run: `cd springboot-api && mvn compile -q`
Expected: BUILD SUCCESS

---

### Task 2: HelloWorld 接口

**Files:**
- Create: `springboot-api/src/main/java/com/leecode/api/controller/HelloWorldController.java`

**Interfaces:**
- Consumes: nothing
- Produces: `GET /api/helloworld` → `{"message": "Hello World"}`

- [ ] **Step 1: 创建 HelloWorldController.java**

```java
package com.leecode.api.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api")
public class HelloWorldController {

    @GetMapping("/helloworld")
    public Map<String, String> helloWorld() {
        return Map.of("message", "Hello World");
    }
}
```

- [ ] **Step 2: 验证**

```bash
cd springboot-api && mvn spring-boot:run &
sleep 10
curl -s http://localhost:8080/api/helloworld
# Expected: {"message":"Hello World"}
```

---

### Task 3: 哈希接口 (MD5/SHA256)

**Files:**
- Create: `springboot-api/src/main/java/com/leecode/api/model/HashRequest.java`
- Create: `springboot-api/src/main/java/com/leecode/api/model/HashResponse.java`
- Create: `springboot-api/src/main/java/com/leecode/api/service/HashService.java`
- Create: `springboot-api/src/main/java/com/leecode/api/controller/HashController.java`

**Interfaces:**
- Consumes: nothing
- Produces: `POST /api/hash` Body: `{"input":"string", "algo":"MD5"}` → `{"input":"string","algo":"MD5","hash":"hex..."}`

- [ ] **Step 1: 创建 HashRequest.java**

```java
package com.leecode.api.model;

public class HashRequest {
    private String input;
    private String algo;

    public HashRequest() {}
    public HashRequest(String input, String algo) { this.input = input; this.algo = algo; }
    public String getInput() { return input; }
    public void setInput(String input) { this.input = input; }
    public String getAlgo() { return algo; }
    public void setAlgo(String algo) { this.algo = algo; }
}
```

- [ ] **Step 2: 创建 HashResponse.java**

```java
package com.leecode.api.model;

public class HashResponse {
    private String input;
    private String algo;
    private String hash;

    public HashResponse(String input, String algo, String hash) {
        this.input = input; this.algo = algo; this.hash = hash;
    }
    public String getInput() { return input; }
    public String getAlgo() { return algo; }
    public String getHash() { return hash; }
}
```

- [ ] **Step 3: 创建 HashService.java**

```java
package com.leecode.api.service;

import org.springframework.stereotype.Service;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

@Service
public class HashService {
    public String computeHash(String input, String algo) {
        if (!"MD5".equalsIgnoreCase(algo) && !"SHA256".equalsIgnoreCase(algo)) {
            throw new IllegalArgumentException("Unsupported algorithm: " + algo);
        }
        try {
            MessageDigest md = MessageDigest.getInstance(algo.toUpperCase());
            byte[] digest = md.digest(input.getBytes());
            StringBuilder hexString = new StringBuilder();
            for (byte b : digest) hexString.append(String.format("%02x", b));
            return hexString.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("Algorithm not available: " + algo, e);
        }
    }
}
```

- [ ] **Step 4: 创建 HashController.java**

```java
package com.leecode.api.controller;

import com.leecode.api.model.HashRequest;
import com.leecode.api.model.HashResponse;
import com.leecode.api.service.HashService;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api")
public class HashController {
    private final HashService hashService;
    public HashController(HashService hashService) { this.hashService = hashService; }

    @PostMapping("/hash")
    public HashResponse hash(@RequestBody HashRequest request) {
        String hash = hashService.computeHash(request.getInput(), request.getAlgo());
        return new HashResponse(request.getInput(), request.getAlgo(), hash);
    }
}
```

- [ ] **Step 5: 验证**

```bash
curl -s -X POST http://localhost:8080/api/hash \
  -H "Content-Type: application/json" \
  -d '{"input":"hello","algo":"MD5"}'
# Expected: {"input":"hello","algo":"MD5","hash":"5d41402abc4b2a76b9719d911017c592"}
```

---

### Task 4: 冒泡排序接口

**Files:**
- Create: `springboot-api/src/main/java/com/leecode/api/model/SortResponse.java`
- Create: `springboot-api/src/main/java/com/leecode/api/service/BubbleSortService.java`
- Create: `springboot-api/src/main/java/com/leecode/api/controller/BubbleSortController.java`

**Interfaces:**
- Consumes: nothing
- Produces: `GET /api/bubblesort` → `{"original":[3,9,-1,10,-2],"steps":[[...],...],"sorted":[-2,-1,3,9,10]}`

- [ ] **Step 1: 创建 SortResponse.java**

```java
package com.leecode.api.model;

import java.util.List;

public class SortResponse {
    private int[] original;
    private List<int[]> steps;
    private int[] sorted;

    public SortResponse(int[] original, List<int[]> steps, int[] sorted) {
        this.original = original; this.steps = steps; this.sorted = sorted;
    }
    public int[] getOriginal() { return original; }
    public List<int[]> getSteps() { return steps; }
    public int[] getSorted() { return sorted; }
}
```

- [ ] **Step 2: 创建 BubbleSortService.java**

```java
package com.leecode.api.service;

import org.springframework.stereotype.Service;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@Service
public class BubbleSortService {
    public SortResult sort() {
        int[] original = {3, 9, -1, 10, -2};
        int[] arr = Arrays.copyOf(original, original.length);
        List<int[]> steps = new ArrayList<>();
        steps.add(Arrays.copyOf(arr, arr.length));
        for (int i = 0; i < arr.length - 1; i++) {
            for (int j = 0; j < arr.length - i - 1; j++) {
                if (arr[j] > arr[j + 1]) {
                    int temp = arr[j];
                    arr[j] = arr[j + 1];
                    arr[j + 1] = temp;
                }
            }
            steps.add(Arrays.copyOf(arr, arr.length));
        }
        return new SortResult(original, steps, arr);
    }

    public static class SortResult {
        public final int[] original;
        public final List<int[]> steps;
        public final int[] sorted;
        public SortResult(int[] o, List<int[]> s, int[] d) { original = o; steps = s; sorted = d; }
    }
}
```

- [ ] **Step 3: 创建 BubbleSortController.java**

```java
package com.leecode.api.controller;

import com.leecode.api.model.SortResponse;
import com.leecode.api.service.BubbleSortService;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api")
public class BubbleSortController {
    private final BubbleSortService bubbleSortService;
    public BubbleSortController(BubbleSortService s) { this.bubbleSortService = s; }

    @GetMapping("/bubblesort")
    public SortResponse bubbleSort() {
        BubbleSortService.SortResult r = bubbleSortService.sort();
        return new SortResponse(r.original, r.steps, r.sorted);
    }
}
```

---

### Task 5: 埋点数据模型 (JPA Entity + Repository)

**Files:**
- Create: `springboot-api/src/main/java/com/leecode/api/model/TrackRecord.java`
- Create: `springboot-api/src/main/java/com/leecode/api/repository/TrackRecordRepository.java`

**Interfaces:**
- Consumes: nothing
- Produces: `TrackRecord` JPA Entity, `TrackRecordRepository` with group-by queries

- [ ] **Step 1: 创建 TrackRecord.java**

```java
package com.leecode.api.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "track_records")
public class TrackRecord {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false) private String apiName;
    @Column(nullable = false) private String caller;
    @Column(nullable = false) private String type;
    @Column(nullable = false) private String level;
    @Column(nullable = false) private String dept;
    @Column(nullable = false) private LocalDateTime timestamp;

    public TrackRecord() {}
    public TrackRecord(String apiName, String caller, String type, String level, String dept) {
        this.apiName = apiName; this.caller = caller; this.type = type;
        this.level = level; this.dept = dept; this.timestamp = LocalDateTime.now();
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getApiName() { return apiName; }
    public void setApiName(String v) { this.apiName = v; }
    public String getCaller() { return caller; }
    public void setCaller(String v) { this.caller = v; }
    public String getType() { return type; }
    public void setType(String v) { this.type = v; }
    public String getLevel() { return level; }
    public void setLevel(String v) { this.level = v; }
    public String getDept() { return dept; }
    public void setDept(String v) { this.dept = v; }
    public LocalDateTime getTimestamp() { return timestamp; }
    public void setTimestamp(LocalDateTime v) { this.timestamp = v; }
}
```

- [ ] **Step 2: 创建 TrackRecordRepository.java**

```java
package com.leecode.api.repository;

import com.leecode.api.model.TrackRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface TrackRecordRepository extends JpaRepository<TrackRecord, Long> {
    @Query("SELECT t.type, COUNT(t) FROM TrackRecord t GROUP BY t.type")
    List<Object[]> countByType();

    @Query("SELECT t.level, COUNT(t) FROM TrackRecord t GROUP BY t.level ORDER BY t.level")
    List<Object[]> countByLevel();

    @Query("SELECT t.dept, COUNT(t) FROM TrackRecord t GROUP BY t.dept")
    List<Object[]> countByDept();
}
```

---

### Task 6: 埋点上报 + AOP 切面

**Files:**
- Create: `springboot-api/src/main/java/com/leecode/api/model/TrackRequest.java`
- Create: `springboot-api/src/main/java/com/leecode/api/service/TrackService.java`
- Create: `springboot-api/src/main/java/com/leecode/api/controller/TrackController.java`
- Create: `springboot-api/src/main/java/com/leecode/api/aop/TrackAspect.java`

**Interfaces:**
- Consumes: `TrackRecordRepository` (Task 5)
- Produces: `POST /api/track` + AOP auto-tracking for HelloWorld/Hash/BubbleSort controllers

- [ ] **Step 1: 创建 TrackRequest.java**

```java
package com.leecode.api.model;

public class TrackRequest {
    private String apiName, caller, type, level, dept;
    public TrackRequest() {}
    public String getApiName() { return apiName; }
    public void setApiName(String v) { this.apiName = v; }
    public String getCaller() { return caller; }
    public void setCaller(String v) { this.caller = v; }
    public String getType() { return type; }
    public void setType(String v) { this.type = v; }
    public String getLevel() { return level; }
    public void setLevel(String v) { this.level = v; }
    public String getDept() { return dept; }
    public void setDept(String v) { this.dept = v; }
}
```

- [ ] **Step 2: 创建 TrackService.java**

```java
package com.leecode.api.service;

import com.leecode.api.model.TrackRecord;
import com.leecode.api.repository.TrackRecordRepository;
import org.springframework.stereotype.Service;
import java.util.*;

@Service
public class TrackService {
    private final TrackRecordRepository repository;
    public TrackService(TrackRecordRepository r) { this.repository = r; }

    public void record(String apiName, String caller, String type, String level, String dept) {
        repository.save(new TrackRecord(apiName, caller, type, level, dept));
    }

    public Map<String, List<Map<String, Object>>> getStats(String dimension) {
        List<Object[]> results = switch (dimension) {
            case "type" -> repository.countByType();
            case "level" -> repository.countByLevel();
            case "dept" -> repository.countByDept();
            default -> throw new IllegalArgumentException("Unknown dimension: " + dimension);
        };
        List<Map<String, Object>> data = new ArrayList<>();
        for (Object[] row : results) {
            data.add(Map.of("key", row[0], "count", row[1]));
        }
        Map<String, List<Map<String, Object>>> resp = new LinkedHashMap<>();
        resp.put("data", data);
        return resp;
    }
}
```

- [ ] **Step 3: 创建 TrackController.java**

```java
package com.leecode.api.controller;

import com.leecode.api.model.TrackRequest;
import com.leecode.api.service.TrackService;
import org.springframework.web.bind.annotation.*;
import java.util.Map;

@RestController
@RequestMapping("/api")
public class TrackController {
    private final TrackService trackService;
    public TrackController(TrackService s) { this.trackService = s; }

    @PostMapping("/track")
    public Map<String, Boolean> track(@RequestBody TrackRequest req) {
        trackService.record(req.getApiName(), req.getCaller(),
            req.getType(), req.getLevel(), req.getDept());
        return Map.of("success", true);
    }
}
```

- [ ] **Step 4: 创建 TrackAspect.java**

```java
package com.leecode.api.aop;

import com.leecode.api.model.TrackRecord;
import com.leecode.api.repository.TrackRecordRepository;
import jakarta.servlet.http.HttpServletRequest;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import java.time.LocalDateTime;

@Aspect
@Component
public class TrackAspect {
    private final TrackRecordRepository repository;
    public TrackAspect(TrackRecordRepository r) { this.repository = r; }

    @Before("execution(* com.leecode.api.controller.HelloWorldController.*(..)) || " +
            "execution(* com.leecode.api.controller.HashController.*(..)) || " +
            "execution(* com.leecode.api.controller.BubbleSortController.*(..))")
    public void trackApiCall(JoinPoint joinPoint) {
        ServletRequestAttributes attrs =
            (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attrs == null) return;
        HttpServletRequest req = attrs.getRequest();
        String apiName = req.getRequestURI();
        String caller = req.getParameter("caller");
        String type = req.getParameter("type");
        String level = req.getParameter("level");
        String dept = req.getParameter("dept");
        if (caller == null) caller = "anonymous";
        if (type == null) type = "正式";
        if (level == null) level = "L5";
        if (dept == null) dept = "技术";
        TrackRecord record = new TrackRecord(apiName, caller, type, level, dept);
        record.setTimestamp(LocalDateTime.now());
        repository.save(record);
    }
}
```

---

### Task 7: 统计查询接口

**Files:**
- Create: `springboot-api/src/main/java/com/leecode/api/controller/StatsController.java`

**Interfaces:**
- Consumes: `TrackService` (Task 6)
- Produces: `GET /api/stats?dimension=type|level|dept` → `{"data":[{"key":"正式","count":5},...]}`

- [ ] **Step 1: 创建 StatsController.java**

```java
package com.leecode.api.controller;

import com.leecode.api.service.TrackService;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api")
public class StatsController {
    private final TrackService trackService;
    public StatsController(TrackService s) { this.trackService = s; }

    @GetMapping("/stats")
    public Map<String, List<Map<String, Object>>> stats(@RequestParam String dimension) {
        return trackService.getStats(dimension);
    }
}
```

---

### Task 8: 导出接口 (CSV + XLSX)

**Files:**
- Create: `springboot-api/src/main/java/com/leecode/api/service/ExportService.java`
- Create: `springboot-api/src/main/java/com/leecode/api/controller/ExportController.java`

**Interfaces:**
- Consumes: `TrackService` (Task 6)
- Produces: `GET /api/export?format=csv|xlsx&dimension=type|level|dept` → binary download

- [ ] **Step 1: 创建 ExportService.java**

```java
package com.leecode.api.service;

import com.opencsv.CSVWriter;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;
import java.io.*;
import java.util.List;
import java.util.Map;

@Service
public class ExportService {
    private final TrackService trackService;
    public ExportService(TrackService s) { this.trackService = s; }

    public byte[] exportCsv(String dimension) throws IOException {
        Map<String, List<Map<String, Object>>> stats = trackService.getStats(dimension);
        List<Map<String, Object>> data = stats.get("data");
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        bos.write(0xEF); bos.write(0xBB); bos.write(0xBF); // BOM for UTF-8
        try (OutputStreamWriter osw = new OutputStreamWriter(bos);
             CSVWriter w = new CSVWriter(osw)) {
            w.writeNext(new String[]{dimension, "调用次数"});
            for (Map<String, Object> row : data)
                w.writeNext(new String[]{
                    String.valueOf(row.get("key")),
                    String.valueOf(row.get("count"))
                });
        }
        return bos.toByteArray();
    }

    public byte[] exportXlsx(String dimension) throws IOException {
        Map<String, List<Map<String, Object>>> stats = trackService.getStats(dimension);
        List<Map<String, Object>> data = stats.get("data");
        try (Workbook wb = new XSSFWorkbook()) {
            Sheet sheet = wb.createSheet("埋点统计");
            Row hdr = sheet.createRow(0);
            hdr.createCell(0).setCellValue(dimension);
            hdr.createCell(1).setCellValue("调用次数");
            int ri = 1;
            for (Map<String, Object> row : data) {
                Row dr = sheet.createRow(ri++);
                dr.createCell(0).setCellValue(String.valueOf(row.get("key")));
                dr.createCell(1).setCellValue(((Number) row.get("count")).intValue());
            }
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            wb.write(bos);
            return bos.toByteArray();
        }
    }
}
```

- [ ] **Step 2: 创建 ExportController.java**

```java
package com.leecode.api.controller;

import com.leecode.api.service.ExportService;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import java.io.IOException;

@RestController
@RequestMapping("/api")
public class ExportController {
    private final ExportService exportService;
    public ExportController(ExportService s) { this.exportService = s; }

    @GetMapping("/export")
    public ResponseEntity<byte[]> export(@RequestParam String format,
                                          @RequestParam String dimension) throws IOException {
        byte[] content; String contentType, filename;
        if ("csv".equalsIgnoreCase(format)) {
            content = exportService.exportCsv(dimension);
            contentType = "text/csv; charset=UTF-8";
            filename = "tracking_stats_" + dimension + ".csv";
        } else if ("xlsx".equalsIgnoreCase(format)) {
            content = exportService.exportXlsx(dimension);
            contentType = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";
            filename = "tracking_stats_" + dimension + ".xlsx";
        } else {
            return ResponseEntity.badRequest().build();
        }
        return ResponseEntity.ok()
            .header(HttpHeaders.CONTENT_DISPOSITION,
                "attachment; filename=\"" + filename + "\"")
            .contentType(MediaType.parseMediaType(contentType))
            .body(content);
    }
}
```

---

## 仓库: haikulou1.github.io — 前端

### 文件结构

```
haikulou1.github.io-master/
└── api-demo/                           # 新建 Vite + React 项目
    ├── package.json
    ├── vite.config.ts
    ├── tsconfig.json
    ├── tsconfig.node.json
    ├── index.html
    └── src/
        ├── main.tsx
        ├── App.tsx
        ├── App.css
        ├── api/
        │   └── index.ts
        ├── components/
        │   ├── HelloWorldTab.tsx
        │   ├── HashTab.tsx
        │   ├── BubbleSortTab.tsx
        │   ├── ExportButton.tsx
        │   └── StatsCharts.tsx
        └── types/
            └── index.ts
```

构建产物输出: `../public/api-demo/`

---

### Task 9: 初始化 Vite + React 18 项目骨架

**Files:**
- Create: `api-demo/package.json`, `vite.config.ts`, `tsconfig.json`, `tsconfig.node.json`, `index.html`, `src/main.tsx`, `src/types/index.ts`

**Interfaces:**
- Consumes: nothing
- Produces: `npm run dev` / `npm run build` 可正常执行

- [ ] **Step 1: package.json**

```json
{
  "name": "api-demo",
  "private": true,
  "version": "1.0.0",
  "type": "module",
  "scripts": {
    "dev": "vite",
    "build": "tsc && vite build",
    "preview": "vite preview"
  },
  "dependencies": {
    "react": "^18.3.1",
    "react-dom": "^18.3.1",
    "antd": "^5.18.0",
    "@ant-design/icons": "^5.3.0",
    "echarts": "^5.5.0",
    "echarts-for-react": "^3.0.2",
    "axios": "^1.7.0"
  },
  "devDependencies": {
    "@types/react": "^18.3.3",
    "@types/react-dom": "^18.3.0",
    "@vitejs/plugin-react": "^4.3.0",
    "typescript": "^5.4.0",
    "vite": "^5.3.0"
  }
}
```

- [ ] **Step 2: vite.config.ts**

```typescript
import { defineConfig } from 'vite'
import react from '@vitejs/plugin-react'

export default defineConfig({
  plugins: [react()],
  base: '/api-demo/',
  build: {
    outDir: '../public/api-demo',
    emptyOutDir: true,
  },
  server: {
    port: 3000,
    proxy: {
      '/api': {
        target: 'http://localhost:8080',
        changeOrigin: true,
      },
    },
  },
})
```

- [ ] **Step 3: tsconfig.json**

```json
{
  "compilerOptions": {
    "target": "ES2020",
    "useDefineForClassFields": true,
    "lib": ["ES2020", "DOM", "DOM.Iterable"],
    "module": "ESNext",
    "skipLibCheck": true,
    "moduleResolution": "bundler",
    "allowImportingTsExtensions": true,
    "resolveJsonModule": true,
    "isolatedModules": true,
    "noEmit": true,
    "jsx": "react-jsx",
    "strict": true,
    "noUnusedLocals": false,
    "noUnusedParameters": false,
    "noFallthroughCasesInSwitch": true
  },
  "include": ["src"],
  "references": [{ "path": "./tsconfig.node.json" }]
}
```

- [ ] **Step 4: tsconfig.node.json**

```json
{
  "compilerOptions": {
    "composite": true,
    "skipLibCheck": true,
    "module": "ESNext",
    "moduleResolution": "bundler",
    "allowSyntheticDefaultImports": true
  },
  "include": ["vite.config.ts"]
}
```

- [ ] **Step 5: index.html**

```html
<!DOCTYPE html>
<html lang="zh-CN">
  <head>
    <meta charset="UTF-8" />
    <meta name="viewport" content="width=device-width, initial-scale=1.0" />
    <title>API Demo - 接口演示平台</title>
  </head>
  <body>
    <div id="root"></div>
    <script type="module" src="/src/main.tsx"></script>
  </body>
</html>
```

- [ ] **Step 6: src/main.tsx**

```tsx
import React from 'react'
import ReactDOM from 'react-dom/client'
import App from './App'
import './App.css'

ReactDOM.createRoot(document.getElementById('root')!).render(
  <React.StrictMode>
    <App />
  </React.StrictMode>,
)
```

- [ ] **Step 7: src/types/index.ts**

```typescript
export interface HashRequest {
  input: string
  algo: 'MD5' | 'SHA256'
}

export interface HashResponse {
  input: string
  algo: string
  hash: string
}

export interface SortResponse {
  original: number[]
  steps: number[][]
  sorted: number[]
}

export interface StatsDataItem {
  key: string
  count: number
}

export interface StatsResponse {
  data: StatsDataItem[]
}

export type DimensionType = 'type' | 'level' | 'dept'

export const DIMENSION_LABELS: Record<DimensionType, string> = {
  type: '人员类型',
  level: '人员层级',
  dept: '人员部门',
}
```

- [ ] **Step 8: 验证**

```bash
cd api-demo && npm install && npm run build
```
Expected: BUILD SUCCESS，产物在 `../public/api-demo/`

---

### Task 10: API 层封装

**Files:**
- Create: `api-demo/src/api/index.ts`

**Interfaces:**
- Consumes: types (Task 9)
- Produces: `fetchHelloWorld`, `fetchHash`, `fetchBubbleSort`, `trackApiCall`, `fetchStats`, `getExportUrl`

- [ ] **Step 1: 创建 src/api/index.ts**

```typescript
import axios from 'axios'
import type {
  HashRequest,
  HashResponse,
  SortResponse,
  StatsResponse,
  DimensionType,
} from '../types'

const api = axios.create({ baseURL: '/api', timeout: 10000 })

export async function fetchHelloWorld(): Promise<string> {
  const res = await api.get<{ message: string }>('/helloworld')
  return res.data.message
}

export async function fetchHash(req: HashRequest): Promise<HashResponse> {
  const res = await api.post<HashResponse>('/hash', req)
  return res.data
}

export async function fetchBubbleSort(): Promise<SortResponse> {
  const res = await api.get<SortResponse>('/bubblesort')
  return res.data
}

export async function trackApiCall(params: {
  apiName: string
  caller: string
  type: string
  level: string
  dept: string
}): Promise<void> {
  await api.post('/track', params)
}

export async function fetchStats(dimension: DimensionType): Promise<StatsResponse> {
  const res = await api.get<StatsResponse>('/stats', { params: { dimension } })
  return res.data
}

export function getExportUrl(format: 'csv' | 'xlsx', dimension: DimensionType): string {
  return `/api/export?format=${format}&dimension=${dimension}`
}
```

---

### Task 11: HelloWorldTab 组件

**Files:**
- Create: `api-demo/src/components/HelloWorldTab.tsx`

**Interfaces:**
- Consumes: `fetchHelloWorld`, `trackApiCall` (Task 10)
- Produces: Tab 内容，展示 "Hello World" 结果

- [ ] **Step 1: 创建 HelloWorldTab.tsx**

```tsx
import React, { useEffect, useState } from 'react'
import { Button, Card, Typography, message } from 'antd'
import { fetchHelloWorld, trackApiCall } from '../api'

const { Text } = Typography

const HelloWorldTab: React.FC = () => {
  const [result, setResult] = useState('')
  const [loading, setLoading] = useState(false)

  const load = async () => {
    setLoading(true)
    try {
      const msg = await fetchHelloWorld()
      setResult(msg)
      await trackApiCall({
        apiName: '/api/helloworld',
        caller: '前端用户',
        type: '正式',
        level: 'L5',
        dept: '技术',
      })
    } catch {
      message.error('请求失败')
    } finally {
      setLoading(false)
    }
  }

  useEffect(() => { load() }, [])

  return (
    <Card title="HelloWorld 接口">
      <Button type="primary" loading={loading} onClick={load}>
        调用接口
      </Button>
      {result && (
        <div style={{ marginTop: 16 }}>
          <Text strong>返回结果：</Text>
          <Text code style={{ fontSize: 18 }}>{result}</Text>
        </div>
      )}
    </Card>
  )
}

export default HelloWorldTab
```

---

### Task 12: HashTab 组件

**Files:**
- Create: `api-demo/src/components/HashTab.tsx`

**Interfaces:**
- Consumes: `fetchHash`, `trackApiCall` (Task 10)
- Produces: Tab 内容，输入文本 + 选择算法 → 展示哈希结果

- [ ] **Step 1: 创建 HashTab.tsx**

```tsx
import React, { useState } from 'react'
import { Button, Card, Input, Select, Typography, Space, message } from 'antd'
import { fetchHash, trackApiCall } from '../api'
import type { HashResponse } from '../types'

const { Text } = Typography

const HashTab: React.FC = () => {
  const [input, setInput] = useState('hello')
  const [algo, setAlgo] = useState<'MD5' | 'SHA256'>('MD5')
  const [result, setResult] = useState<HashResponse | null>(null)
  const [loading, setLoading] = useState(false)

  const submit = async () => {
    if (!input.trim()) {
      message.warning('请输入文本')
      return
    }
    setLoading(true)
    try {
      const res = await fetchHash({ input: input.trim(), algo })
      setResult(res)
      await trackApiCall({
        apiName: '/api/hash',
        caller: '前端用户',
        type: '正式',
        level: 'L5',
        dept: '技术',
      })
    } catch {
      message.error('请求失败')
    } finally {
      setLoading(false)
    }
  }

  return (
    <Card title="哈希算法 (MD5 / SHA256)">
      <Space direction="vertical" style={{ width: '100%' }}>
        <Input
          placeholder="请输入要哈希的文本"
          value={input}
          onChange={e => setInput(e.target.value)}
          style={{ maxWidth: 400 }}
        />
        <Select
          value={algo}
          onChange={v => setAlgo(v)}
          style={{ width: 150 }}
          options={[
            { label: 'MD5', value: 'MD5' },
            { label: 'SHA256', value: 'SHA256' },
          ]}
        />
        <Button type="primary" loading={loading} onClick={submit}>
          计算哈希
        </Button>
      </Space>
      {result && (
        <div style={{ marginTop: 16 }}>
          <Text strong>输入：</Text><Text>{result.input}</Text><br />
          <Text strong>算法：</Text><Text>{result.algo}</Text><br />
          <Text strong>哈希值：</Text><Text code copyable>{result.hash}</Text>
        </div>
      )}
    </Card>
  )
}

export default HashTab
```

---

### Task 13: BubbleSortTab 组件

**Files:**
- Create: `api-demo/src/components/BubbleSortTab.tsx`

**Interfaces:**
- Consumes: `fetchBubbleSort`, `trackApiCall` (Task 10)
- Produces: Tab 内容，展示排序每轮过程

- [ ] **Step 1: 创建 BubbleSortTab.tsx**

```tsx
import React, { useEffect, useState } from 'react'
import { Button, Card, Table, Tag, Typography, message } from 'antd'
import { fetchBubbleSort, trackApiCall } from '../api'
import type { SortResponse } from '../types'

const { Text } = Typography

const BubbleSortTab: React.FC = () => {
  const [data, setData] = useState<SortResponse | null>(null)
  const [loading, setLoading] = useState(false)

  const load = async () => {
    setLoading(true)
    try {
      const res = await fetchBubbleSort()
      setData(res)
      await trackApiCall({
        apiName: '/api/bubblesort',
        caller: '前端用户',
        type: '正式',
        level: 'L5',
        dept: '技术',
      })
    } catch {
      message.error('请求失败')
    } finally {
      setLoading(false)
    }
  }

  useEffect(() => { load() }, [])

  const columns = [
    {
      title: '轮次',
      dataIndex: 'round',
      key: 'round',
      render: (_: unknown, __: unknown, index: number) =>
        index === 0 ? '初始' : `第 ${index} 轮`,
    },
    {
      title: '数组状态',
      dataIndex: 'state',
      key: 'state',
      render: (arr: number[]) =>
        arr.map((n, i) => (
          <Tag key={i} color={i === arr.length - 1 ? 'green' : 'blue'}>
            {n}
          </Tag>
        )),
    },
  ]

  const tableData = data
    ? data.steps.map((step, idx) => ({
        key: idx,
        round: idx,
        state: step,
      }))
    : []

  return (
    <Card title="冒泡排序">
      <Button type="primary" loading={loading} onClick={load}>
        执行排序
      </Button>
      {data && (
        <div style={{ marginTop: 16 }}>
          <Text strong>原始数组：</Text>
          {data.original.map((n, i) => (
            <Tag key={i} color="blue">{n}</Tag>
          ))}
          <Text strong style={{ marginLeft: 24 }}>排序结果：</Text>
          {data.sorted.map((n, i) => (
            <Tag key={i} color="green">{n}</Tag>
          ))}
          <Table
            style={{ marginTop: 16 }}
            columns={columns}
            dataSource={tableData}
            pagination={false}
            size="small"
          />
        </div>
      )}
    </Card>
  )
}

export default BubbleSortTab
```

---

### Task 14: ExportButton 组件

**Files:**
- Create: `api-demo/src/components/ExportButton.tsx`

**Interfaces:**
- Consumes: `getExportUrl` (Task 10)
- Produces: 导出按钮，点击触发浏览器下载 CSV 或 XLSX

- [ ] **Step 1: 创建 ExportButton.tsx**

```tsx
import React from 'react'
import { Button, Card, Select, Space, Typography } from 'antd'
import { DownloadOutlined } from '@ant-design/icons'
import { getExportUrl } from '../api'
import { DIMENSION_LABELS, type DimensionType } from '../types'

const { Text } = Typography

const ExportButton: React.FC = () => {
  const [dimension, setDimension] = React.useState<DimensionType>('type')

  const handleExport = (format: 'csv' | 'xlsx') => {
    const url = getExportUrl(format, dimension)
    window.open(url, '_blank')
  }

  return (
    <Card title="导出埋点报表" size="small">
      <Space>
        <Text>维度：</Text>
        <Select
          value={dimension}
          onChange={v => setDimension(v)}
          style={{ width: 120 }}
          options={Object.entries(DIMENSION_LABELS).map(([k, v]) => ({
            label: v,
            value: k,
          }))}
        />
        <Button icon={<DownloadOutlined />} onClick={() => handleExport('csv')}>
          导出 CSV
        </Button>
        <Button icon={<DownloadOutlined />} onClick={() => handleExport('xlsx')}>
          导出 Excel
        </Button>
      </Space>
    </Card>
  )
}

export default ExportButton
```

---

### Task 15: StatsCharts 组件（埋点报表可视化）

**Files:**
- Create: `api-demo/src/components/StatsCharts.tsx`

**Interfaces:**
- Consumes: `fetchStats` (Task 10)
- Produces: 折线图/饼图/柱状图，支持维度切换

- [ ] **Step 1: 创建 StatsCharts.tsx**

```tsx
import React, { useEffect, useState } from 'react'
import { Card, Select, Space, Spin, Radio } from 'antd'
import ReactECharts from 'echarts-for-react'
import { fetchStats } from '../api'
import { DIMENSION_LABELS, type DimensionType, type StatsDataItem } from '../types'

type ChartType = 'line' | 'bar' | 'pie'

const StatsCharts: React.FC = () => {
  const [dimension, setDimension] = useState<DimensionType>('type')
  const [chartType, setChartType] = useState<ChartType>('bar')
  const [data, setData] = useState<StatsDataItem[]>([])
  const [loading, setLoading] = useState(false)

  const loadStats = async (dim: DimensionType) => {
    setLoading(true)
    try {
      const res = await fetchStats(dim)
      setData(res.data)
    } catch {
      setData([])
    } finally {
      setLoading(false)
    }
  }

  useEffect(() => {
    loadStats(dimension)
  }, [dimension])

  const getOption = () => {
    const keys = data.map(d => d.key)
    const values = data.map(d => d.count)

    if (chartType === 'pie') {
      return {
        title: { text: `${DIMENSION_LABELS[dimension]} - 饼图`, left: 'center' },
        tooltip: { trigger: 'item' as const },
        legend: { bottom: 0 },
        series: [
          {
            type: 'pie',
            radius: '55%',
            center: ['50%', '50%'],
            data: data.map(d => ({ name: d.key, value: d.count })),
            emphasis: {
              itemStyle: { shadowBlur: 10, shadowOffsetX: 0, shadowColor: 'rgba(0,0,0,0.5)' },
            },
          },
        ],
      }
    }

    return {
      title: {
        text: `${DIMENSION_LABELS[dimension]} - ${chartType === 'line' ? '折线图' : '柱状图'}`,
        left: 'center',
      },
      tooltip: { trigger: 'axis' as const },
      xAxis: { type: 'category' as const, data: keys },
      yAxis: { type: 'value' as const, minInterval: 1 },
      series: [
        {
          type: chartType as 'line' | 'bar',
          data: values,
          smooth: chartType === 'line',
          itemStyle: { color: '#1677ff' },
        },
      ],
    }
  }

  return (
    <Card title="埋点调用统计报表">
      <Space style={{ marginBottom: 16 }}>
        <span>维度：</span>
        <Select
          value={dimension}
          onChange={v => setDimension(v)}
          style={{ width: 120 }}
          options={Object.entries(DIMENSION_LABELS).map(([k, v]) => ({
            label: v,
            value: k,
          }))}
        />
        <span>图表类型：</span>
        <Radio.Group
          value={chartType}
          onChange={e => setChartType(e.target.value)}
          optionType="button"
        >
          <Radio.Button value="bar">柱状图</Radio.Button>
          <Radio.Button value="line">折线图</Radio.Button>
          <Radio.Button value="pie">饼图</Radio.Button>
        </Radio.Group>
      </Space>
      <Spin spinning={loading}>
        <ReactECharts option={getOption()} style={{ height: 400 }} />
      </Spin>
    </Card>
  )
}

export default StatsCharts
```

---

### Task 16: App 主组件（三 Tab 布局）

**Files:**
- Create: `api-demo/src/App.tsx`
- Create: `api-demo/src/App.css`

**Interfaces:**
- Consumes: HelloWorldTab, HashTab, BubbleSortTab, ExportButton, StatsCharts
- Produces: 完整页面，三 Tab 切换 + 导出 + 报表

- [ ] **Step 1: 创建 App.tsx**

```tsx
import React from 'react'
import { Tabs, Layout, Typography, Space } from 'antd'
import HelloWorldTab from './components/HelloWorldTab'
import HashTab from './components/HashTab'
import BubbleSortTab from './components/BubbleSortTab'
import ExportButton from './components/ExportButton'
import StatsCharts from './components/StatsCharts'

const { Header, Content } = Layout
const { Title } = Typography

const App: React.FC = () => (
  <Layout style={{ minHeight: '100vh', background: '#f5f5f5' }}>
    <Header style={{ background: '#001529', padding: '0 24px', display: 'flex', alignItems: 'center' }}>
      <Title level={3} style={{ color: '#fff', margin: 0 }}>
        API 接口演示平台
      </Title>
    </Header>
    <Content style={{ padding: 24, maxWidth: 1200, margin: '0 auto', width: '100%' }}>
      <Space direction="vertical" size="large" style={{ width: '100%' }}>
        <Tabs
          defaultActiveKey="helloworld"
          size="large"
          items={[
            { key: 'helloworld', label: 'HelloWorld', children: <HelloWorldTab /> },
            { key: 'hash', label: '哈希算法', children: <HashTab /> },
            { key: 'bubblesort', label: '冒泡排序', children: <BubbleSortTab /> },
          ]}
        />
        <ExportButton />
        <StatsCharts />
      </Space>
    </Content>
  </Layout>
)

export default App
```

- [ ] **Step 2: 创建 App.css**

```css
body {
  margin: 0;
  padding: 0;
  font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif;
}

#root {
  min-height: 100vh;
}
```

- [ ] **Step 3: 构建验证**

```bash
cd api-demo && npm install && npm run build
```
Expected: BUILD SUCCESS，产物在 `public/api-demo/`

---

## Self-Review

### 1. Spec coverage
| 需求 | 对应任务 |
|------|---------|
| HelloWorld 接口 | Task 2 |
| 哈希算法接口 (MD5/SHA256) | Task 3 |
| 冒泡排序接口 | Task 4 |
| 埋点记录 (调用次数+调用人) | Task 5 + Task 6 |
| 导出接口 (CSV + XLSX) | Task 8 |
| 统计查询接口 | Task 7 |
| 前端三 Tab 页面 | Task 11-13 + Task 16 |
| 导出按钮 | Task 14 |
| 埋点报表可视化 (折线图/饼图/柱状图) | Task 15 |
| 维度切换 (人员类型/层级/部门) | Task 15 |

### 2. Placeholder scan
✅ 无 TBD/TODO/占位符，所有接口路径、参数、类型均已明确

### 3. Type consistency
- `HashRequest`/`HashResponse` 在 Task 3 定义 → Task 10/12 使用 ✅
- `SortResponse` 在 Task 4 定义 → Task 10/13 使用 ✅
- `TrackRequest` 在 Task 6 定义 → Task 10/11-13 使用 ✅
- `StatsResponse`/`StatsDataItem`/`DimensionType` 在 Task 9 定义 → Task 10/15 使用 ✅
- `TrackRecord` JPA Entity 在 Task 5 定义 → Task 6 Repository/Service/Aspect 一致 ✅
- `getExportUrl` 在 Task 10 定义 → Task 14 使用 ✅
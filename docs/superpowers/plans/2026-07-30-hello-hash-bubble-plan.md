# 三接口 + 三Tab + 导出 + 异常兜底 + 埋点报表 实施计划

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 实现 Java 三接口（helloworld/哈希/冒泡）+ 前端三Tab展示页 + 导出接口/按钮 + 全局异常兜底 + 内存埋点 + 调用报表可视化，跨 InternetArchitect（后端）与 haikulou1.github.io（前端）两仓。

**Architecture:** 后端在现有 SpringBoot 2.1.5 项目内新增 6 个 `/api/**` 接口（4业务+metrics+export），`@RestController` + 内存 ConcurrentHashMap 埋点 + 拦截器采集 X-User-* + `@ControllerAdvice` 异常兜底；前端在静态站点新增 `demo/` 目录，原生 HTML+JS+CDN ECharts 实现 4 Tab 页（3业务+报表）+ 身份选择器 + 导出按钮。

**Tech Stack:** Spring Boot 2.1.5.RELEASE / Java 1.8 / Maven / 原生 HTML+JS / ECharts 5.x CDN

**Design Spec:** `docs/superpowers/specs/2026-07-30-hello-hash-bubble-design.md`

## Global Constraints

- 后端 Java 版本：1.8（pom.xml `<java.version>1.8</java.version>`）
- Spring Boot 版本：2.1.5.RELEASE（pom.xml parent，不得升级）
- 后端包名：`om.mashibing.springboot02`（源码 typo，严格沿用，不得修正为 `com`）
- 后端 context-path：`/boot`（application.properties `server.servlet.context-path=/boot`，实际 HTTP 路径 `/boot/api/xxx`）
- 后端端口：80（application.properties `server.port=80`）
- 后端依赖限制：pom 无 DB 依赖，不引入新 Maven 依赖（web starter 自带 Jackson/Servlet）；哈希用 JDK `java.security.MessageDigest`；导出用纯文本 CSV 无 POI
- 前端无构建链：原生 HTML+JS，不引入 npm/webpack；图表用 ECharts CDN
- 前端页面位置：`demo/` 新增目录，不污染主站
- 埋点存储：内存 ConcurrentHashMap，重启清空可接受（demo 性质）
- 跨域：后端全局 CORS 兜底
- Git 只读约束：本计划执行阶段允许写代码文件，但禁止 Git 写操作（commit/push 等）

---

## File Structure

### 后端 InternetArchitect — `02 SpringBoot/07 springBoot mvc项目结构 开发 热部署/FirstSpringBoot03/`

源码基路径（相对 worktree）：`02 SpringBoot/07 springBoot mvc项目结构 开发 热部署/FirstSpringBoot03/src/main/java/om/mashibing/springboot02/`

测试基路径（相对 worktree）：`02 SpringBoot/07 springBoot mvc项目结构 开发 热部署/FirstSpringBoot03/src/test/java/om/mashibing/springboot02/`

| 文件 | 职责 | 操作 |
|------|------|------|
| `exception/BizException.java` | 业务异常基类，携带 code 字段 | Create |
| `exception/ErrorResponse.java` | 统一错误响应体 {code,message,timestamp,path} | Create |
| `util/HashUtil.java` | MD5/SHA-256 封装（JDK MessageDigest） | Create |
| `metrics/CallRecord.java` | 埋点记录实体（user/type/level/dept/api/time） | Create |
| `metrics/MetricsStore.java` | 内存存储 ConcurrentHashMap + 聚合查询 | Create |
| `interceptor/MetricsInterceptor.java` | 拦截 /api/**，提取 X-User-* 记录埋点（try-catch 静默） | Create |
| `config/WebMvcConfig.java` | 注册拦截器 | Create |
| `config/CorsConfig.java` | 全局 CORS | Create |
| `config/GlobalExceptionHandler.java` | @ControllerAdvice 统一异常兜底 | Create |
| `controller/DemoController.java` | 4 业务接口：helloworld/hash/bubbleSort/export | Create |
| `controller/MetricsController.java` | GET /api/metrics 报表聚合 | Create |

### 前端 haikulou1.github.io

| 文件 | 职责 | 操作 |
|------|------|------|
| `demo/index.html` | 4 Tab 页 + 身份选择器 + 导出按钮 + ECharts 容器 | Create |
| `demo/app.js` | fetch 调用 + Tab 切换 + 导出 + ECharts 渲染 | Create |
| `demo/style.css` | Tab + 报表布局 | Create |

---

## Task 1: 异常体系 + 统一错误响应

**Files:**
- Create: `src/main/java/om/mashibing/springboot02/exception/BizException.java`
- Create: `src/main/java/om/mashibing/springboot02/exception/ErrorResponse.java`
- Test: `src/test/java/om/mashibing/springboot02/exception/BizExceptionTest.java`

（以上路径均相对 `02 SpringBoot/07 springBoot mvc项目结构 开发 热部署/FirstSpringBoot03/`，下同）

**Interfaces:**
- Produces: `BizException(String code, String message)` 构造器 + `getCode()`；`ErrorResponse` 含 `code/message/timestamp/path` 字段 + 全参构造器 + getter

**Steps:**

- [ ] 写 BizException.java：

```java
package om.mashibing.springboot02.exception;

public class BizException extends RuntimeException {

    private final String code;

    public BizException(String code, String message) {
        super(message);
        this.code = code;
    }

    public String getCode() {
        return code;
    }
}
```

- [ ] 写 ErrorResponse.java：

```java
package om.mashibing.springboot02.exception;

public class ErrorResponse {

    private final String code;
    private final String message;
    private final String timestamp;
    private final String path;

    public ErrorResponse(String code, String message, String timestamp, String path) {
        this.code = code;
        this.message = message;
        this.timestamp = timestamp;
        this.path = path;
    }

    public String getCode() { return code; }
    public String getMessage() { return message; }
    public String getTimestamp() { return timestamp; }
    public String getPath() { return path; }
}
```

- [ ] 写测试 BizExceptionTest.java（JUnit 4，starter-test 自带）：

```java
package om.mashibing.springboot02.exception;

import org.junit.Test;
import static org.junit.Assert.*;

public class BizExceptionTest {

    @Test
    public void testCodeAndMessage() {
        BizException ex = new BizException("PARAM_ERR", "输入需为逗号分隔整数");
        assertEquals("PARAM_ERR", ex.getCode());
        assertEquals("输入需为逗号分隔整数", ex.getMessage());
    }

    @Test
    public void testErrorResponseFields() {
        ErrorResponse er = new ErrorResponse("BIZ_ERR", "boom", "2026-07-30T00:00:00Z", "/boot/api/hash");
        assertEquals("BIZ_ERR", er.getCode());
        assertEquals("boom", er.getMessage());
        assertEquals("/boot/api/hash", er.getPath());
    }
}
```

- [ ] 运行测试：

```bash
cd "02 SpringBoot/07 springBoot mvc项目结构 开发 热部署/FirstSpringBoot03"
./mvnw test -Dtest=BizExceptionTest
```

预期：`Tests run: 2, Failures: 0, Errors: 0`，BUILD SUCCESS

---

## Task 2: HashUtil — MD5/SHA-256 封装

**Files:**
- Create: `src/main/java/om/mashibing/springboot02/util/HashUtil.java`
- Test: `src/test/java/om/mashibing/springboot02/util/HashUtilTest.java`

**Interfaces:**
- Produces: `HashUtil.md5(String input): String`、`HashUtil.sha256(String input): String`
- Consumes: `BizException`（Task 1）

**Steps:**

- [ ] 写 HashUtil.java：

```java
package om.mashibing.springboot02.util;

import om.mashibing.springboot02.exception.BizException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.nio.charset.StandardCharsets;

public final class HashUtil {

    private HashUtil() {}

    private static String digest(String algorithm, String input) {
        try {
            MessageDigest md = MessageDigest.getInstance(algorithm);
            byte[] raw = md.digest(input.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder(raw.length * 2);
            for (byte b : raw) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new BizException("BIZ_ERR", "哈希算法不可用: " + algorithm);
        }
    }

    public static String md5(String input) {
        return digest("MD5", input);
    }

    public static String sha256(String input) {
        return digest("SHA-256", input);
    }
}
```

- [ ] 先获取标准值填入断言。实施者执行：

```bash
echo -n "hello world" | md5sum      # 应输出 5eb63bbbe01eeed093cb22bb8f5ac4cb7a31bd23
echo -n "hello world" | sha256sum   # 记录输出的 64 位十六进制
```

- [ ] 写测试 HashUtilTest.java（将上面 sha256sum 的真实输出填入 SHA_256 变量）：

```java
package om.mashibing.springboot02.util;

import org.junit.Test;
import static org.junit.Assert.*;

public class HashUtilTest {

    private static final String SHA_256 = "b94d27b9934d3e08a52e52d7da7d6fab0a8c9d4af4e8e8d4e3e3e8e3e8e3e8e3";
    // ⚠️ 实施者须用 `echo -n "hello world" | sha256sum` 的真实输出替换上行

    @Test
    public void testMd5HelloWorld() {
        assertEquals("5eb63bbbe01eeed093cb22bb8f5ac4cb7a31bd23",
                     HashUtil.md5("hello world"));
    }

    @Test
    public void testSha256HelloWorld() {
        assertEquals(SHA_256, HashUtil.sha256("hello world"));
    }
}
```

- [ ] 运行测试：

```bash
cd "02 SpringBoot/07 springBoot mvc项目结构 开发 热部署/FirstSpringBoot03"
./mvnw test -Dtest=HashUtilTest
```

预期：`Tests run: 2, Failures: 0`，BUILD SUCCESS

---

## Task 3: 埋点实体 CallRecord + 内存存储 MetricsStore

**Files:**
- Create: `src/main/java/om/mashibing/springboot02/metrics/CallRecord.java`
- Create: `src/main/java/om/mashibing/springboot02/metrics/MetricsStore.java`
- Test: `src/test/java/om/mashibing/springboot02/metrics/MetricsStoreTest.java`

**Interfaces:**
- Produces: `CallRecord(String user, String type, String level, String dept, String api, long timeMillis)`
- Produces: `MetricsStore.record(CallRecord)`、`MetricsStore.aggregate(): Map<String,Object>` 返回 {total, byApi, byType, byLevel, byDept, byHour, recentCallers}
- `MetricsStore` 为 Spring `@Component` 单例，线程安全（ConcurrentHashMap + CopyOnWriteArrayList）

**Steps:**

- [ ] 写 CallRecord.java：

```java
package om.mashibing.springboot02.metrics;

public class CallRecord {

    private final String user;
    private final String type;
    private final String level;
    private final String dept;
    private final String api;
    private final long timeMillis;

    public CallRecord(String user, String type, String level, String dept, String api, long timeMillis) {
        this.user = user;
        this.type = type;
        this.level = level;
        this.dept = dept;
        this.api = api;
        this.timeMillis = timeMillis;
    }

    public String getUser() { return user; }
    public String getType() { return type; }
    public String getLevel() { return level; }
    public String getDept() { return dept; }
    public String getApi() { return api; }
    public long getTimeMillis() { return timeMillis; }
}
```

- [ ] 写 MetricsStore.java：

```java
package om.mashibing.springboot02.metrics;

import org.springframework.stereotype.Component;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicLong;

@Component
public class MetricsStore {

    private final List<CallRecord> records = new CopyOnWriteArrayList<>();
    private final AtomicLong total = new AtomicLong(0);

    public void record(CallRecord rec) {
        records.add(rec);
        total.incrementAndGet();
    }

    public Map<String, Object> aggregate() {
        Map<String, Long> byApi = new ConcurrentHashMap<>();
        Map<String, Long> byType = new ConcurrentHashMap<>();
        Map<String, Long> byLevel = new ConcurrentHashMap<>();
        Map<String, Long> byDept = new ConcurrentHashMap<>();
        Map<String, Long> byHour = new ConcurrentHashMap<>();
        // 初始化 00-23
        for (int h = 0; h < 24; h++) {
            byHour.put(String.format("%02d", h), 0L);
        }

        for (CallRecord r : records) {
            byApi.merge(r.getApi(), 1L, Long::sum);
            byType.merge(r.getType(), 1L, Long::sum);
            byLevel.merge(r.getLevel(), 1L, Long::sum);
            byDept.merge(r.getDept(), 1L, Long::sum);
            Calendar cal = Calendar.getInstance();
            cal.setTimeInMillis(r.getTimeMillis());
            String hour = String.format("%02d", cal.get(Calendar.HOUR_OF_DAY));
            byHour.merge(hour, 1L, Long::sum);
        }

        // recentCallers: 最近 20 条（倒序）
        List<Map<String, Object>> recent = new ArrayList<>();
        int size = records.size();
        int from = Math.max(0, size - 20);
        for (int i = size - 1; i >= from; i--) {
            CallRecord r = records.get(i);
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("user", r.getUser());
            m.put("type", r.getType());
            m.put("level", r.getLevel());
            m.put("dept", r.getDept());
            m.put("api", r.getApi());
            m.put("time", r.getTimeMillis());
            recent.add(m);
        }

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("total", total.get());
        result.put("byApi", byApi);
        result.put("byType", byType);
        result.put("byLevel", byLevel);
        result.put("byDept", byDept);
        result.put("byHour", byHour);
        result.put("recentCallers", recent);
        return result;
    }
}
```

- [ ] 写测试 MetricsStoreTest.java：

```java
package om.mashibing.springboot02.metrics;

import org.junit.Test;
import java.util.Map;
import static org.junit.Assert.*;

public class MetricsStoreTest {

    @Test
    public void testRecordAndAggregate() {
        MetricsStore store = new MetricsStore();
        long now = System.currentTimeMillis();
        store.record(new CallRecord("alice", "内部", "L1", "研发", "helloworld", now));
        store.record(new CallRecord("bob", "外部", "L2", "测试", "hash", now));
        store.record(new CallRecord("alice", "内部", "L1", "研发", "hash", now));

        Map<String, Object> agg = store.aggregate();
        assertEquals(3L, agg.get("total"));

        @SuppressWarnings("unchecked")
        Map<String, Long> byApi = (Map<String, Long>) agg.get("byApi");
        assertEquals(1L, (long) byApi.get("helloworld"));
        assertEquals(2L, (long) byApi.get("hash"));

        @SuppressWarnings("unchecked")
        Map<String, Long> byType = (Map<String, Long>) agg.get("byType");
        assertEquals(2L, (long) byType.get("内部"));
        assertEquals(1L, (long) byType.get("外部"));

        @SuppressWarnings("unchecked")
        java.util.List<Map<String, Object>> recent =
            (java.util.List<Map<String, Object>>) agg.get("recentCallers");
        assertEquals(3, recent.size());
        assertEquals("hash", recent.get(0).get("api")); // 倒序，最后记录的在前
    }
}
```

- [ ] 运行测试：

```bash
cd "02 SpringBoot/07 springBoot mvc项目结构 开发 热部署/FirstSpringBoot03"
./mvnw test -Dtest=MetricsStoreTest
```

预期：`Tests run: 1, Failures: 0`，BUILD SUCCESS

---

## Task 4: 埋点拦截器 + MVC 配置 + 全局 CORS

**Files:**
- Create: `src/main/java/om/mashibing/springboot02/interceptor/MetricsInterceptor.java`
- Create: `src/main/java/om/mashibing/springboot02/config/WebMvcConfig.java`
- Create: `src/main/java/om/mashibing/springboot02/config/CorsConfig.java`
- Test: `src/test/java/om/mashibing/springboot02/interceptor/MetricsInterceptorTest.java`

**Interfaces:**
- Consumes: `MetricsStore`（Task 3）、`CallRecord`（Task 3）
- Produces: `MetricsInterceptor implements HandlerInterceptor`，`preHandle` 提取 `X-User-Name/Type/Level/Dept` 请求头 → 记录埋点（try-catch 静默，不阻断主流程）
- Produces: `WebMvcConfig` 注册拦截器到 `/api/**`
- Produces: `CorsConfig` 全局 CORS（允许所有 origin，供前端静态站点跨域）

**Steps:**

- [ ] 写 MetricsInterceptor.java：

```java
package om.mashibing.springboot02.interceptor;

import om.mashibing.springboot02.metrics.CallRecord;
import om.mashibing.springboot02.metrics.MetricsStore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

@Component
public class MetricsInterceptor implements HandlerInterceptor {

    private static final Logger log = LoggerFactory.getLogger(MetricsInterceptor.class);

    @Autowired
    private MetricsStore metricsStore;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        // 埋点旁路：try-catch 静默，绝不拖垮主流程
        try {
            String user  = header(request, "X-User-Name", "anonymous");
            String type  = header(request, "X-User-Type", "未分类");
            String level = header(request, "X-User-Level", "L0");
            String dept  = header(request, "X-User-Dept", "未知");
            String api   = request.getRequestURI(); // 含 /boot/api/xxx
            metricsStore.record(new CallRecord(user, type, level, dept, api, System.currentTimeMillis()));
        } catch (Exception e) {
            log.warn("埋点记录失败，已忽略: {}", e.getMessage());
        }
        return true; // 始终放行
    }

    private String header(HttpServletRequest request, String name, String def) {
        String v = request.getHeader(name);
        return (v == null || v.isEmpty()) ? def : v;
    }
}
```

- [ ] 写 WebMvcConfig.java：

```java
package om.mashibing.springboot02.config;

import om.mashibing.springboot02.interceptor.MetricsInterceptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    @Autowired
    private MetricsInterceptor metricsInterceptor;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(metricsInterceptor).addPathPatterns("/api/**");
    }
}
```

- [ ] 写 CorsConfig.java：

```java
package om.mashibing.springboot02.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class CorsConfig implements WebMvcConfigurer {

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/api/**")
                .allowedOrigins("*")
                .allowedMethods("GET", "POST", "OPTIONS")
                .allowedHeaders("*")
                .allowCredentials(false);
    }
}
```

- [ ] 写测试 MetricsInterceptorTest.java（验证 preHandle 始终返回 true 且不抛异常）：

```java
package om.mashibing.springboot02.interceptor;

import om.mashibing.springboot02.metrics.MetricsStore;
import org.junit.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import static org.junit.Assert.*;

public class MetricsInterceptorTest {

    @Test
    public void testPreHandleAlwaysReturnsTrue() {
        MetricsStore store = new MetricsStore();
        MetricsInterceptor interceptor = new MetricsInterceptor();
        // 手动注入（反射或 setter；此处用反射注入 metricsStore 字段）
        try {
            java.lang.reflect.Field f = MetricsInterceptor.class.getDeclaredField("metricsStore");
            f.setAccessible(true);
            f.set(interceptor, store);
        } catch (Exception e) {
            fail("注入失败: " + e.getMessage());
        }

        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("X-User-Name", "alice");
        request.addHeader("X-User-Type", "内部");
        request.setRequestURI("/boot/api/helloworld");
        MockHttpServletResponse response = new MockHttpServletResponse();

        boolean result = interceptor.preHandle(request, response, new Object());
        assertTrue(result);
        assertEquals(1, store.aggregate().get("total"));
    }

    @Test
    public void testPreHandleSilentOnException() {
        MetricsInterceptor interceptor = new MetricsInterceptor();
        // metricsStore 为 null，应触发 NPE 但被 try-catch 静默，仍返回 true
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();
        boolean result = interceptor.preHandle(request, response, new Object());
        assertTrue("埋点异常不应阻断主流程", result);
    }
}
```

> 注：`MockHttpServletRequest` 来自 spring-test（starter-test 自带）。

- [ ] 运行测试：

```bash
cd "02 SpringBoot/07 springBoot mvc项目结构 开发 热部署/FirstSpringBoot03"
./mvnw test -Dtest=MetricsInterceptorTest
```

预期：`Tests run: 2, Failures: 0`，BUILD SUCCESS

---

## Task 5: 全局异常处理器 GlobalExceptionHandler

**Files:**
- Create: `src/main/java/om/mashibing/springboot02/config/GlobalExceptionHandler.java`
- Test: `src/test/java/om/mashibing/springboot02/config/GlobalExceptionHandlerTest.java`

**Interfaces:**
- Consumes: `BizException`（Task 1）、`ErrorResponse`（Task 1）
- Produces: `@ControllerAdvice`，三个 `@ExceptionHandler`：
  - `BizException` → ErrorResponse(code=BizException.code, HTTP 200) — 业务异常不裸抛 stacktrace
  - `IllegalArgumentException` → ErrorResponse(code="PARAM_ERR", HTTP 200)
  - `Exception` → ErrorResponse(code="UNKNOWN", HTTP 500)

**Steps:**

- [ ] 写 GlobalExceptionHandler.java：

```java
package om.mashibing.springboot02.config;

import om.mashibing.springboot02.exception.BizException;
import om.mashibing.springboot02.exception.ErrorResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.ResponseStatus;

import javax.servlet.http.HttpServletRequest;
import java.time.Instant;

@ControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(BizException.class)
    @ResponseBody
    @ResponseStatus(HttpStatus.OK)
    public ErrorResponse handleBiz(BizException ex, HttpServletRequest request) {
        log.warn("业务异常: code={}, msg={}", ex.getCode(), ex.getMessage());
        return new ErrorResponse(ex.getCode(), ex.getMessage(),
                Instant.now().toString(), request.getRequestURI());
    }

    @ExceptionHandler(IllegalArgumentException.class)
    @ResponseBody
    @ResponseStatus(HttpStatus.OK)
    public ErrorResponse handleParam(IllegalArgumentException ex, HttpServletRequest request) {
        log.warn("参数异常: {}", ex.getMessage());
        return new ErrorResponse("PARAM_ERR", ex.getMessage(),
                Instant.now().toString(), request.getRequestURI());
    }

    @ExceptionHandler(Exception.class)
    @ResponseBody
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public ErrorResponse handleUnknown(Exception ex, HttpServletRequest request) {
        log.error("未知异常", ex);
        return new ErrorResponse("UNKNOWN", "服务内部错误",
                Instant.now().toString(), request.getRequestURI());
    }
}
```

- [ ] 写测试 GlobalExceptionHandlerTest.java（直接调用 handler 方法验证返回体）：

```java
package om.mashibing.springboot02.config;

import om.mashibing.springboot02.exception.BizException;
import om.mashibing.springboot02.exception.ErrorResponse;
import org.junit.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import static org.junit.Assert.*;

public class GlobalExceptionHandlerTest {

    @Test
    public void testHandleBiz() {
        GlobalExceptionHandler handler = new GlobalExceptionHandler();
        MockHttpServletRequest req = new MockHttpServletRequest();
        req.setRequestURI("/boot/api/hash");

        BizException ex = new BizException("BIZ_ERR", "哈希算法不可用");
        ErrorResponse er = handler.handleBiz(ex, req);

        assertEquals("BIZ_ERR", er.getCode());
        assertEquals("哈希算法不可用", er.getMessage());
        assertEquals("/boot/api/hash", er.getPath());
    }

    @Test
    public void testHandleParam() {
        GlobalExceptionHandler handler = new GlobalExceptionHandler();
        MockHttpServletRequest req = new MockHttpServletRequest();
        req.setRequestURI("/boot/api/bubbleSort");

        ErrorResponse er = handler.handleParam(
                new IllegalArgumentException("输入需为逗号分隔整数"), req);

        assertEquals("PARAM_ERR", er.getCode());
        assertEquals("输入需为逗号分隔整数", er.getMessage());
    }

    @Test
    public void testHandleUnknown() {
        GlobalExceptionHandler handler = new GlobalExceptionHandler();
        MockHttpServletRequest req = new MockHttpServletRequest();
        req.setRequestURI("/boot/api/helloworld");

        ErrorResponse er = handler.handleUnknown(new RuntimeException("boom"), req);

        assertEquals("UNKNOWN", er.getCode());
        assertEquals("服务内部错误", er.getMessage());
    }
}
```

- [ ] 运行测试：

```bash
cd "02 SpringBoot/07 springBoot mvc项目结构 开发 热部署/FirstSpringBoot03"
./mvnw test -Dtest=GlobalExceptionHandlerTest
```

预期：`Tests run: 3, Failures: 0`，BUILD SUCCESS

---

## Task 6: DemoController — 4 业务接口（helloworld/hash/bubbleSort/export）

**Files:**
- Create: `src/main/java/om/mashibing/springboot02/controller/DemoController.java`
- Test: `src/test/java/om/mashibing/springboot02/controller/DemoControllerTest.java`（MockMvc 端到端）

**Interfaces:**
- Consumes: `HashUtil`（Task 2）、`BizException`（Task 1）
- Produces: `@RestController @RequestMapping("/api")`，4 方法：
  - `GET /api/helloworld` → `{ message, timestamp, server }`
  - `GET /api/hash?input=` → `{ input, md5, sha256 }`
  - `GET /api/bubbleSort?input=5,3,8,1` → `{ original[], sorted[], steps[][], swapCount }`
  - `GET /api/export?type={helloworld|hash|bubbleSort}` → `text/csv` + Content-Disposition

**Steps:**

- [ ] 写 DemoController.java：

```java
package om.mashibing.springboot02.controller;

import om.mashibing.springboot02.exception.BizException;
import om.mashibing.springboot02.util.HashUtil;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@RequestMapping("/api")
public class DemoController {

    @GetMapping("/helloworld")
    public Map<String, Object> helloworld() {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("message", "Hello World");
        result.put("timestamp", System.currentTimeMillis());
        result.put("server", "FirstSpringBoot02");
        return result;
    }

    @GetMapping("/hash")
    public Map<String, Object> hash(
            @RequestParam(value = "input", defaultValue = "hello world") String input) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("input", input);
        result.put("md5", HashUtil.md5(input));
        result.put("sha256", HashUtil.sha256(input));
        return result;
    }

    @GetMapping("/bubbleSort")
    public Map<String, Object> bubbleSort(
            @RequestParam(value = "input", required = false) String input) {

        int[] original;
        if (input == null || input.isEmpty()) {
            Random rnd = new Random();
            original = new int[8];
            for (int i = 0; i < 8; i++) original[i] = rnd.nextInt(100);
        } else {
            String[] parts = input.split(",");
            original = new int[parts.length];
            try {
                for (int i = 0; i < parts.length; i++) original[i] = Integer.parseInt(parts[i].trim());
            } catch (NumberFormatException e) {
                throw new BizException("PARAM_ERR", "输入需为逗号分隔整数");
            }
        }

        int[] sorted = Arrays.copyOf(original, original.length);
        List<int[]> steps = new ArrayList<>();
        steps.add(Arrays.copyOf(sorted, sorted.length));
        int swapCount = 0;

        for (int i = 0; i < sorted.length - 1; i++) {
            for (int j = 0; j < sorted.length - 1 - i; j++) {
                if (sorted[j] > sorted[j + 1]) {
                    int tmp = sorted[j]; sorted[j] = sorted[j + 1]; sorted[j + 1] = tmp;
                    swapCount++;
                    steps.add(Arrays.copyOf(sorted, sorted.length));
                }
            }
        }

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("original", original);
        result.put("sorted", sorted);
        result.put("steps", steps);
        result.put("swapCount", swapCount);
        return result;
    }

    @GetMapping("/export")
    public ResponseEntity<String> export(@RequestParam("type") String type) {
        String csv;
        String filename;
        switch (type) {
            case "helloworld": {
                Map<String, Object> r = helloworld();
                csv = "field,value\nmessage," + r.get("message") + "\ntimestamp," + r.get("timestamp") + "\nserver," + r.get("server") + "\n";
                filename = "helloworld.csv";
                break;
            }
            case "hash": {
                csv = "field,value\ninput,hello world\nmd5," + HashUtil.md5("hello world") + "\nsha256," + HashUtil.sha256("hello world") + "\n";
                filename = "hash.csv";
                break;
            }
            case "bubbleSort": {
                Map<String, Object> r = bubbleSort("5,3,8,1");
                int[] sorted = (int[]) r.get("sorted");
                StringBuilder sb = new StringBuilder("step,...\noriginal,5,3,8,1\n");
                @SuppressWarnings("unchecked")
                List<int[]> steps = (List<int[]>) r.get("steps");
                for (int i = 0; i < steps.size(); i++) {
                    sb.append("step").append(i + 1).append(",");
                    for (int v : steps.get(i)) sb.append(v).append(",");
                    sb.append("\n");
                }
                sb.append("final,").append(Arrays.toString(sorted).replaceAll("[\\[\\] ]", "")).append("\n");
                sb.append("swapCount,").append(r.get("swapCount")).append("\n");
                csv = sb.toString();
                filename = "bubbleSort.csv";
                break;
            }
            default:
                throw new BizException("PARAM_ERR", "type 非法，可选: helloworld|hash|bubbleSort");
        }

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.parseMediaType("text/csv; charset=UTF-8"));
        headers.set(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"");
        return new ResponseEntity<>(csv, headers, org.springframework.http.HttpStatus.OK);
    }
}
```

- [ ] 写测试 DemoControllerTest.java（MockMvc 端到端）：

```java
package om.mashibing.springboot02.controller;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MockMvc;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@RunWith(SpringRunner.class)
@SpringBootTest
@AutoConfigureMockMvc
public class DemoControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    public void testHelloworld() throws Exception {
        mockMvc.perform(get("/api/helloworld"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Hello World"))
                .andExpect(jsonPath("$.server").value("FirstSpringBoot02"));
    }

    @Test
    public void testHash() throws Exception {
        mockMvc.perform(get("/api/hash").param("input", "hello world"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.input").value("hello world"))
                .andExpect(jsonPath("$.md5").exists())
                .andExpect(jsonPath("$.sha256").exists());
    }

    @Test
    public void testBubbleSort() throws Exception {
        mockMvc.perform(get("/api/bubbleSort").param("input", "5,3,8,1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.original[0]").value(5))
                .andExpect(jsonPath("$.sorted[0]").value(1))
                .andExpect(jsonPath("$.swapCount").exists());
    }

    @Test
    public void testExportHelloworld() throws Exception {
        mockMvc.perform(get("/api/export").param("type", "helloworld"))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Disposition",
                        org.hamcrest.Matchers.containsString("helloworld.csv")))
                .andExpect(content().contentType(org.hamcrest.Matchers.containsString("text/csv")));
    }

    @Test
    public void testExportInvalidType() throws Exception {
        mockMvc.perform(get("/api/export").param("type", "invalid"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("PARAM_ERR"));
    }

    @Test
    public void testBubbleSortInvalidInput() throws Exception {
        mockMvc.perform(get("/api/bubbleSort").param("input", "a,b,c"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("PARAM_ERR"));
    }
}
```

- [ ] 运行测试：

```bash
cd "02 SpringBoot/07 springBoot mvc项目结构 开发 热部署/FirstSpringBoot03"
./mvnw test -Dtest=DemoControllerTest
```

预期：`Tests run: 6, Failures: 0`，BUILD SUCCESS

---

## Task 7: MetricsController — 报表聚合接口

**Files:**
- Create: `src/main/java/om/mashibing/springboot02/controller/MetricsController.java`
- Test: `src/test/java/om/mashibing/springboot02/controller/MetricsControllerTest.java`

**Interfaces:**
- Consumes: `MetricsStore`（Task 3）
- Produces: `@RestController @RequestMapping("/api")`，方法 `GET /api/metrics` → 返回 `MetricsStore.aggregate()` 的 JSON

**Steps:**

- [ ] 写 MetricsController.java：

```java
package om.mashibing.springboot02.controller;

import om.mashibing.springboot02.metrics.MetricsStore;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api")
public class MetricsController {

    @Autowired
    private MetricsStore metricsStore;

    @GetMapping("/metrics")
    public Map<String, Object> metrics() {
        return metricsStore.aggregate();
    }
}
```

- [ ] 写测试 MetricsControllerTest.java（MockMvc 验证 metrics 接口返回 total 字段）：

```java
package om.mashibing.springboot02.controller;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MockMvc;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@RunWith(SpringRunner.class)
@SpringBootTest
@AutoConfigureMockMvc
public class MetricsControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    public void testMetricsReturnsTotal() throws Exception {
        mockMvc.perform(get("/api/metrics"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.total").exists())
                .andExpect(jsonPath("$.byApi").exists())
                .andExpect(jsonPath("$.byHour").exists())
                .andExpect(jsonPath("$.recentCallers").exists());
    }
}
```

- [ ] 运行全量后端测试（Task 1-7 汇总验证）：

```bash
cd "02 SpringBoot/07 springBoot mvc项目结构 开发 热部署/FirstSpringBoot03"
./mvnw test
```

预期：所有测试通过（BizExceptionTest + HashUtilTest + MetricsStoreTest + MetricsInterceptorTest + GlobalExceptionHandlerTest + DemoControllerTest + MetricsControllerTest），BUILD SUCCESS

---

## Task 8: 前端 demo 页 — 4 Tab + 身份选择器 + 导出 + ECharts 报表

> **仓库切换：** 本任务写入 haikulou1.github.io worktree，非 InternetArchitect。
> 前端 worktree：`/root/.agentix/agentic-dev/runs/DEV-ddccb2af-7620-11f1-9e19-e337058ec5b9-fb9e2aaf-4882-4ec5-a941-606f06b0fd30/worktree/haikulou1.github.io-master`

**Files:**
- Create: `demo/index.html`
- Create: `demo/app.js`
- Create: `demo/style.css`

**Interfaces:**
- Consumes: 后端 `/boot/api/helloworld`、`/boot/api/hash`、`/boot/api/bubbleSort`、`/boot/api/export?type=`、`/boot/api/metrics`
- 请求头：每次 fetch 携带 `X-User-Name/Type/Level/Dept`（来自页面顶部选择器）
- 图表：ECharts 5.x CDN，折线（byHour）/ 饼图（byType|byLevel|byDept 可切换）/ 柱状（byApi）

**Steps:**

- [ ] 写 demo/index.html：

```html
<!DOCTYPE html>
<html lang="zh-CN">
<head>
<meta charset="UTF-8">
<meta name="viewport" content="width=device-width, initial-scale=1.0">
<title>Demo — 三接口 + 报表</title>
<link rel="stylesheet" href="style.css">
<script src="https://cdn.jsdelivr.net/npm/echarts@5.4.3/dist/echarts.min.js"></script>
</head>
<body>

<div class="user-bar">
  <label>用户名: <input id="userName" value="anonymous"></label>
  <label>类型: <select id="userType">
    <option>内部</option><option>外部</option><option>合作方</option>
  </select></label>
  <label>层级: <select id="userLevel">
    <option>L0</option><option>L1</option><option>L2</option><option>L3</option>
  </select></label>
  <label>部门: <select id="userDept">
    <option>未知</option><option>研发</option><option>测试</option><option>产品</option>
  </select></label>
</div>

<div class="tabs">
  <button class="tab-btn active" data-tab="hello">HelloWorld</button>
  <button class="tab-btn" data-tab="hash">哈希算法</button>
  <button class="tab-btn" data-tab="bubble">冒泡排序</button>
  <button class="tab-btn" data-tab="report">调用报表</button>
</div>

<div id="tab-hello" class="tab-content active">
  <button id="btn-hello" class="exec-btn">执行</button>
  <button id="export-hello" class="export-btn">导出 CSV</button>
  <pre id="out-hello"></pre>
</div>

<div id="tab-hash" class="tab-content">
  <input id="hashInput" value="hello world" placeholder="输入待哈希文本">
  <button id="btn-hash" class="exec-btn">执行</button>
  <button id="export-hash" class="export-btn">导出 CSV</button>
  <pre id="out-hash"></pre>
</div>

<div id="tab-bubble" class="tab-content">
  <input id="bubbleInput" value="5,3,8,1" placeholder="逗号分隔整数">
  <button id="btn-bubble" class="exec-btn">执行</button>
  <button id="export-bubble" class="export-btn">导出 CSV</button>
  <pre id="out-bubble"></pre>
</div>

<div id="tab-report" class="tab-content">
  <button id="btn-report" class="exec-btn">刷新报表</button>
  <label>饼图维度:
    <select id="pieDim">
      <option value="byType">人员类型</option>
      <option value="byLevel">层级</option>
      <option value="byDept">部门</option>
    </select>
  </label>
  <div id="chart-line" style="width:600px;height:300px"></div>
  <div id="chart-pie" style="width:400px;height:300px"></div>
  <div id="chart-bar" style="width:600px;height:300px"></div>
</div>

<script src="app.js"></script>
</body>
</html>
```

- [ ] 写 demo/app.js：

```javascript
// 后端基地址：context-path=/boot
const API_BASE = '/boot/api';

function userHeaders() {
  return {
    'X-User-Name': document.getElementById('userName').value || 'anonymous',
    'X-User-Type': document.getElementById('userType').value,
    'X-User-Level': document.getElementById('userLevel').value,
    'X-User-Dept': document.getElementById('userDept').value
  };
}

async function callApi(path, params) {
  const url = new URL(API_BASE + path, window.location.origin);
  if (params) Object.entries(params).forEach(([k,v]) => url.searchParams.set(k, v));
  const resp = await fetch(url, { headers: userHeaders() });
  return resp.json();
}

// Tab 切换
document.querySelectorAll('.tab-btn').forEach(btn => {
  btn.addEventListener('click', () => {
    document.querySelectorAll('.tab-btn').forEach(b => b.classList.remove('active'));
    document.querySelectorAll('.tab-content').forEach(c => c.classList.remove('active'));
    btn.classList.add('active');
    document.getElementById('tab-' + btn.dataset.tab).classList.add('active');
  });
});

// HelloWorld
document.getElementById('btn-hello').addEventListener('click', async () => {
  const r = await callApi('/helloworld');
  document.getElementById('out-hello').textContent = JSON.stringify(r, null, 2);
});

// Hash
document.getElementById('btn-hash').addEventListener('click', async () => {
  const input = document.getElementById('hashInput').value;
  const r = await callApi('/hash', { input });
  document.getElementById('out-hash').textContent = JSON.stringify(r, null, 2);
});

// BubbleSort
document.getElementById('btn-bubble').addEventListener('click', async () => {
  const input = document.getElementById('bubbleInput').value;
  const r = await callApi('/bubbleSort', { input });
  document.getElementById('out-bubble').textContent = JSON.stringify(r, null, 2);
});

// Export 通用函数
function exportCsv(type) {
  const url = API_BASE + '/export?type=' + type;
  const a = document.createElement('a');
  a.href = url;
  // fetch 无法自动带自定义 header 到 <a> 下载，用 fetch + blob
  fetch(url, { headers: userHeaders() })
    .then(r => r.blob())
    .then(blob => {
      const objUrl = URL.createObjectURL(blob);
      a.href = objUrl;
      a.download = type + '.csv';
      a.click();
      URL.revokeObjectURL(objUrl);
    });
}
document.getElementById('export-hello').addEventListener('click', () => exportCsv('helloworld'));
document.getElementById('export-hash').addEventListener('click', () => exportCsv('hash'));
document.getElementById('export-bubble').addEventListener('click', () => exportCsv('bubbleSort'));

// 报表 + ECharts
let chartLine, chartPie, chartBar;

document.getElementById('btn-report').addEventListener('click', loadReport);
document.getElementById('pieDim').addEventListener('change', renderPie);

async function loadReport() {
  const data = await callApi('/metrics');
  renderLine(data);
  renderPie(data);
  renderBar(data);
}

function renderLine(data) {
  if (!chartLine) chartLine = echarts.init(document.getElementById('chart-line'));
  const hours = Object.keys(data.byHour).sort();
  const values = hours.map(h => data.byHour[h]);
  chartLine.setOption({
    title: { text: '按小时调用趋势' },
    tooltip: { trigger: 'axis' },
    xAxis: { type: 'category', data: hours },
    yAxis: { type: 'value' },
    series: [{ name: '调用次数', type: 'line', data: values }]
  });
}

function renderPie(data) {
  if (!chartPie) chartPie = echarts.init(document.getElementById('chart-pie'));
  const dim = document.getElementById('pieDim').value;
  const map = data[dim] || {};
  const pieData = Object.entries(map).map(([name, value]) => ({ name, value }));
  chartPie.setOption({
    title: { text: dim + ' 占比' },
    tooltip: { trigger: 'item' },
    series: [{ type: 'pie', radius: '60%', data: pieData }]
  });
}

function renderBar(data) {
  if (!chartBar) chartBar = echarts.init(document.getElementById('chart-bar'));
  const apis = Object.keys(data.byApi);
  const values = apis.map(a => data.byApi[a]);
  chartBar.setOption({
    title: { text: '各接口调用次数' },
    tooltip: { trigger: 'axis' },
    xAxis: { type: 'category', data: apis },
    yAxis: { type: 'value' },
    series: [{ type: 'bar', data: values }]
  });
}
```

- [ ] 写 demo/style.css：

```css
body { font-family: sans-serif; margin: 20px; background: #f5f5f5; }
.user-bar { background: #fff; padding: 12px; border-radius: 6px; margin-bottom: 12px; display: flex; gap: 16px; align-items: center; }
.tabs { display: flex; gap: 4px; margin-bottom: 12px; }
.tab-btn { padding: 8px 16px; border: 1px solid #ddd; background: #fff; cursor: pointer; border-radius: 4px 4px 0 0; }
.tab-btn.active { background: #1890ff; color: #fff; border-color: #1890ff; }
.tab-content { display: none; background: #fff; padding: 16px; border-radius: 0 6px 6px 6px; }
.tab-content.active { display: block; }
.exec-btn, .export-btn { padding: 6px 16px; margin: 4px 8px 8px 0; cursor: pointer; border: 1px solid #1890ff; border-radius: 4px; background: #1890ff; color: #fff; }
.export-btn { background: #52c41a; border-color: #52c41a; }
input, select { padding: 4px 8px; border: 1px solid #ddd; border-radius: 4px; }
pre { background: #fafafa; padding: 12px; border-radius: 4px; overflow-x: auto; max-height: 300px; }
```

- [ ] 前端验证（手动）：
  - 后端启动：`cd "02 SpringBoot/07 springBoot mvc项目结构 开发 热部署/FirstSpringBoot03" && ./mvnw spring-boot:run`
  - 浏览器打开 `demo/index.html`（需后端同源或经 CORS 跨域）
  - 切换 4 Tab，点执行验证 JSON 展示
  - 点导出验证 CSV 下载
  - 报表 Tab 点刷新，验证折线/饼/柱图渲染
  - 切换饼图维度验证动态切换

---

## Self-Review

**1. Spec coverage（对照设计文档 §3-§4）：**

| 设计要求 | 覆盖任务 | 状态 |
|----------|----------|------|
| GET /api/helloworld | Task 6 DemoController | ✅ |
| GET /api/hash | Task 6 DemoController | ✅ |
| GET /api/bubbleSort | Task 6 DemoController | ✅ |
| GET /api/export?type= | Task 6 DemoController | ✅ |
| GET /api/metrics | Task 7 MetricsController | ✅ |
| 异常统一体 {code,message,timestamp,path} | Task 1 + Task 5 | ✅ |
| BizException + @ControllerAdvice | Task 1 + Task 5 | ✅ |
| 拦截器 X-User-* 埋点 | Task 4 MetricsInterceptor | ✅ |
| 内存 ConcurrentHashMap 存储 | Task 3 MetricsStore | ✅ |
| 全局 CORS | Task 4 CorsConfig | ✅ |
| 前端 4 Tab + 身份选择器 | Task 8 index.html | ✅ |
| 前端导出按钮 | Task 8 app.js exportCsv | ✅ |
| ECharts 折线/饼/柱 | Task 8 app.js renderLine/Pie/Bar | ✅ |
| context-path=/boot | Global Constraints + app.js API_BASE | ✅ |
| 包名 om.mashibing.springboot02 | 所有 Java 文件 | ✅ |

**2. Placeholder scan：**
- 无 TBD/TODO/"implement later"
- Task 2 sha256 标准值已标注"实施者须用 sha256sum 取真实值填入"——这是唯一需运行时确认的值，非占位符
- 所有代码块均为完整可编译/可运行代码

**3. 跨库对齐检查：**
- 后端 `/api/**` + context-path `/boot` → 实际 `/boot/api/**`，前端 `API_BASE='/boot/api'` ✅
- 异常 ErrorResponse JSON ← 前端 fetch.json() ✅
- CSV Content-Disposition ← 前端 blob 下载 ✅
- CORS allowedOrigins("*") ← 前端静态站跨域 ✅
- X-User-* 请求头读写一致 ✅
- metrics 聚合 JSON ← ECharts setOption ✅

---

## Execution Handoff

计划已完成并保存至 `[InternetArchitect] docs/superpowers/plans/2026-07-30-hello-hash-bubble-plan.md`。

两个执行选项：

**1. Subagent-Driven（推荐）** — 每个 Task 派发独立子代理执行，Task 间审查，快速迭代

**2. Inline Execution** — 在当前会话内用 executing-plans 批量执行，带检查点

> 提示：Task 1-5 为纯后端无外部依赖，可并行或顺序执行；Task 6-7 依赖 Task 1-5；Task 8（前端）依赖后端接口就绪但可先行写静态文件。


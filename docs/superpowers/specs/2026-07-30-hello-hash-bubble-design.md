# 设计文档 — 三接口 + 三Tab + 导出 + 异常兜底 + 埋点报表

> 阶段：需求澄清/设计（brainstorming）｜产物仓库：InternetArchitect｜日期：2026-07-30  
> 状态：设计稿，未触碰代码。后续编码阶段依此执行。

## 1. 通览

### 需求全貌
1. Java 三接口：helloworld / 哈希算法 / 冒泡排序
2. 前端页面：三 Tab 展示各接口结果
3. 导出按钮 + 后台导出接口：导出各页面结果
4. **异常兜底**：统一异常处理，保证接口失败不裸抛
5. **后端埋点**：记录调用次数 + 调用人（含人员类型/层级/部门维度）
6. **前端报表**：页面内可视化报表，按人员类型/层级/部门维度，折线/饼/柱三种图

### 跨仓落点（已验证）

| 仓库 | 角色 | 关键事实 |
|------|------|----------|
| InternetArchitect | 后端 | `02 SpringBoot/07 springBoot mvc项目结构 开发 热部署/FirstSpringBoot03/`：SpringBoot 2.1.5.RELEASE / Java1.8 / web+thymeleaf+devtools；包名 `om.mashibing.springboot02`（源码 typo `om`，沿用）；pom **无DB依赖**；有 application.properties |
| haikulou1.github.io | 前端 | 静态 Hexo/NexT 站点，无 package.json/无构建链；lib 仅 anime/font-awesome/velocity，**无图表库** → 图表用 CDN ECharts |

leecode / naiss 与本需求无关，不改。

## 2. 需求澄清决策（自主裁定）

| 编号 | 模糊点 | 决策 | 依据 |
|------|--------|------|------|
| Q1 | 哈希算法 | MD5+SHA-256（JDK `MessageDigest`） | 零依赖，双算法展示直观 |
| Q2 | 哈希输入 | query `input`，缺省 `"hello world"` | 兼顾演示与交互 |
| Q3 | 冒泡输入 | query `input` 逗号分隔整数，缺省随机8个 | 可演示可交互 |
| Q4 | 冒泡返回 | `original/sorted/steps[]/swapCount` | 展示排序轨迹 |
| Q5 | 导出格式 | CSV（纯文本，零依赖，无需POI） | 通用最简 |
| Q6 | 前端技术 | 原生 HTML+JS（站点无构建链） | 约束决定 |
| Q7 | 前端页面位置 | `demo/index.html`（新增目录） | 不污染主站 |
| Q8 | 图表库 | ECharts（CDN 引入） | 功能全，静态站点友好，折线/饼/柱全覆盖 |
| Q9 | 埋点存储 | **内存 ConcurrentHashMap**（pom 无DB依赖，不引入DB） | 零额外依赖，重启清空可接受（demo 性质） |
| Q10 | 调用人来源 | 请求头 `X-User-*` 系列，缺省 anonymous | 无鉴权体系，前端 demo 页提供身份选择器 |
| Q11 | 人员维度数据来源 | 前端请求头携带 `X-User-Type/Level/Dept` | 维度由前端选择器填，后端只记录聚合 |
| Q12 | 报表位置 | demo 页内第 4 Tab「调用报表」 | 与三业务Tab同页，统一入口 |
| Q13 | 报表数据接口 | `GET /api/metrics` 返回多维度聚合 JSON | 单接口返全量聚合，前端 ECharts 渲染 |
| Q14 | 异常兜底 | `@ControllerAdvice` 全局 + `BizException` + 统一错误体 | 覆盖参数/业务/未知异常 |
| Q15 | 埋点异常 | 埋点 try-catch 静默，不影响主流程 | 埋点是旁路，不可拖垮业务 |

## 3. 接口契约（跨库对齐基准）

### 3.1 业务接口（4个）

```
GET /api/helloworld
  → 200 { message, timestamp, server }

GET /api/hash?input=hello world
  → 200 { input, md5, sha256 }

GET /api/bubbleSort?input=5,3,8,1   （缺省随机8个）
  → 200 { original[], sorted[], steps[][], swapCount }

GET /api/export?type={helloworld|hash|bubbleSort}
  → 200 text/csv; attachment; filename="<type>.csv"
```

### 3.2 埋点与报表接口（2个）

```
GET /api/metrics
  → 200 {
      total,                      // 总调用次数
      byApi:    { helloworld:N, hash:N, bubbleSort:N, export:N },
      byType:   { "内部":N, "外部":N, ... },        // 人员类型
      byLevel:  { "L1":N, "L2":N, ... },            // 人员层级
      byDept:   { "研发":N, "测试":N, ... },         // 人员部门
      byHour:   { "00":N, ..., "23":N },           // 按小时趋势(折线)
      recentCallers: [ {user,type,level,dept,api,time}, ... ]  // 最近N条
    }
```

**调用人识别**：每次请求经拦截器从请求头提取 `X-User-Name/Type/Level/Dept`，缺省 `anonymous/未分类/L0/未知`，记录到内存。

### 3.3 异常统一体

```
所有异常 → 200(或500) { code, message, timestamp, path }
  code: BIZ_ERR / PARAM_ERR / NOT_FOUND / UNKNOWN
```

## 4. 改动计划（编码阶段执行）

### 4.1 后端 InternetArchitect / FirstSpringBoot03

| 文件（相对 worktree） | 职责 |
|----------------------|------|
| `.../controller/DemoController.java` | 4 业务接口：helloworld/hash/bubbleSort/export |
| `.../controller/MetricsController.java` | `GET /api/metrics` 报表聚合 |
| `.../config/CorsConfig.java` | 全局 CORS（前端静态站点跨域兜底） |
| `.../config/GlobalExceptionHandler.java` | `@ControllerAdvice` 统一异常兜底 |
| `.../interceptor/MetricsInterceptor.java` | 拦截 `/api/**`，提取 X-User-* 记录埋点（try-catch 静默） |
| `.../metrics/CallRecord.java` | 埋点记录实体（user/type/level/dept/api/time） |
| `.../metrics/MetricsStore.java` | 内存存储 `ConcurrentHashMap` + 聚合查询 |
| `.../util/HashUtil.java` | MD5/SHA-256 封装 |
| `.../exception/BizException.java` | 业务异常 |
| `.../config/WebMvcConfig.java` | 注册拦截器 |

### 4.2 前端 haikulou1.github.io

| 文件（相对 worktree） | 职责 |
|----------------------|------|
| `demo/index.html` | 4 Tab 页：HelloWorld/哈希/冒泡/调用报表 + 顶部身份选择器 + 各Tab导出按钮 |
| `demo/app.js` | fetch 调用 + Tab切换 + 导出触发 + ECharts 渲染（折线/饼/柱） |
| `demo/style.css` | Tab + 报表布局样式 |

**前端身份选择器**：页面顶部下拉选择 人员类型/层级/部门/用户名，每次请求注入 `X-User-*` 请求头，供埋点采集。

**报表 Tab 可视化**：
- 折线图：`byHour` 调用趋势
- 饼图：`byType` / `byLevel` / `byDept` 占比（可切换维度）
- 柱状图：`byApi` 各接口调用次数

### 4.3 导出 CSV 格式

| type | 表头 + 行 |
|------|-----------|
| helloworld | `field,value` / message,timestamp,server |
| hash | `field,value` / input,md5,sha256 |
| bubbleSort | `step,...` / original,step1,...,final,swapCount |

## 5. 异常兜底方案（专项）

| 异常场景 | 处理 | code |
|----------|------|------|
| 哈希 `NoSuchAlgorithmException` | 理论不发生（MD5/SHA-256 标准算法），仍 catch 转 BizException | BIZ_ERR |
| 冒泡输入非数字 | 校验 → BizException("输入需为逗号分隔整数") | PARAM_ERR |
| 导出 type 非法 | 校验枚举 → BizException("type 非法") | PARAM_ERR |
| 埋点自身异常 | 拦截器内 try-catch 静默，记录日志，不抛出 | — |
| 未捕获异常 | `@ExceptionHandler(Exception.class)` 兜底 | UNKNOWN |

**设计原则**：业务接口绝不裸抛 stacktrace 给前端；埋点作为旁路，故障隔离不影响主流程。

## 6. 跨库对齐点

| 对齐点 | 后端 | 前端 | 兼容策略 |
|--------|------|------|----------|
| 数据格式 | JSON 驼峰 | fetch+JSON.parse | 仅新增字段不改名 |
| 导出 | CSV 流 + Content-Disposition | `<a download>`/location 触发 | 浏览器原生下载 |
| 跨域 | 全局 CORS | fetch 携带 origin | CORS 兜底 |
| 埋点身份 | 读 X-User-* 请求头 | 选择器注入请求头 | 缺省 anonymous，向后兼容 |
| 图表数据 | `/api/metrics` 聚合JSON | ECharts setOption | 聚合在后端，前端只渲染 |

## 7. 风险与缓解

| 风险 | 缓解 |
|------|------|
| 包名 typo `om` | 严格沿用 `om/mashibing/springboot02/...` 物理路径 |
| 内存埋点重启丢失 | demo 性质可接受；预留 `MetricsStore` 接口，后续可换 DB 实现 |
| ECharts CDN 不可用 | 预留本地 fallback 备注；或图表区显示"加载失败"兜底 |
| 跨域 | 后端全局 CORS 兜底 |
| 埋点拖垮业务 | 拦截器 try-catch 静默，故障隔离 |

## 8. 验证策略（编码阶段）

- 后端：`mvnw test`；启动后 `curl` 六接口（4业务+metrics）验 JSON/CSV
- 前端：浏览器开 `demo/index.html`，切四 Tab + 查图表渲染 + 点导出验下载
- 降级：Maven 编译 ≥2次失败 或 >120s → 转静态审查契约类型匹配

## 9. 设计产物清点

| 产物 | 逻辑路径 | 仓库 | 状态 |
|------|---------|------|------|
| 本设计文档 | `[InternetArchitect] docs/superpowers/specs/2026-07-30-hello-hash-bubble-design.md` | InternetArchitect | ✅ 已落盘 |

> 本阶段为需求澄清/设计，**未修改任何代码文件**。后续编码阶段依 §4 执行。

## 附录：brainstorming 自检
- [x] 占位符：无 TBD/TODO
- [x] 内部一致：§3 契约与 §4 改动一致
- [x] 范围：单一计划可覆盖（6后端接口 + 1前端4Tab页）
- [x] 歧义：Q1-Q15 逐项消歧

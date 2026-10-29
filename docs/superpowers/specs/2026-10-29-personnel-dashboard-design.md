# 人员看板（Personnel Dashboard）设计文档

日期：2026-10-29
状态：已澄清并获用户批准（待书面审阅）

## 1. 背景与目标

开发一个人员看板系统，用于：

- 记录员工基本信息，支持增删改查（CRUD）。
- 支持批量导入员工信息。
- 记录成本预算，成本科目受「白名单」约束（成本科目/预算项目白名单），白名单本身可维护、可导入。
- 提供成本预算看板：预算额度 vs 实际支出 vs 预留/占用的对比。

## 2. 范围

### 2.1 In Scope

- 员工基本信息 CRUD + 分页/条件查询 + 详情。
- 成本科目（预算项目）白名单维护（管理员）。
- 员工 × 成本科目 的预算额度记录，科目必须命中白名单。
- 实际支出与预留/占用记录（员工 × 科目维度），支撑「预算 vs 实际 vs 预留」看板。
- 批量导入：Excel(.xlsx) 与 CSV 双格式，覆盖员工、预算、白名单三类数据，提供模板下载。
- 登录 + 简单角色（管理员 / 普通用户）。

### 2.2 Out of Scope

- 复杂审批流、多级组织架构树、薪资核算。
- 与外部 HR/OA 系统的对接集成。
- 移动端适配（桌面优先，但页面自适应基础布局）。

## 3. 技术基线（已确认）

| 项 | 选型 |
| --- | --- |
| 框架 | Spring Boot 3.x（Jakarta EE） |
| JDK | Java 17 |
| 数据库 | MySQL 8.x |
| 模板引擎 | Thymeleaf |
| 持久层 | MyBatis（mybatis-spring-boot-starter 3.0.x）+ PageHelper |
| 安全 | Spring Security 6（会话登录 + 简单角色） |
| Excel 解析 | Apache POI（.xlsx） |
| CSV 解析 | Apache Commons CSV |
| 构建 | Maven |

工程形态：全新单体应用，前后端不分离（Thymeleaf 服务端渲染），页面风格沿用仓库内 OA 工程（Bootstrap 风格）。

## 4. 总体架构

分层：

```
Controller（Thymeleaf 页面 + 表单提交）
  → Service（业务规则：白名单校验、预算对比、导入校验）
    → Mapper（MyBatis）
      → MySQL 8
```

- Controller：负责页面渲染与表单绑定，不做业务判断。
- Service：承载全部业务规则，包括导入的整表校验与事务边界。
- Mapper：单表/多表查询。
- 导入解析器（ImportParser）：将 Excel/CSV 行解析为 DTO，逐行校验，汇总错误。

模块划分（按包）：

```
com.company.personnel
├── config        # Security、Web、事务配置
├── controller    # 页面控制器
├── service       # 业务服务与导入服务
├── mapper        # MyBatis Mapper 接口 + XML
├── domain        # 实体
├── dto           # 传输对象（含导入行 DTO、回执 DTO）
├── importers     # Excel/CSV 解析与校验
└── common        # 异常、分页结果、常量
```

## 5. 数据模型

### 5.1 employee（员工）

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| id | BIGINT PK | 自增 |
| emp_no | VARCHAR(32) UK | 员工编号/工号 |
| name | VARCHAR(64) | 姓名 |
| gender | VARCHAR(8) | 性别 |
| birth_date | DATE | 出生日期 |
| phone | VARCHAR(20) | 手机号 |
| email | VARCHAR(128) | 邮箱 |
| department | VARCHAR(64) | 部门 |
| position | VARCHAR(64) | 岗位 |
| rank | VARCHAR(32) | 职级 |
| superior | VARCHAR(64) | 直属上级 |
| hire_date | DATE | 入职日期 |
| emp_status | VARCHAR(16) | 在职/离职 |
| employment_type | VARCHAR(16) | 用工类型 |
| cost_center | VARCHAR(64) | 成本中心 |
| created_at / updated_at | DATETIME | 审计时间戳 |

### 5.2 budget_project（成本科目 / 预算项目白名单）

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| id | BIGINT PK | 自增 |
| project_code | VARCHAR(32) UK | 科目编码 |
| project_name | VARCHAR(128) | 科目名称 |
| category | VARCHAR(32) | 科目类型/分类 |
| status | VARCHAR(16) | 启用/停用（默认启用） |
| created_at / updated_at | DATETIME | 审计时间戳 |

- 仅管理员可增删改。
- 支持从 Excel/CSV 导入维护。
- 预算记录插入时强校验：科目必须存在且启用（白名单命中）。

### 5.3 employee_budget（员工 × 科目 预算额度）

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| id | BIGINT PK | 自增 |
| employee_id | BIGINT FK | 员工 |
| project_id | BIGINT FK | 成本科目（白名单） |
| period | VARCHAR(16) | 期间（如 2026-01） |
| budget_amount | DECIMAL(12,2) | 预算额度 |
| created_at / updated_at | DATETIME | 审计时间戳 |

唯一约束：(employee_id, project_id, period)。

### 5.4 budget_actual（实际支出）

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| id | BIGINT PK | 自增 |
| employee_id | BIGINT FK | 员工 |
| project_id | BIGINT FK | 成本科目 |
| period | VARCHAR(16) | 期间 |
| actual_amount | DECIMAL(12,2) | 实际支出金额 |
| remark | VARCHAR(255) | 备注 |
| created_at / updated_at | DATETIME | 审计时间戳 |

### 5.5 budget_reserve（预留 / 占用）

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| id | BIGINT PK | 自增 |
| employee_id | BIGINT FK | 员工 |
| project_id | BIGINT FK | 成本科目 |
| period | VARCHAR(16) | 期间 |
| reserve_amount | DECIMAL(12,2) | 预留金额 |
| remark | VARCHAR(255) | 备注 |
| created_at / updated_at | DATETIME | 审计时间戳 |

### 5.6 sys_user（登录用户，简单角色）

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| id | BIGINT PK | 自增 |
| employee_id | BIGINT FK nullable | 关联员工（普通用户必填） |
| username | VARCHAR(64) UK | 登录名 |
| password | VARCHAR(128) | BCrypt 加密 |
| role | VARCHAR(16) | ADMIN / USER |
| status | VARCHAR(16) | 启用/停用 |
| created_at / updated_at | DATETIME | 审计时间戳 |

简单角色用单字段枚举，不做独立 role 表（YAGNI）。

## 6. 功能模块与页面

### 6.1 登录
- /login：用户名密码 + Spring Security 会话；失败提示、超时跳转。

### 6.2 员工管理（CRUD）
- 列表：分页 + 条件查询（姓名/工号/部门/状态）。
- 新增/编辑表单、删除（软性确认，离职员工保留记录）。
- 详情页。

### 6.3 看板（Dashboard）
- 员工信息浏览 + 成本预算汇总。
- 按 员工×科目 展示：预算额度、实际支出、预留、剩余（预算 − 实际 − 预留）。
- 支持按部门 / 科目 / 期间聚合。

### 6.4 预算记录
- 新增/编辑 员工×科目 预算额度（科目下拉仅展示白名单内启用科目）。
- 维护实际支出与预留记录。
- 预算重复录入（同员工+科目+期间）提示冲突。

### 6.5 白名单维护（管理员）
- 科目 CRUD、启停用。
- 支持 Excel/CSV 导入。

### 6.6 批量导入（员工 / 预算 / 白名单）
- 每个入口提供「模板下载」。
- 上传 Excel(.xlsx) 或 CSV，自动识别格式。
- 导入策略（已确认）：**全对才导入（整批回滚）**——任一数据行校验失败，整批不导入，返回全部错误行号与原因；全部合法才一次性写入（单事务）。

### 6.7 权限矩阵（已确认）

| 操作 | 管理员 | 普通用户 |
| --- | --- | --- |
| 员工增删改 | ✅ | 仅本人基本信息可维护 |
| 员工/看板浏览 | ✅ | ✅（只读） |
| 预算录入/维护 | ✅ | 只读 |
| 白名单维护/导入 | ✅ | 只读 |
| 导入员工/预算/白名单 | ✅ | ❌ |

## 7. 批量导入设计

### 7.1 格式与模板
- Excel：.xlsx（Apache POI），首个 sheet 为数据，表头固定。
- CSV：UTF-8，逗号分隔，首行表头。
- 模板文件内置在 resources 中，下载时按类型返回。

### 7.2 校验规则（逐行，整表先校验后写入）
- 必填：工号、姓名、科目编码/名称等按类型必填字段。
- 格式：手机号、邮箱、金额（非负、两位小数）、日期格式。
- 业务：员工工号唯一（库内冲突即报错）、科目必须命中启用白名单、期间格式 YYYY-MM、预算重复（员工+科目+期间）报错。
- 失败回执：错误行号 + 字段 + 原因；全部行合法才写入。

### 7.3 事务边界
- Service 层 @Transactional：校验全部通过 → 批量写入 → 提交；任一失败 → 整批回滚，返回错误回执。

## 8. 错误处理与异常

- 业务异常统一 BusinessException，页面/回执友好提示。
- 越权访问 → 403 页面；未登录 → 跳登录页。
- 上传格式不识别、文件为空 → 明确报错。
- 数据库约束冲突（工号/科目编码唯一）映射为业务提示。

## 9. 测试策略

- 单元测试：导入解析与校验器（正确行、错误行、边界金额/日期）、预算白名单校验、预算对比计算。
- 集成测试（Spring Boot Test + H2 或测试库）：员工 CRUD、导入「全对才导入/整批回滚」路径、权限矩阵。
- 冒烟：启动应用，登录 → 员工 CRUD → 导入模板下载与上传 → 看板展示，全链路手工走查。

## 10. 初始化与运行

- SQL 脚本：schema.sql（建表）+ data.sql（种子：管理员账号 admin、示例成本科目白名单、示例员工）。
- 管理员种子账号：admin / 初始化密码（首次登录提示修改）。
- application.yml：数据源、MyBatis、PageHelper、文件上传大小限制（建议 ≤ 5MB）。

## 11. 设计自审记录

- 占位符：无 TBD/TODO，全部决策已确认。
- 一致性：技术基线（Spring Boot 3.x/Jakarta）贯穿配置、依赖、代码；导入策略（整批回滚）在 6.6/7.3 一致表述。
- 范围：单模块单体应用，适合一个实施计划。
- 歧义收敛：白名单=成本科目/预算项目白名单；导入=全对才导入；普通用户=维护本人+看板只读。

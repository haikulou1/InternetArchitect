# 需求澄清 - DIMA 分析文档

> 阶段: loop-1 / 需求澄清
> 技能: brainstorming
> 状态: ✅ 全部澄清完成，设计定稿，可进入实施

---

## 1. 需求概述

分别写三个接口 helloworld、哈希算法以及冒泡排序；前端新增一个页面，有三个 tab 分别展示不同的执行结果。新增导出按钮，后台提供导出接口，支持导出各个页面的展示结果。后端再做个埋点，获取调用次数和调用人，前端在当前页面上可视化出来一个报表查看调用情况（根据不同的维度：人员类型、人员层级、人员部门等），折线图以及饼图和柱状图不同展示形式。

## 2. 跨仓依赖与现状摘要

| 仓库 | 定位 | 关键资产 |
|------|------|----------|
| **InternetArchitect** | 后端课程代码仓 | Spring Boot 2.2.2 (`com.mashibing:study`)，SpringCloud 微服务示例 |
| **leecode** | 算法/数据结构 Java 实现 | 已有 `冒泡排序.java`（冒泡排序）、`HashTabDemo.java`（哈希表取模法） |
| **haikulou1.github.io** | Hexo 静态博客 | 前端页面展示入口，现有 js/css 体系 |

### 关键发现

- **leecode** 已有冒泡排序和哈希表实现，可直接复用算法核心逻辑
- **InternetArchitect** 的 Spring Boot 2.2.2 项目可作为后端技术参考（实际后端建在 leecode）
- **haikulou1.github.io** 是 Hexo 博客，需新增独立页面承载前端功能（三 Tab + 图表可视化）

## 3. 待澄清问题

### 已确认
1. **后端项目归属** → ✅ **leecode 仓库**（在 leecode 中新建 Spring Boot 后端模块，与现有算法实现同仓）

### 已确认
2. **"哈希算法"定义** → ✅ **密码学哈希 MD5/SHA256**（对输入字符串计算哈希摘要，返回十六进制字符串）

### 已确认
3. **前端技术方案** → ✅ **Vue/React SPA**（在 haikulou1.github.io 中构建单页应用）

### 已确认
4. **埋点维度数据来源** → ✅ **请求参数传入**（前端调用时在请求中传入人员类型/层级/部门等维度信息）

### 已确认
5. **导出格式** → ✅ **CSV + Excel (xlsx) 双格式支持**

### 已确认
6. **冒泡排序输入** → ✅ **固定数组**（后端硬编码固定数组演示排序过程）

---

## 汇总：全部 6 项澄清已完成

| # | 维度 | 决策 |
|---|------|------|
| 1 | 后端项目归属 | **leecode 仓库** |
| 2 | 哈希算法 | **密码学哈希 MD5/SHA256** |
| 3 | 前端方案 | **Vue/React SPA**（haikulou1.github.io） |
| 4 | 埋点维度数据 | **请求参数传入** |
| 5 | 导出格式 | **CSV + Excel (xlsx) 双格式** |
| 6 | 冒泡排序输入 | **固定数组** |

---

## 4. 待实施

### 后端（leecode 仓库）— 新建 `springboot-api` 模块（Spring Boot 3.x / Java 17+）
| # | 接口 | 方法 | 路径 | 说明 |
|---|------|------|------|------|
| 1 | HelloWorld | GET | `/api/helloworld` | 返回 "Hello World" |
| 2 | 哈希 | POST | `/api/hash` | Body: `{input, algo}` → MD5/SHA256 十六进制 |
| 3 | 排序 | GET | `/api/bubblesort` | 固定数组冒泡排序，返回每轮过程 |
| 4 | 埋点上报 | POST | `/api/track` | Body: `{apiName, caller, type, level, dept}` |
| 5 | 导出报表 | GET | `/api/export?format=csv\|xlsx&dimension=type\|level\|dept` | 聚合统计导出 |
| 6 | 统计查询 | GET | `/api/stats?dimension=type\|level\|dept` | JSON 聚合统计 |

- 复用 leecode 现有 `冒泡排序.java`、`HashTabDemo.java` 算法核心
- 埋点存储：H2 内存数据库（轻量无外部依赖）
- 埋点采集：Spring AOP 切面自动记录每次 API 调用的调用人和时间
- 埋点维度枚举（默认值）：
  - **人员类型**：正式 / 外包 / 实习
  - **人员层级**：L1 / L2 / L3 / L4 / L5 / L6 / L7 / L8 / L9 / L10
  - **人员部门**：技术 / 产品 / 运营 / 市场 / 职能

### 前端（haikulou1.github.io）— React 18 + Vite SPA
- 新建 `/api-demo/` 页面，三 Tab 切换
- 图表库：ECharts（折线图/饼图/柱状图）
- 导出：前端调用导出接口，触发浏览器下载 CSV/XLSX

---

## 6. Spec 自检 (Self-Review)

| 检查项 | 结果 |
|--------|------|
| **Placeholder 扫描** | ✅ 无 TBD/TODO，接口路径和参数均已明确 |
| **内部一致性** | ✅ 架构图已修正为 leecode 后端，与确认决策一致 |
| **范围检查** | ✅ 6 接口 + 1 前端页面，适合单次实施计划 |
| **歧义检查** | ✅ 前端框架已确认：React 18 + Vite |
| **歧义检查** | ✅ Spring Boot 版本已确认：3.x (Java 17+) |
| **歧义检查** | ✅ 埋点维度枚举已确认：正式/外包/实习，L1-L10，技术/产品/运营/市场/职能 |

### 收口状态：全部 3 项已确认，无遗留问题。

## 5. 架构设想

> ⚠️ 注意：以下架构已根据全部 6 项澄清结果修正。后端确认在 **leecode** 仓库中新建 Spring Boot 模块。

```
┌─────────────────────────────────────────────────┐
│  haikulou1.github.io (前端 SPA)                  │
│  /api-demo/                                      │
│  ├── Tab1: HelloWorld 结果展示                   │
│  ├── Tab2: 哈希算法 结果展示                     │
│  ├── Tab3: 冒泡排序 结果展示                     │
│  ├── 导出按钮 → 调用后端导出接口(CSV/XLSX)       │
│  └── 埋点报表 → 折线图/饼图/柱状图(ECharts)      │
└──────────────┬──────────────────────────────────┘
               │ HTTP/REST (跨域)
┌──────────────▼──────────────────────────────────┐
│  leecode (Spring Boot 后端 + 算法核心)           │
│  ├── datastruct/src/排序/冒泡排序.java (复用)    │
│  ├── datastruct/src/Hashtab/HashTabDemo.java     │
│  ├── springboot-api/ (新建模块)                  │
│  │   ├── GET  /api/helloworld                   │
│  │   ├── POST /api/hash    {input, algo}         │
│  │   ├── GET  /api/bubblesort                    │
│  │   ├── GET  /api/export?format=csv|xlsx        │
│  │   │       &dimension=type|level|dept          │
│  │   ├── POST /api/track  (埋点上报)             │
│  │   └── GET  /api/stats   (聚合统计查询)        │
│  └── AOP 埋点切面 + H2/内存存储                  │
└─────────────────────────────────────────────────┘
```

## 7. 当前状态

- **loop-1 需求澄清**：✅ 全部 9 项澄清已完成，设计文档已定稿。
- **Spec 自检**：全部通过，无遗留问题。
- **下一动作**：进入实施阶段（writing-plans）。
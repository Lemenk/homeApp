# 家庭记账 App — 开发计划（v1 网页端）

> 依据：`PRD.md`
> 目标：交付可运行的 v1 网页端（前端 + 后端 + 数据库），架构为多端复用预留。
> 技术栈为**推荐方案**，可结合个人偏好替换（替代方案在文中标注）。

---

## 1. 技术选型（推荐）

| 层 | 选型 | 说明 / 替代方案 |
|---|---|---|
| 前端框架 | **Vue 3 + TypeScript + Vite** | 替代：React + Vite |
| 状态管理 | Pinia | 存储登录态、当前账本、用户信息 |
| 路由 | Vue Router | 页面地图见第 5 节 |
| UI 组件库 | Element Plus | 表单 / 表格 / 弹窗；替代：Ant Design Vue |
| 图表 | ECharts | 收支趋势、分类占比 |
| 后端框架 | **Spring Boot 3.x + Java 17** | 契合你的 Java 背景 |
| 认证鉴权 | Spring Security + **JWT**（无状态，利于多端） | 替代：Sa-Token |
| ORM | MyBatis-Plus | 替代：Spring Data JPA |
| 数据库 | **MySQL 8**（主库） + **Redis**（缓存 / 短信码 / 登录态） | |
| 短信服务 | 阿里云 / 腾讯云短信（手机号验证码） | 需企业资质，见风险 |
| 微信登录（后置） | 微信开放平台「网站应用」扫码登录 | **v1 暂缓**：缺备案域名 + 企业主体；接口与开关预留 |
| API 文档 | springdoc-openapi (Swagger UI) | |
| 部署 | 云服务器 Linux + Nginx + Docker（可选） | 前后端分离部署 |

---

## 2. 系统架构

```
浏览器（Vue3 SPA）──HTTPS──> Nginx ──> 后端服务（Spring Boot, RESTful API）
                                        ├── MySQL 8（业务数据）
                                        └── Redis（验证码 / 缓存）
                                        └── 短信服务（外部）；微信开放平台登录接口预留，资质到位后接入
```

- **前后端完全分离**，后端只暴露 RESTful API + JWT 鉴权。
- 接口与客户端解耦，后续小程序 / iOS 直接复用同一套 API，仅新增客户端。
- 业务模块划分（后端）：
  `auth` 认证 → `family` 家庭 → `ledger` 账本 → `category/tag` 分类标签 → `account` 账户 → `bill` 账单 → `budget` 预算 → `statistics` 统计 → `audit` 操作留痕。

---

## 3. 数据库设计（核心表）

> 以下为核心表结构与关系，用于指导建表脚本（`sql/init.sql`）。

**user 用户**
- id, openid（微信，可空）, phone（可空）, nickname, avatar, status, created_at, updated_at

**family 家庭**
- id, name, creator_id, invite_code（唯一，邀请用）, created_at

**family_member 家庭成员关系**
- id, family_id, user_id, role（creator / member）, joined_at
- 唯一索引 (family_id, user_id)

**ledger 账本**
- id, name, type（public 公共 / personal 个人）, icon, theme, owner_id（创建者）, family_id（公共账本关联家庭，个人账本为空）, created_at, status

**ledger_member 账本成员关系**
- id, ledger_id, user_id, role（creator / member）, joined_at
- 唯一索引 (ledger_id, user_id)；个人账本仅 owner 一人

**category 分类**
- id, ledger_id, type（expense / income）, name, icon, sort, enabled, created_at
- 创建者维护；每个账本支出 / 收入各一套预设 + 自定义

**tag 标签**
- id, ledger_id, name, color, created_by, created_at
- 创建者维护标签库；账单关联 tag（bill_tag 多对多）

**account 账户**
- id, ledger_id, type（common 常见 / liability 负债 / stored_value 储值 / investment 理财）, name, icon, initial_balance, balance（当前余额）, status, created_at

**bill 账单**
- id, ledger_id, type（expense / income / transfer）, category_id, member_id（记账人，公共账本可代记）, amount, bill_date, remark, created_by, created_at, updated_at

**bill_account 账单-账户明细**（支撑多账户组合付款 / 转账）
- id, bill_id, account_id, direction（out 支出 / in 收入）, amount, pair_id（转账配对：同一次转账的两条明细用同一 pair_id）
- 校验约束：同一账单所有 direction=out 明细金额合计 = 所有 direction=in 合计 = bill.amount（转账时 out 合计 = in 合计，总资产不变）

**budget 预算**
- id, ledger_id, category_id, amount, period_type（week / month / custom）, period_start, period_end, created_at

**audit_log 操作留痕**
- id, ledger_id, bill_id, operator_id, action（create / update / delete）, change_detail（JSON，记录变更前后）, created_at

**account_balance_log 余额调整留痕**
- id, account_id, old_balance, new_balance, reason, operator_id, created_at

> 关键约束：
> - 公共账本数据对全体成员可见；个人账本仅本人可见（数据按 ledger 隔离）。
> - 所有「改 / 删」账单必须写 audit_log。

---

## 4. 后端 API 设计（RESTful）

统一前缀 `/api`，JWT 鉴权（除 auth 接口外）。

**认证 auth**
- `POST /api/auth/sms-code` 发送验证码
- `POST /api/auth/login/phone` 手机号验证码登录
- `POST /api/auth/login/wechat` 微信扫码登录（code 换 token）
- `GET /api/auth/me` 当前用户信息

**家庭 family**
- `POST /api/families` 创建家庭（成为创建者，生成 invite_code）
- `GET /api/families/me` 我的家庭
- `GET /api/families/{id}/members` 成员列表
- `POST /api/families/{id}/invite` 生成 / 刷新邀请码（创建者）
- `POST /api/families/join` 凭邀请码加入

**账本 ledger**
- `GET /api/ledgers` 我的账本列表
- `POST /api/ledgers` 新建账本（类型 / 名称 / 图标 / 主题）
- `GET /api/ledgers/{id}` 账本详情
- `DELETE /api/ledgers/{id}` 删除账本（二次确认，级联处理）
- `GET /api/ledgers/{id}/members` 账本成员
- `POST /api/ledgers/{id}/members` 邀请成员（创建者）
- `DELETE /api/ledgers/{id}/members/{userId}` 移除成员（创建者）
- `GET/POST/PUT/DELETE /api/ledgers/{id}/categories` 分类管理（创建者）
- `GET/POST/PUT/DELETE /api/ledgers/{id}/tags` 标签管理（创建者）

**账单 bill**
- `GET /api/ledgers/{id}/bills` 账单流水（筛选：type / category / account / member / 日期范围 + 分页 + 搜索）
- `POST /api/ledgers/{id}/bills` 记账（含 bill_account 明细，事务提交）
- `GET /api/bills/{id}` 账单详情（含账户明细、标签、操作记录）
- `PUT /api/bills/{id}` 编辑账单（写 audit_log）
- `DELETE /api/bills/{id}` 删除账单（写 audit_log）
- `GET /api/bills/{id}/logs` 操作记录

**账户 account**
- `GET /api/ledgers/{id}/accounts` 账户列表（按类型分组）
- `POST /api/ledgers/{id}/accounts` 新增账户
- `PUT /api/accounts/{id}` 编辑账户
- `POST /api/accounts/{id}/balance` 维护余额（写 account_balance_log）
- `GET /api/ledgers/{id}/accounts/summary` 资产总览（总资产 / 总负债 / 净资产）

**预算 budget**
- `GET /api/ledgers/{id}/budgets` 预算列表（含进度：已用 / 剩余 / 是否超支）
- `POST /api/ledgers/{id}/budgets` 新建
- `PUT /api/budgets/{id}` 编辑
- `DELETE /api/budgets/{id}` 删除

**统计 statistics**
- `GET /api/ledgers/{id}/stats/trend?range=&unit=` 收支趋势（日 / 周 / 月）
- `GET /api/ledgers/{id}/stats/category?range=` 分类占比（饼图数据）

> 转账实现约定：`POST /bills` 时 type=transfer，携带两条 bill_account（一 out 一 in，同一 pair_id），后端事务保证两边账户余额同步增减。

---

## 5. 前端页面与路由

```
/login                    登录（微信扫码 + 手机号验证码）
/onboarding               首次引导（创建 / 加入家庭 → 建账本）
/                         主框架（侧边栏 + 顶栏，路由嵌套）
  ├── /dashboard          首页概览
  ├── /ledgers            账本列表
  ├── /ledgers/new        新建账本
  ├── /ledgers/:id        账本详情（账单流水 + 筛选）
  ├── /ledgers/:id/settings  账本管理（仅创建者：成员 / 分类 / 标签 / 外观）
  ├── /bill/new           记账（四步流程，弹窗复用）
  ├── /bills/:id          账单详情 / 编辑（含操作记录）
  ├── /accounts           账户资产（总览 + 账户分组列表）
  ├── /accounts/new       新增账户
  ├── /accounts/:id       账户详情（余额 + 流水 + 调整记录）
  ├── /budgets            预算总览
  ├── /budgets/new        新建 / 编辑预算
  ├── /statistics         统计（趋势 + 占比）
  ├── /family             家庭（成员列表 + 邀请）
  └── /settings           设置（账户信息 / 成员查看 / 账本管理）
```

- 全局状态（Pinia）：`user`、`currentFamily`、`currentLedger`（账本切换器驱动全站数据）。
- 记账采用**弹窗组件**（全局 + 按钮触发），支持连续记账。

---

## 6. 开发阶段与任务拆分

> 每阶段均有可运行产物与验收点，前序验收通过再进下一阶段。

### Phase 0 — 项目初始化（约 3~4 天）
- [ ] 前后端脚手架：Vue3+Vite+TS / Spring Boot3+Java17，目录结构、统一响应体、全局异常处理
- [ ] Git 仓库初始化、`.gitignore`、README、分支规范
- [ ] MySQL 建库 + 建表脚本（`sql/init.sql`）+ 基础数据（支出 / 收入预设分类）
- [ ] Redis 接入；MyBatis-Plus 集成；Swagger 配置
- [ ] 统一前端请求封装（axios 拦截器、token 注入、401 处理）、路由骨架
- **验收**：前后端能各自启动，`GET /api/health` 通，页面骨架可访问。

### Phase 1 — 认证与用户（约 5~6 天）
- [ ] 手机号验证码：发送接口（接短信服务 / 先接 Redis 存储 + 开发环境 console 输出）、登录接口、JWT 签发
- [ ] 微信登录：仅预留接口与功能开关（资质具备后接入，v1 不阻塞）
- [ ] 用户表读写、`/api/auth/me`
- [ ] 前端登录页（手机号验证码为主，微信入口置灰提示"即将开放"）、登录态持久化（Pinia + localStorage）
- **验收**：手机号验证码可登录，刷新页面登录态不丢，退出登录生效。

### Phase 2 — 家庭与成员（约 3~4 天）
- [ ] 家庭 CRUD、邀请码生成、凭码加入
- [ ] 成员列表 / 角色
- [ ] 前端首次引导页（创建 / 加入家庭）、家庭页（成员列表 + 邀请）
- **验收**：A 创建家庭 → 生成邀请码 → B 登录后凭码加入 → 双方看到同一家庭成员。

### Phase 3 — 账本与基础数据（约 4~5 天）
- [ ] 账本 CRUD（公共 / 个人）、账本成员关系、当前账本概念
- [ ] 分类管理、标签管理（创建者权限校验）
- [ ] 前端账本列表 / 新建 / 详情骨架、账本管理页、顶部账本切换器
- **验收**：建公共账本并邀请成员后，成员可见该账本；个人账本仅本人可见；创建者可增删分类 / 标签，普通成员无入口。

### Phase 4 — 记账核心（约 8~10 天，重点）
- [ ] 账单 CRUD + 多账户明细事务（bill_account）
- [ ] 支出多账户组合付款（前端金额分配 + 后端校验分配合计 = 总额）
- [ ] 转账（pair 配对，双边余额增减）
- [ ] 记账人字段：公共账本可代记，个人账本固定本人
- [ ] 分类图标网格、日期（精确到时分）、备注、标签多选
- [ ] 连续记账（保留上次账户 / 标签）
- [ ] 账单列表：日期分组、筛选（类型 / 分类 / 账户 / 成员 / 日期）、搜索、排序
- [ ] 操作留痕：改 / 删写 audit_log，账单详情展示操作记录
- **验收**：完整走通"建账本 → 记支出（多账户分配）→ 记收入 → 记转账 → 编辑 → 删除"，账户余额随之正确变化；改删有记录；组合付款分配不等于总额被拦截。

### Phase 5 — 账户资产（约 4~5 天）
- [ ] 账户 CRUD（四类账户）、负债视觉区分
- [ ] 余额维护 + account_balance_log 留痕
- [ ] 资产总览（总资产 / 总负债 / 净资产）、账户详情（流水 + 调整记录）
- [ ] 前端账户页 / 新增 / 详情
- **验收**：资产总览数值 = 各账户余额汇总；负债账户红/橙展示；调整余额有原因与记录。

### Phase 6 — 预算（约 3~4 天）
- [ ] 预算 CRUD（按分类、自定义周期）
- [ ] 预算进度计算（按周期聚合分类支出）、超支标记
- [ ] 前端预算总览（进度条、超支标红）、新建 / 编辑
- **验收**：月度餐饮预算 2000，本月餐饮支出超 2000 后预算页标红并提示。

### Phase 7 — 统计（约 3~4 天）
- [ ] 后端聚合接口：趋势（日 / 周 / 月）、分类占比
- [ ] ECharts 集成：折线 / 柱状趋势图、饼图，筛选器（账本 / 时间 / 收 / 支）
- **验收**：图表数据与账单流水一致，切换时间范围与账本数据正确刷新。

### Phase 8 — 设置与收尾（约 4~6 天）
- [ ] 设置页：账户信息只读、成员查看、账本管理（新建 / 删除二次确认）
- [ ] 空状态全量补齐、加载态、全局错误提示
- [ ] 权限可见性终检（非创建者无管理入口）
- [ ] 前后端联调、边界用例（删除账本级联、并发记账、金额精度 BigDecimal）
- [ ] 部署上线：Nginx + 后端部署 + HTTPS 域名（微信登录正式配置待资质到位后补）
- [ ] 编写部署文档 `docs/DEPLOY.md` 与 README 使用说明
- **验收**：全功能在真实环境跑通，达到 PRD 第 5 节非功能要求。

---

## 7. 里程碑与排期（建议，单人全职）

| 里程碑 | 阶段 | 累计工作日 |
|---|---|---|
| M1 可登录 | P0+P1 | ~9-10 |
| M2 可建家庭 / 账本 | P2+P3 | ~16-19 |
| M3 记账闭环（核心） | P4 | ~24-29 |
| M4 资产 / 预算 / 统计 | P5+P6+P7 | ~34-41 |
| M5 v1 上线 | P8 | ~38-47（约 8~10 周） |

> 手机号验证码登录先行，微信登录在资质到位后接入，可并行推进。

---

## 8. 前置条件与风险

1. **微信网页扫码登录**：需微信开放平台「网站应用」+ **已备案域名 + HTTPS + 企业主体**。**已确认**：v1 不接入，先做手机号验证码登录，微信登录后置（接口预留开关，资质到位后开启）。
2. **短信服务**：阿里 / 腾讯云短信需企业认证与签名审核；开发期可用 Redis 验证码 + 控制台输出代替。
3. **域名与部署**：云服务器 + 备案域名（可后续补）。
4. **金额精度**：全部使用 `BigDecimal`，禁止浮点。
5. **数据一致性**：多账户记账 / 转账必须事务；删除账本需级联清理（账单、账户、预算、日志）。
6. **并发**：家庭公共账本多人记账，后端需做好幂等与事务隔离。

---

## 9. 验收标准（整体）

- 功能：PRD 第 3 节 v1 范围全部实现，无缺项。
- 记账正确性：任意组合付款 / 转账后，账户余额、资产总览、统计图表三者数据一致。
- 留痕：所有改 / 删操作可在账单详情查到操作记录。
- 权限：非创建者看不到管理入口，越权接口返回 403。
- 体验：记账流程 ≤ 4 步，连续记账可用，空状态 / 加载态齐全。
- 架构：后端为纯 API 服务，新增小程序 / iOS 客户端不需改后端。

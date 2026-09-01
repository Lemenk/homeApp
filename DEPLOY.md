# 家庭记账 App · v1 部署文档

## 一、架构与运行环境

| 模块 | 技术栈 | 说明 |
| --- | --- | --- |
| 前端 | Vue 3 + TypeScript + Vite 5 + Element Plus + Pinia + ECharts | 端口 5173（开发），构建产物为静态文件 |
| 后端 | Spring Boot 3.2.5 + Java 17 + MyBatis-Plus 3.5.7 + JWT | 端口 8080 |
| 数据库 | 开发/测试/生产统一用 MySQL 8 | 建表脚本见 `backend/src/main/resources/db/init.sql`（开发自动执行）与 `sql/schema.sql`（生产初始化） |

v1 仅交付网页端；微信小程序 / iOS App 在 v2 规划中，后端接口按多端复用设计。

## 二、本地开发启动

### 1. 后端（需本机 MySQL 8 可用）

```bash
cd backend
# 使用 JDK 17 与 Maven（示例路径）
set JAVA_HOME=C:\Users\lemenk\dev\tools\jdk-17.0.20.1+1
mvn spring-boot:run
```

- 数据库连接默认 `127.0.0.1:3306 / family_home / root / root`，可用环境变量 `MYSQL_HOST / MYSQL_PORT / MYSQL_DB / MYSQL_USER / MYSQL_PASSWORD` 覆盖；应用启动时自动执行建表脚本（`CREATE TABLE IF NOT EXISTS`，幂等）。
- 验证码服务：`app.sms.debug=true`（默认），任意手机号验证码为 `123456`；生产切换为真实短信通道。
- 接口文档：启动后访问 `http://localhost:8080/swagger-ui.html`（springdoc）。

### 2. 前端

```bash
cd frontend
npm install
npm run dev
```

- 开发服务器将 `/api` 代理到 `http://localhost:8080`（见 `vite.config.ts`）。
- 浏览器访问 `http://localhost:5173`。

### 3. 首次使用流程

1. 注册/登录：输入手机号 → 验证码 `123456`（开发环境）。
2. 创建或加入家庭（家庭有 8 位邀请码，成员凭码加入）。
3. 创建账本（个人 / 公共；公共账本需先有家庭，家庭成员自动入账本）。
4. 进入账本 → 新增账户（现金/银行卡/信用卡/储值/投资）。
5. 记一笔：支持支出/收入/转账，多账户组合付款，可添加备注、标签、记账人。

## 三、生产部署（MySQL）

### 1. 初始化数据库

```sql
CREATE DATABASE family_home DEFAULT CHARACTER SET utf8mb4;
USE family_home;
SOURCE backend/src/main/resources/db/init.sql;
```

### 2. 后端配置

复制 `application-prod.yml` 中对应配置到运行环境，关键项：

```yaml
spring:
  datasource:
    url: jdbc:mysql://<host>:3306/family_home?useUnicode=true&characterEncoding=utf8&serverTimezone=Asia/Shanghai
    username: <user>
    password: <pass>
app:
  sms:
    debug: false          # 关闭万能验证码
```

打包并运行：

```bash
cd backend
mvn clean package -DskipTests
java -jar target/family-home-backend-0.1.0.jar --spring.profiles.active=prod
```

### 3. 前端构建与托管

```bash
cd frontend
npm run build      # 产物在 dist/
```

将 `dist/` 部署到 Nginx 等静态服务器，并把 `/api` 反向代理到后端 8080：

```nginx
location /api/ {
    proxy_pass http://127.0.0.1:8080;
}
location / {
    root /path/to/dist;
    try_files $uri $uri/ /index.html;
}
```

## 四、测试

```bash
# 后端：全部测试（需本机 MySQL 8 可用，连接信息同开发配置）
cd backend
set JAVA_HOME=C:\Users\lemenk\dev\tools\jdk-17.0.20.1+1
mvn test

# 前端：单元测试 + 类型检查
cd frontend
npm run test       # vitest
npx vue-tsc --noEmit
```

## 五、版本规划（v1 范围边界）

- ✅ 已实现：手机号验证码登录、家庭/成员、个人+公共账本、分类/标签管理、记账（支出/收入/转账、多账户付款、记账人、备注/标签）、操作留痕、账户资产（含负债/储值/投资、余额调整、资产总览）、预算（按月/自定义周期、进度、超支标记）、统计（收支趋势、分类占比）、账本切换与外观入口、设置（账户信息/成员查看）。
- ⏳ v2 规划：微信登录（接口已预留 `POST /api/auth/login/wechat`，需备案域名+企业主体）、iOS App、微信小程序、借入借出、更多统计维度。

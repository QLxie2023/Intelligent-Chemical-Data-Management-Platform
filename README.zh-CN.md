# Chem+ 实验数据管理与分析平台

[English](README.md) | [中文](README.zh-CN.md)

## 项目简介

Chem+ 是一个面向化学研究场景的实验数据管理与分析平台。用户可以在网页端创建研究项目，上传实验文档和图片，调用 AI 分析服务提取结构化信息，并对分析结果进行查看、编辑和保存。

系统由 Vue 3 前端、Spring Boot 后端、MySQL 数据库和外部 AI 解析服务组成。

## 主要功能

- 基于邀请码的用户注册与登录。
- 项目创建、查看、删除和项目详情管理。
- 按项目上传实验文档和实验图片。
- 对上传文件进行 AI 辅助分析。
- 查看和编辑 AI 生成的摘要、关键词和结构化特征表。
- 用户管理和个人资料管理。
- 数据洞察页面，用于查看项目和文件统计信息。

## 技术栈

| 层级 | 技术 |
| --- | --- |
| 前端 | Vue 3, Vue Router, Vite, Axios, Tailwind CSS |
| 后端 | Java 17, Spring Boot 3.3, Spring Security, Spring Data JPA |
| 数据库 | MySQL |
| 构建工具 | Maven Wrapper, npm |
| AI 服务 | 基于 DashScope 兼容模式的 Qwen Long API |

## 项目结构

```text
.
+-- backend/            # Spring Boot 后端服务
+-- database/           # 数据库建表和初始化脚本
+-- frontend/           # Vue 3 前端应用
+-- start.bat           # Windows 一键启动脚本
+-- README.md
```

## 运行前置条件

运行项目前需要安装：

- 推荐使用 Windows 环境运行 `start.bat`。
- JDK 17。
- Node.js 18+ 或 20+。
- npm。
- MySQL 8.x。
- 可访问 Maven 和 npm 依赖源的网络环境。
- 如果需要使用 AI 分析功能，需要配置有效的 AI 服务。

## 配置说明

后端配置文件分布在：

```text
backend/src/main/resources/application.yml
backend/src/main/resources/application.properties
```

重要默认配置如下：

| 配置项 | 默认值 |
| --- | --- |
| 后端地址 | `http://localhost:8080` |
| 前端地址 | `http://localhost:5173` |
| 数据库名 | `datagov_db` |
| MySQL 地址 | `127.0.0.1:3306` |
| MySQL 用户名 | `teammate` |
| MySQL 密码 | `123456` |
| 文件上传目录 | `C:/uploads/chem_data_platform` |
| 最大上传大小 | `10 MB` |
| JWT 有效期 | `24 hours` |
| Qwen API Key | 环境变量 `QWEN_API_KEY` |

请根据本机环境修改数据库用户名、密码、地址和文件上传目录。

如果要部署到本地并使用 AI 分析功能，必须先将 Qwen API Key 配置为本机环境变量。项目会通过 `application.properties` 中的 `qwen.api-key=${QWEN_API_KEY:}` 读取该环境变量。

PowerShell：

```powershell
[Environment]::SetEnvironmentVariable("QWEN_API_KEY", "your_dashscope_api_key", "User")
```

命令提示符：

```bat
setx QWEN_API_KEY "your_dashscope_api_key"
```

配置后需要重启终端或 IDE，让环境变量生效。

## 数据库初始化

1. 启动 MySQL。
2. 使用以下脚本创建或重置数据库：

```text
database/schema.sql
```

3. 使用以下脚本插入邀请码或权限相关数据：

```text
database/permission.sql
```

后端配置了 `spring.jpa.hibernate.ddl-auto=update`，但首次运行时仍建议执行 SQL 脚本完成初始化和测试数据准备。

## Windows 快速启动

在项目根目录运行：

```bat
start.bat
```

该脚本会自动完成：

1. 使用 Maven Wrapper 打包后端。
2. 如果前端不存在 `node_modules`，自动安装 npm 依赖。
3. 启动后端服务。
4. 启动前端开发服务器。

启动完成后访问：

```text
Backend:  http://localhost:8080
Frontend: http://localhost:5173
```

## 手动启动

### 后端

```bat
cd backend
.\mvnw.cmd clean package -DskipTests
java -jar target/auth-system-0.0.1-SNAPSHOT.jar
```

### 前端

```bat
cd frontend
npm install
npm run dev
```

浏览器访问：

```text
http://localhost:5173
```

## 生产构建

### 后端 JAR 包

```bat
cd backend
.\mvnw.cmd clean package -DskipTests
```

生成文件：

```text
backend/target/auth-system-0.0.1-SNAPSHOT.jar
```

### 前端静态文件

```bat
cd frontend
npm run build
```

生成目录：

```text
frontend/dist/
```

## 支持上传的文件类型

| 类型 | 当前配置支持 |
| --- | --- |
| 文档 | PDF, DOC, DOCX, XLS, XLSX, CSV |
| 图片 | JPEG, PNG, GIF |

## 基本使用流程

1. 使用有效邀请码注册账号。
2. 使用用户名和密码登录。
3. 在项目管理页面创建项目。
4. 向项目中上传实验文档或图片。
5. 等待 AI 分析完成。
6. 查看并编辑 AI 提取结果。
7. 确认并保存最终结果。
8. 通过数据看板和用户页面查看平台数据。

## 常见问题

### 后端打包失败

检查是否已安装 JDK 17，以及 Maven 依赖是否可以正常下载。

### 前端无法启动

进入 `frontend/` 目录执行 `npm install`，然后重新运行 `npm run dev`。

### 数据库连接失败

检查 MySQL 是否启动，并确认 `application.yml` 中的数据库地址、用户名和密码是否正确。

### 文件上传失败

检查文件类型、文件大小、后端服务状态，以及上传目录是否存在并可写。

### AI 分析无法工作

检查 `QWEN_API_KEY` 是否已正确设置，以及后端是否可以访问 DashScope API 接口。

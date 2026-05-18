# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

Chem+ (智能化学数据管理平台) — an intelligent chemical data management platform with AI-powered document analysis, project management, and knowledge graph visualization. Three services must run together:

| Service | Port | Tech |
|---------|------|------|
| Backend API | 8080 | Spring Boot 3.3, Java 17, MySQL |
| Knowledge Graph | 8000 | Python HTTP server + D3.js |
| Frontend | 5173 | Vue 3 + Vite + Tailwind CSS 4 |

## Commands

### Start all services
```
start.bat              # Windows: builds backend (if needed), starts all 3 services
start.bat --build      # Force rebuild backend JAR
```

### Backend (Maven)
```
cd backend
./mvnw clean package -DskipTests          # Build JAR
./mvnw spring-boot:run                    # Run in dev mode
./mvnw test                               # Run all tests
java -jar target/auth-system-0.0.1-SNAPSHOT.jar  # Run from JAR
```

### Frontend
```
cd frontend
npm install        # Install dependencies
npm run dev        # Dev server (port 5173)
npm run build      # Production build
```

### Knowledge Graph
```
cd frontend/my-notes-site
python start.py    # Parses markdown vault, starts HTTP server on port 8000
```

## Architecture

### Backend (`backend/src/main/java/chem_data_platform/demo/`)

Layered Spring Boot architecture: `controller` → `service` → `repository` → `entity`, with `dto` for request/response objects and `vo` for unified API responses.

- **Authentication**: JWT (jjwt 0.11.5) + Spring Security. `JwtAuthenticationFilter` validates Bearer tokens on all endpoints except `/api/v1/auth/*`, `/actuator/*`, `/media/*`. Tokens stored in localStorage on frontend.
- **API response wrapper**: All endpoints return `ApiResponse<T>` with status code, message, and data fields.
- **Registration**: Invitation code system — users need a valid code to register.
- **AI analysis**: Primary provider is Qwen (阿里云通义千问) via `QwenService`, optional fallback to Xunfei (讯飞星火). Analysis is async (`@Async`). Results include status tracking (PENDING → PROCESSING → COMPLETED/FAILED) and Excel export via Apache POI.
- **File uploads**: Max 100MB, stored at `C:/uploads/chem_data_platform`. Supports PDF, Word, TXT, and images (PNG/JPEG/GIF).
- **Maven repos**: Configured with Aliyun, Tencent, and Huawei mirrors (China network).

### Frontend (`frontend/src/`)

Vue 3 SPA with `<script setup>` SFCs. No Vuex/Pinia — state managed via localStorage (token, user info).

- **Router** (`router/index.js`): `/`, `/login`, `/register`, `/search`, `/user`, `/insight`, `/normal_user`, `/projects/:id`. Auth guard exists but is currently commented out.
- **API client** (`utils/request.js`): Axios instance with baseURL `/api/v1`, 100s timeout, JWT interceptor from localStorage.
- **Vite proxy** (`vite.config.js`): `/api` → `http://localhost:8080` with `changeOrigin: true`.
- **Insight page** embeds the knowledge graph via iframe to `http://localhost:8000`.

### Knowledge Graph (`frontend/my-notes-site/`)

Standalone service built on an Obsidian vault of chemistry markdown files. `start.py` parses markdown → JSON knowledge graph data → serves static web files (D3.js visualization) on port 8000.

### Database

MySQL (`datagov_db`), schema in `database/schema.sql`. Key tables: `users`, `projects`, `project_members`, `file_infos`, `image_infos`, `analysis_records`, `analysis_results`, `graph_visualizations`. JPA DDL auto-update is enabled. Charset: utf8mb4.

## Key External Dependencies

- **Qwen API** (`backend/qwen/`): API docs for chat, file parsing, and response formats. Primary AI provider.
- **Swagger/OpenAPI**: Available at `/swagger-ui.html` when backend is running (springdoc 2.1.0).
- **Prometheus metrics**: Available at `/actuator/prometheus`.

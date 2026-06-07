# Chem+ Experimental Data Management and Analytics Platform

[English](README.md) | [中文](README.zh-CN.md)

## Overview

Chem+ is a web-based experimental data management and analytics platform for chemical research workflows. It helps users create research projects, upload experimental documents and images, run AI-assisted analysis, review structured extraction results, and manage project-related data in a browser-based interface.

The system uses a Vue 3 single-page frontend, a Spring Boot backend API, a MySQL database, and an external AI parsing service.

## Main Features

- User registration and login with invitation-code based access control.
- Project creation, browsing, deletion, and project detail management.
- Document and image upload for each project.
- AI-assisted analysis for uploaded experimental files.
- Editable analysis results with summaries, keywords, and structured feature tables.
- User management and profile management.
- Data insight dashboard for project and file statistics.

## Tech Stack

| Layer | Technology |
| --- | --- |
| Frontend | Vue 3, Vue Router, Vite, Axios, Tailwind CSS |
| Backend | Java 17, Spring Boot 3.3, Spring Security, Spring Data JPA |
| Database | MySQL |
| Build Tools | Maven Wrapper, npm |
| AI Service | Qwen Long API via DashScope compatible mode |

## Project Structure

```text
.
+-- backend/            # Spring Boot backend service
+-- database/           # Database schema and seed scripts
+-- frontend/           # Vue 3 frontend application
+-- start.bat           # Windows startup script
+-- README.md
```

## Prerequisites

Install the following before running the project:

- Windows environment is recommended for `start.bat`.
- JDK 17.
- Node.js 18+ or 20+.
- npm.
- MySQL 8.x.
- Internet access for Maven and npm dependency downloads.
- A valid AI service configuration if AI analysis is required.

## Configuration

Backend configuration is split across:

```text
backend/src/main/resources/application.yml
backend/src/main/resources/application.properties
```

Important default values:

| Item | Default |
| --- | --- |
| Backend URL | `http://localhost:8080` |
| Frontend URL | `http://localhost:5173` |
| Database | `datagov_db` |
| MySQL host | `127.0.0.1:3306` |
| MySQL username | `teammate` |
| MySQL password | `123456` |
| Upload directory | `C:/uploads/chem_data_platform` |
| Max upload size | `10 MB` |
| JWT expiration | `24 hours` |
| Qwen API key | Environment variable `QWEN_API_KEY` |

Update the database username, password, host, and upload path to match your local environment.

For local deployment, you must set the Qwen API key as a local environment variable before starting the backend. The project reads it through `qwen.api-key=${QWEN_API_KEY:}` in `application.properties`.

PowerShell:

```powershell
[Environment]::SetEnvironmentVariable("QWEN_API_KEY", "your_dashscope_api_key", "User")
```

Command Prompt:

```bat
setx QWEN_API_KEY "your_dashscope_api_key"
```

Restart the terminal or IDE after setting the variable.

## Database Setup

1. Start MySQL.
2. Create or reset the database using:

```text
database/schema.sql
```

3. Insert invitation codes or permission data using:

```text
database/permission.sql
```

The backend uses `spring.jpa.hibernate.ddl-auto=update`, but the SQL scripts are still recommended for first-time setup and seed data.

## Quick Start on Windows

From the project root, run:

```bat
start.bat
```

The script will:

1. Build the backend with Maven Wrapper.
2. Install frontend dependencies if `node_modules` does not exist.
3. Start the backend service.
4. Start the frontend development server.

After startup:

```text
Backend:  http://localhost:8080
Frontend: http://localhost:5173
```

## Manual Startup

### Backend

```bat
cd backend
.\mvnw.cmd clean package -DskipTests
java -jar target/auth-system-0.0.1-SNAPSHOT.jar
```

### Frontend

```bat
cd frontend
npm install
npm run dev
```

Open the frontend in a browser:

```text
http://localhost:5173
```

## Build for Production

### Backend JAR

```bat
cd backend
.\mvnw.cmd clean package -DskipTests
```

Generated JAR:

```text
backend/target/auth-system-0.0.1-SNAPSHOT.jar
```

### Frontend Static Build

```bat
cd frontend
npm run build
```

Generated output:

```text
frontend/dist/
```

## Supported Upload Types

| Category | MIME types configured |
| --- | --- |
| Documents | PDF, DOC, DOCX, XLS, XLSX, CSV |
| Images | JPEG, PNG, GIF |

## Basic Usage Flow

1. Register with a valid invitation code.
2. Log in with username and password.
3. Create a project from the project management dashboard.
4. Upload documents or images to the project.
5. Wait for AI analysis to complete.
6. Review and edit extracted results.
7. Confirm and save the final results.
8. Use dashboards and user pages to review platform data.

## Troubleshooting

### Backend build fails

Check that JDK 17 is installed and available. If dependencies cannot be downloaded, check network access and Maven mirror availability.

### Frontend cannot start

Run `npm install` inside `frontend/`, then run `npm run dev` again.

### Database connection fails

Check MySQL status and update `spring.datasource.url`, `username`, and `password` in `application.yml`.

### Upload fails

Check file type, file size, backend status, and whether the upload directory exists and is writable.

### AI analysis does not work

Check whether `QWEN_API_KEY` is set correctly and whether the backend can access the DashScope API endpoint.

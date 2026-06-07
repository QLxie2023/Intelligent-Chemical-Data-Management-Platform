-- [Core fix]Force utf8mb4 encoding to avoid Windows insertion encoding errors
SET NAMES utf8mb4;

-- [Added]Create database with utf8mb4 character set
CREATE DATABASE IF NOT EXISTS datagov_db 
CHARACTER SET utf8mb4 
COLLATE utf8mb4_unicode_ci;

-- Select database
USE datagov_db;

-- 1. Disable foreign key checks
SET FOREIGN_KEY_CHECKS = 0;

-- 2. Drop all tables
DROP TABLE IF EXISTS analysis_results;
DROP TABLE IF EXISTS analysis_records;
DROP TABLE IF EXISTS image_infos;
DROP TABLE IF EXISTS file_infos;
DROP TABLE IF EXISTS project_members;
DROP TABLE IF EXISTS projects;
DROP TABLE IF EXISTS invitation_codes;
DROP TABLE IF EXISTS users;

-- 3. Re-enable foreign key checks
SET FOREIGN_KEY_CHECKS = 1;

-- 4. Recreate all tables
-- [1] Users table
CREATE TABLE users (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    username VARCHAR(50) UNIQUE NOT NULL,
    email VARCHAR(100) UNIQUE NULL,
    password_hash VARCHAR(255) NOT NULL,
    display_name VARCHAR(100) NOT NULL DEFAULT '',
    role ENUM('ROLE_ADMIN', 'ROLE_RESEARCHER') NOT NULL DEFAULT 'ROLE_RESEARCHER',
    enabled BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_username (username),
    INDEX idx_role (role)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- [2] Invitation codes table
CREATE TABLE invitation_codes (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    code VARCHAR(50) UNIQUE NOT NULL,
    role ENUM('ROLE_ADMIN', 'ROLE_RESEARCHER') NOT NULL,
    used BOOLEAN DEFAULT FALSE,
    created_by BIGINT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    used_by BIGINT,
    used_at TIMESTAMP NULL,
    FOREIGN KEY (created_by) REFERENCES users(id) ON DELETE SET NULL,
    FOREIGN KEY (used_by) REFERENCES users(id) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- [3] Projects table
CREATE TABLE projects (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    description TEXT,
    visibility ENUM('PUBLIC', 'PRIVATE') NOT NULL DEFAULT 'PRIVATE',
    owner_id BIGINT NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (owner_id) REFERENCES users(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- [4] Project members table
CREATE TABLE project_members (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    project_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    role ENUM('PROJECT_OWNER', 'PROJECT_MEMBER') NOT NULL DEFAULT 'PROJECT_MEMBER',
    joined_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uk_project_user (project_id, user_id),
    FOREIGN KEY (project_id) REFERENCES projects(id) ON DELETE CASCADE,
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- [5] File information table
CREATE TABLE file_infos (
    file_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    project_id BIGINT NOT NULL,
    file_name VARCHAR(255) NOT NULL,
    file_path VARCHAR(500) NOT NULL,
    file_type ENUM('DOCUMENT', 'IMAGE', 'AUDIO', 'VIDEO', 'ARCHIVE', 'OTHER') NOT NULL DEFAULT 'DOCUMENT',
    mime_type VARCHAR(100) NOT NULL,
    file_size BIGINT NOT NULL,
    uploader_id BIGINT NOT NULL,
    upload_timestamp TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    access_url VARCHAR(500) NULL,
    is_deleted BOOLEAN DEFAULT FALSE,
    deleted_at TIMESTAMP NULL,
    analysis_status VARCHAR(50) DEFAULT NULL COMMENT 'Analysis status: PENDING/PROCESSING/COMPLETED/FAILED',
    analysis_data LONGTEXT DEFAULT NULL COMMENT 'Analysis result data in JSON format',
    analysis_start_time DATETIME DEFAULT NULL COMMENT 'Analysis start time',
    analysis_end_time DATETIME DEFAULT NULL COMMENT 'Analysis end time',
    analysis_error_reason VARCHAR(500) DEFAULT NULL COMMENT 'Reason for analysis failure',
    INDEX idx_project_id (project_id),
    INDEX idx_analysis_status (analysis_status),
    FOREIGN KEY (project_id) REFERENCES projects(id) ON DELETE CASCADE,
    FOREIGN KEY (uploader_id) REFERENCES users(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- [6] Image information table
CREATE TABLE image_infos (
    image_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    project_id BIGINT NOT NULL,
    image_name VARCHAR(255) NOT NULL,
    image_url VARCHAR(500) NOT NULL,
    uploader_id BIGINT NOT NULL,
    upload_timestamp TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (project_id) REFERENCES projects(id) ON DELETE CASCADE,
    FOREIGN KEY (uploader_id) REFERENCES users(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- [7] Analysis records table
CREATE TABLE analysis_records (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    file_id BIGINT NOT NULL,
    status ENUM('PENDING', 'PROCESSING', 'COMPLETED', 'FAILED') NOT NULL DEFAULT 'PENDING',
    summary TEXT,
    result_json JSON, 
    error_message VARCHAR(500),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_file_id (file_id),
    FOREIGN KEY (file_id) REFERENCES file_infos(file_id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- [8] Analysis results table
CREATE TABLE analysis_results (
    result_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    file_id BIGINT NULL COMMENT 'Associated file ID if the analyzed item is a file',
    image_id BIGINT NULL COMMENT 'Associated image ID if the analyzed item is an image',
    project_id BIGINT NULL COMMENT 'Associated project ID',
    summary TEXT NULL COMMENT 'Analysis summary',
    raw_response LONGTEXT NULL COMMENT 'Raw response data in JSON format',
    status VARCHAR(20) NULL COMMENT 'Analysis status',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT 'Creation time',
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT 'Update time',
    INDEX idx_file_id (file_id),
    INDEX idx_image_id (image_id),
    INDEX idx_project_id (project_id),
    INDEX idx_status (status),
    FOREIGN KEY (file_id) REFERENCES file_infos(file_id) ON DELETE CASCADE,
    FOREIGN KEY (image_id) REFERENCES image_infos(image_id) ON DELETE CASCADE,
    FOREIGN KEY (project_id) REFERENCES projects(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 5. [fixed version]Insert test data
-- Insert users
INSERT INTO users (username, email, password_hash, display_name, role) VALUES
('admin', 'admin@datagov.com', '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL1lh.S6', 'System Administrator', 'ROLE_ADMIN'),
('researcher1', 'researcher1@example.com', '$2a$10$rOzZJkI7C8e.YbWp9.AkOeY6W9kQ8q8q8q8q8q8q8q8q8q8q8q8q8q8', 'Researcher One', 'ROLE_RESEARCHER');

-- Insert project
INSERT INTO projects (name, description, visibility, owner_id) VALUES
('Thermal Spray Coating Optimization Study', 'Performance analysis of Al2O3-TiO2 coatings based on the APS process.', 'PRIVATE', 2);

-- Insert project members
INSERT INTO project_members (project_id, user_id, role) VALUES
(1, 2, 'PROJECT_OWNER');

-- Insert file information; fixed by removing manually specified file_id and using auto-increment
INSERT INTO file_infos (project_id, file_name, file_path, file_type, mime_type, file_size, uploader_id, access_url) VALUES
(1, 'Optimization_of_plasma.pdf', '/uploads/projects/1/documents/opt_plasma.pdf', 'DOCUMENT', 'application/pdf', 3045120, 2, '/api/files/projects/1/documents/opt_plasma.pdf');

-- Insert analysis record
INSERT INTO analysis_records (file_id, status, summary, result_json) VALUES
(1, 'COMPLETED', 
 'This study used atmospheric plasma spraying (APS) to prepare nanostructured Al2O3-13%TiO2 coatings on an A36 low-carbon steel substrate. A two-level factorial experiment was used to obtain optimal coating performance.', 
 '[{"paperTitle": "Optimization of plasma spray parameters", "materialComposition": "Al2O3-13wt%TiO2", "tio2Content": 13.0, "sprayProcess": "APS", "powerKw": 35.0, "currentA": 600, "sprayDistanceMm": 100, "powderFeedRateGMin": "3 rpm", "hardnessGpa": 10.36, "wearRate": 0.0023}]'
);

-- 6. Final verification
SELECT '✅ Database update completed successfully!' AS 'Status information';

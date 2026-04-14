-- 选择数据库
USE datagov_db;

-- 1. 禁用外键检查
SET FOREIGN_KEY_CHECKS = 0;
target: 'http://localhost:8080',
-- 2. 删除所有表
DROP TABLE IF EXISTS graph_visualizations;
DROP TABLE IF EXISTS analysis_results;
DROP TABLE IF EXISTS analysis_records;
DROP TABLE IF EXISTS image_infos;
DROP TABLE IF EXISTS file_infos;
DROP TABLE IF EXISTS project_members;
DROP TABLE IF EXISTS projects;
DROP TABLE IF EXISTS invitation_codes;
DROP TABLE IF EXISTS users;

-- 3. 重新启用外键检查
SET FOREIGN_KEY_CHECKS = 1;

-- 4. 重新创建所有表

-- [1] 用户表
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

-- [2] 邀请码表
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

-- [3] 项目表
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

-- [4] 项目成员表
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

-- [5] 文件信息表
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
    -- AI 分析相关字段（2024年新增）
    analysis_status VARCHAR(50) DEFAULT NULL COMMENT '分析状态: PENDING/PROCESSING/COMPLETED/FAILED',
    analysis_data LONGTEXT DEFAULT NULL COMMENT '分析结果数据（JSON格式）',
    analysis_start_time DATETIME DEFAULT NULL COMMENT '分析开始时间',
    analysis_end_time DATETIME DEFAULT NULL COMMENT '分析结束时间',
    analysis_error_reason VARCHAR(500) DEFAULT NULL COMMENT '分析失败的错误原因',
    INDEX idx_project_id (project_id),
    INDEX idx_analysis_status (analysis_status),
    FOREIGN KEY (project_id) REFERENCES projects(id) ON DELETE CASCADE,
    FOREIGN KEY (uploader_id) REFERENCES users(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- [6] 图片信息表 (保留，虽然目前重点在PDF分析，但系统可能有图片上传)
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

-- [7] *** 分析记录表 ***
-- 用于存储针对 file_id 的 AI 分析结果
CREATE TABLE analysis_records (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    file_id BIGINT NOT NULL,
    -- 状态机：对应 API 的 PROCESSING, COMPLETED, FAILED
    status ENUM('PENDING', 'PROCESSING', 'COMPLETED', 'FAILED') NOT NULL DEFAULT 'PENDING',
    -- 摘要字段 (Summary)
    summary TEXT,
    -- 结构化数据 (Table Data)，使用 JSON 类型存储 List<Map>，兼容性好且易于扩展
    -- 如果你的 MySQL 版本低于 5.7，请将 JSON 改为 LONGTEXT
    result_json JSON, 
    error_message VARCHAR(500),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    -- 确保一个文件查找分析记录时能快速定位
    INDEX idx_file_id (file_id),
    FOREIGN KEY (file_id) REFERENCES file_infos(file_id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- [8] *** 分析结果表 ***
-- 用于存储文件/图片的 AI 分析结果（支持文件和图片）
CREATE TABLE analysis_results (
    result_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    file_id BIGINT NULL COMMENT '关联的文件ID（如果分析的是文件）',
    image_id BIGINT NULL COMMENT '关联的图片ID（如果分析的是图片）',
    project_id BIGINT NULL COMMENT '关联的项目ID',
    summary TEXT NULL COMMENT '分析摘要',
    raw_response LONGTEXT NULL COMMENT '原始响应数据（JSON格式）',
    status VARCHAR(20) NULL COMMENT '分析状态',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    INDEX idx_file_id (file_id),
    INDEX idx_image_id (image_id),
    INDEX idx_project_id (project_id),
    INDEX idx_status (status),
    FOREIGN KEY (file_id) REFERENCES file_infos(file_id) ON DELETE CASCADE,
    FOREIGN KEY (image_id) REFERENCES image_infos(image_id) ON DELETE CASCADE,
    FOREIGN KEY (project_id) REFERENCES projects(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- [9] *** 知识图谱可视化表 ***
-- 用于存储知识图谱的多种可视化格式数据
CREATE TABLE graph_visualizations (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '可视化记录ID',
    file_id BIGINT NOT NULL COMMENT '关联的文件ID',
    project_id BIGINT NOT NULL COMMENT '关联的项目ID',
    -- 多种格式的可视化数据
    d3_graph_data LONGTEXT COMMENT 'D3.js 格式的图谱数据（JSON格式）',
    echarts_graph_data LONGTEXT COMMENT 'ECharts 格式的图谱数据（JSON格式）',
    cytoscape_graph_data LONGTEXT COMMENT 'Cytoscape.js 格式的图谱数据（JSON格式）',
    svg_graph_image LONGTEXT COMMENT 'SVG 格式的图谱图像',
    raw_graph_data LONGTEXT COMMENT '原始图谱数据（通用格式，JSON）',
    -- 时间戳
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    -- 统计信息
    node_count INT COMMENT '图谱中的节点数量',
    link_count INT COMMENT '图谱中的边/链接数量',
    remarks TEXT COMMENT '备注信息',
    -- 索引
    INDEX idx_file_id (file_id),
    INDEX idx_project_id (project_id),
    INDEX idx_created_at (created_at),
    -- 外键约束
    FOREIGN KEY (file_id) REFERENCES file_infos(file_id) ON DELETE CASCADE,
    FOREIGN KEY (project_id) REFERENCES projects(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='知识图谱可视化数据表';

-- 5. 插入测试数据

-- 插入用户
INSERT INTO users (username, email, password_hash, display_name, role) VALUES
('admin', 'admin@datagov.com', '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL1lh.S6', '系统管理员', 'ROLE_ADMIN'),
('researcher1', 'researcher1@example.com', '$2a$10$rOzZJkI7C8e.YbWp9.AkOeY6W9kQ8q8q8q8q8q8q8q8q8q8q8q8q8q8', '马睿欣', 'ROLE_RESEARCHER');

-- 插入项目
INSERT INTO projects (name, description, visibility, owner_id) VALUES
('热喷涂涂层优化研究', '基于APS工艺的Al2O3-TiO2涂层性能分析。', 'PRIVATE', 2);

-- 插入项目成员
INSERT INTO project_members (project_id, user_id, role) VALUES
(1, 2, 'PROJECT_OWNER');

-- 插入文件信息 (对应你提供的PDF)
INSERT INTO file_infos (file_id, project_id, file_name, file_path, file_type, mime_type, file_size, uploader_id, access_url) VALUES
(1, 1, 'Optimization_of_plasma.pdf', '/uploads/projects/1/documents/opt_plasma.pdf', 'DOCUMENT', 'application/pdf', 3045120, 2, '/api/files/projects/1/documents/opt_plasma.pdf');

-- 插入一条“已完成”的分析记录作为测试 (使用 Mock Data 的内容)
-- 注意：这里将 JSON 字符串单行化以适应 SQL 插入
INSERT INTO analysis_records (file_id, status, summary, result_json) VALUES
(1, 'COMPLETED', 
 '本研究采用大气等离子喷涂（APS）技术在A36低碳钢基底上制备了纳米结构的Al2O3-13%TiO2涂层。通过两水平析因实验设计，获得了最佳的涂层性能。', 
 '[{"paperTitle": "Optimization of plasma spray parameters", "materialComposition": "Al2O3-13wt%TiO2", "tio2Content": 13.0, "sprayProcess": "APS", "powerKw": 35.0, "currentA": 600, "sprayDistanceMm": 100, "powderFeedRateGMin": "3 rpm", "hardnessGpa": 10.36, "wearRate": 0.0023}]'
);

-- 6. 最终验证
SELECT '✅ 数据库更新成功 (包含 analysis_records、analysis_results 和 graph_visualizations 表)！' AS '状态信息';
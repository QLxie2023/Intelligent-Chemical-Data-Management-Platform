-- 1. 创建 teammate 用户允许从 127.0.0.1 连接
CREATE USER IF NOT EXISTS 'teammate'@'127.0.0.1' IDENTIFIED BY '123456';

-- 2. 授予权限
GRANT ALL PRIVILEGES ON datagov_db.* TO 'teammate'@'127.0.0.1';

-- 3. 刷新权限
FLUSH PRIVILEGES;

-- 4. 切换到该数据库
USE datagov_db;

-- 插入一个新的邀请码（管理员权限）
INSERT INTO invitation_codes (code, role, used, created_by) 
VALUES ('TEST2024', 'ROLE_ADMIN', FALSE, NULL);

-- 或者插入研究员权限的邀请码
INSERT INTO invitation_codes (code, role, used, created_by) 
VALUES ('RESEARCHER2024', 'ROLE_RESEARCHER', FALSE, NULL);

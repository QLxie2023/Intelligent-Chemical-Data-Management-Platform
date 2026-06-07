-- 1. Create the teammate user and allow connections from 127.0.0.1
CREATE USER IF NOT EXISTS 'teammate'@'127.0.0.1' IDENTIFIED BY '123456';

-- 2. Grant permissions
GRANT ALL PRIVILEGES ON datagov_db.* TO 'teammate'@'127.0.0.1';

-- 3. Flush privileges
FLUSH PRIVILEGES;

-- 4. Switch to this database
USE datagov_db;

-- Insert a new invitation code with administrator permissions
INSERT INTO invitation_codes (code, role, used, created_by) 
VALUES ('TEST2024', 'ROLE_ADMIN', FALSE, NULL);

-- Or insert an invitation code with researcher permissions
INSERT INTO invitation_codes (code, role, used, created_by) 
VALUES ('RESEARCHER2024', 'ROLE_RESEARCHER', FALSE, NULL);

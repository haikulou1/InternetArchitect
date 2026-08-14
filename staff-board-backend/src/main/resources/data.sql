-- 初始管理员账户 (密码: admin123, BCrypt encoded)
INSERT INTO users (username, password, role, status) VALUES
('admin', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iKTVKIUi', 'ADMIN', 1);

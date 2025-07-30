-- PostgreSQL initialization script
        DO
    $do$
BEGIN
    IF NOT EXISTS (
        SELECT FROM pg_database WHERE datname = 'materialized_view_db'
    ) THEN
        EXECUTE 'CREATE DATABASE materialized_view_db';
END IF;
END
$do$;

-- Create user_view table
CREATE TABLE IF NOT EXISTS user_view (
    user_id BIGINT PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    email VARCHAR(255) NOT NULL,
    department VARCHAR(255),
    status VARCHAR(50) NOT NULL,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    version BIGINT NOT NULL DEFAULT 1,
    last_processed_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Create indexes for better query performance
CREATE INDEX IF NOT EXISTS idx_user_view_department ON user_view(department);
CREATE INDEX IF NOT EXISTS idx_user_view_status ON user_view(status);
CREATE INDEX IF NOT EXISTS idx_user_view_name ON user_view(name);
CREATE INDEX IF NOT EXISTS idx_user_view_email ON user_view(email);
CREATE INDEX IF NOT EXISTS idx_user_view_last_processed ON user_view(last_processed_at);

-- Create materialized view for department statistics
CREATE MATERIALIZED VIEW IF NOT EXISTS department_stats AS
SELECT 
    department,
    COUNT(*) as user_count,
    COUNT(CASE WHEN status = 'ACTIVE' THEN 1 END) as active_users,
    COUNT(CASE WHEN status = 'INACTIVE' THEN 1 END) as inactive_users,
    MAX(last_processed_at) as last_updated
FROM user_view
GROUP BY department;

-- Create index on materialized view
CREATE UNIQUE INDEX IF NOT EXISTS idx_department_stats_dept ON department_stats(department);

-- Function to refresh materialized view
CREATE OR REPLACE FUNCTION refresh_department_stats()
RETURNS TRIGGER AS $$
BEGIN
            REFRESH MATERIALIZED VIEW CONCURRENTLY department_stats;
    RETURN NULL;
END;
$$ LANGUAGE plpgsql;

-- Trigger to automatically refresh materialized view
DROP TRIGGER IF EXISTS trigger_refresh_department_stats ON user_view;
CREATE TRIGGER trigger_refresh_department_stats
    AFTER INSERT OR UPDATE OR DELETE ON user_view
    FOR EACH STATEMENT
    EXECUTE FUNCTION refresh_department_stats(); 
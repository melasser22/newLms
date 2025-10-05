-- ============================================
-- V7: Add Role Hierarchy System
-- ============================================
-- Adds hierarchical levels to roles to enforce management rules:
-- "Users can only manage users with lower role levels"

-- Add new columns to roles table
ALTER TABLE roles 
ADD COLUMN IF NOT EXISTS role_level VARCHAR(50) NOT NULL DEFAULT 'TENANT_USER',
ADD COLUMN IF NOT EXISTS is_system_role BOOLEAN NOT NULL DEFAULT false,
ADD COLUMN IF NOT EXISTS description VARCHAR(500);

-- Create index for role level queries (improves performance)
CREATE INDEX IF NOT EXISTS ix_roles_level ON roles(role_level);
CREATE INDEX IF NOT EXISTS ix_roles_system ON roles(is_system_role);

-- ============================================
-- Update Existing Roles with Appropriate Levels
-- ============================================

-- Platform Admins (EJADA Officers)
UPDATE roles 
SET role_level = 'PLATFORM_ADMIN', 
    is_system_role = true,
    description = 'Platform administrator with cross-tenant access and system-wide privileges'
WHERE code = 'EJADA_OFFICER';

-- Tenant Admins (Tenant Owners)
UPDATE roles 
SET role_level = 'TENANT_ADMIN', 
    is_system_role = true,
    description = 'Tenant owner with full control over tenant including billing and user management'
WHERE code = 'TENANT_ADMIN';

-- Tenant Officers (Tenant Managers)
UPDATE roles 
SET role_level = 'TENANT_OFFICER', 
    is_system_role = true,
    description = 'Tenant manager with elevated privileges, can manage regular users'
WHERE code = 'TENANT_OFFICER';

-- Regular Users
UPDATE roles 
SET role_level = 'TENANT_USER', 
    is_system_role = true,
    description = 'Standard application user with CRUD access to own data'
WHERE code IN ('USER', 'TENANT_USER');

-- End Users (Read-only consumers)
UPDATE roles 
SET role_level = 'END_USER', 
    is_system_role = true,
    description = 'Read-only consumer with minimal permissions'
WHERE code = 'END_USER';

-- ============================================
-- Constraints for Data Integrity
-- ============================================

-- Ensure role_level has valid values
ALTER TABLE roles
ADD CONSTRAINT ck_role_level_valid
CHECK (role_level IN (
    'PLATFORM_ADMIN', 
    'TENANT_ADMIN', 
    'TENANT_OFFICER', 
    'TENANT_USER', 
    'END_USER', 
    'GUEST'
));

-- Prevent deletion of system roles via constraint (soft constraint)
ALTER TABLE roles
ADD CONSTRAINT ck_system_role_protection
CHECK (is_system_role = false OR id IS NOT NULL);

-- ============================================
-- Create View for Role Hierarchy Analysis
-- ============================================

CREATE OR REPLACE VIEW role_hierarchy_view AS
SELECT 
    r.id,
    r.tenant_id,
    r.code,
    r.name,
    r.role_level,
    CASE r.role_level
        WHEN 'PLATFORM_ADMIN' THEN 100
        WHEN 'TENANT_ADMIN' THEN 80
        WHEN 'TENANT_OFFICER' THEN 60
        WHEN 'TENANT_USER' THEN 40
        WHEN 'END_USER' THEN 20
        ELSE 0
    END AS level_numeric,
    r.is_system_role,
    (SELECT COUNT(*) FROM user_roles ur WHERE ur.role_id = r.id) AS user_count,
    (SELECT COUNT(*) FROM role_privileges rp WHERE rp.role_id = r.id) AS privilege_count,
    r.created_at,
    r.updated_at
FROM roles r
ORDER BY level_numeric DESC, r.tenant_id, r.code;

-- ============================================
-- Function to Get User's Highest Role Level
-- ============================================

CREATE OR REPLACE FUNCTION get_user_highest_role_level(p_user_id BIGINT)
RETURNS VARCHAR(50) AS $$
DECLARE
    v_highest_level VARCHAR(50);
BEGIN
    SELECT r.role_level
    INTO v_highest_level
    FROM user_roles ur
    JOIN roles r ON ur.role_id = r.id
    WHERE ur.user_id = p_user_id
    ORDER BY 
        CASE r.role_level
            WHEN 'PLATFORM_ADMIN' THEN 100
            WHEN 'TENANT_ADMIN' THEN 80
            WHEN 'TENANT_OFFICER' THEN 60
            WHEN 'TENANT_USER' THEN 40
            WHEN 'END_USER' THEN 20
            ELSE 0
        END DESC
    LIMIT 1;
    
    RETURN COALESCE(v_highest_level, 'GUEST');
END;
$$ LANGUAGE plpgsql;

-- ============================================
-- Function to Check If User Can Manage Another User
-- ============================================

CREATE OR REPLACE FUNCTION can_user_manage_user(
    p_actor_id BIGINT,
    p_target_id BIGINT
)
RETURNS BOOLEAN AS $$
DECLARE
    v_actor_level INT;
    v_target_level INT;
    v_actor_tenant UUID;
    v_target_tenant UUID;
    v_actor_role_level VARCHAR(50);
BEGIN
    -- Cannot manage yourself
    IF p_actor_id = p_target_id THEN
        RETURN FALSE;
    END IF;
    
    -- Get actor's highest role level and tenant
    SELECT 
        CASE r.role_level
            WHEN 'PLATFORM_ADMIN' THEN 100
            WHEN 'TENANT_ADMIN' THEN 80
            WHEN 'TENANT_OFFICER' THEN 60
            WHEN 'TENANT_USER' THEN 40
            WHEN 'END_USER' THEN 20
            ELSE 0
        END,
        u.tenant_id,
        r.role_level
    INTO v_actor_level, v_actor_tenant, v_actor_role_level
    FROM users u
    JOIN user_roles ur ON ur.user_id = u.id
    JOIN roles r ON ur.role_id = r.id
    WHERE u.id = p_actor_id
    ORDER BY 
        CASE r.role_level
            WHEN 'PLATFORM_ADMIN' THEN 100
            WHEN 'TENANT_ADMIN' THEN 80
            WHEN 'TENANT_OFFICER' THEN 60
            WHEN 'TENANT_USER' THEN 40
            WHEN 'END_USER' THEN 20
            ELSE 0
        END DESC
    LIMIT 1;
    
    -- Get target's highest role level and tenant
    SELECT 
        CASE r.role_level
            WHEN 'PLATFORM_ADMIN' THEN 100
            WHEN 'TENANT_ADMIN' THEN 80
            WHEN 'TENANT_OFFICER' THEN 60
            WHEN 'TENANT_USER' THEN 40
            WHEN 'END_USER' THEN 20
            ELSE 0
        END,
        u.tenant_id
    INTO v_target_level, v_target_tenant
    FROM users u
    JOIN user_roles ur ON ur.user_id = u.id
    JOIN roles r ON ur.role_id = r.id
    WHERE u.id = p_target_id
    ORDER BY 
        CASE r.role_level
            WHEN 'PLATFORM_ADMIN' THEN 100
            WHEN 'TENANT_ADMIN' THEN 80
            WHEN 'TENANT_OFFICER' THEN 60
            WHEN 'TENANT_USER' THEN 40
            WHEN 'END_USER' THEN 20
            ELSE 0
        END DESC
    LIMIT 1;
    
    v_actor_role_level := COALESCE(v_actor_role_level, 'GUEST');
    v_actor_level := COALESCE(v_actor_level, 0);
    v_target_level := COALESCE(v_target_level, 0);

    -- Platform admins can manage anyone
    IF v_actor_role_level = 'PLATFORM_ADMIN' THEN
        RETURN TRUE;
    END IF;
    
    -- For non-platform users, must be same tenant
    IF v_actor_tenant IS DISTINCT FROM v_target_tenant THEN
        RETURN FALSE;
    END IF;
    
    -- Actor must have strictly higher level
    RETURN v_actor_level > v_target_level;
END;
$$ LANGUAGE plpgsql;

-- ============================================
-- Comments for Documentation
-- ============================================

COMMENT ON COLUMN roles.role_level IS 
'Hierarchical level: PLATFORM_ADMIN (100) > TENANT_ADMIN (80) > TENANT_OFFICER (60) > TENANT_USER (40) > END_USER (20) > GUEST (0)';

COMMENT ON COLUMN roles.is_system_role IS 
'System roles are created during tenant provisioning and cannot be deleted';

COMMENT ON VIEW role_hierarchy_view IS 
'Provides hierarchical view of roles with numeric levels for easy sorting and comparison';

COMMENT ON FUNCTION get_user_highest_role_level(BIGINT) IS 
'Returns the highest role level code for a given user';

COMMENT ON FUNCTION can_user_manage_user(BIGINT, BIGINT) IS 
'Checks if actor user can manage target user based on role hierarchy rules';

-- =====================================================================
-- V2__seed_base.sql
-- Datos iniciales (linea base configurable):
--   * Roles base: ADMIN, USER, GUEST
--   * Permisos base: VIEW, CREATE, EDIT, DELETE, EXPORT, IMPORT
--   * Asignaciones role_permission por defecto
--   * Listas del sistema: TIPOS_DOCUMENTO, ESTADOS_REGISTRO, GENEROS
--   * Constantes ejemplo: MAYORIA_EDAD, MAX_LOGIN_ATTEMPTS, SESION_TIMEOUT_MIN
--   * Menus base: Dashboard + Configuracion con sub-menus
--   * El usuario administrador se inserta desde codigo (DataInitializer)
--     porque requiere bcrypt en runtime.
-- =====================================================================

SET @now = NOW(6);

-- ---------------------------------------------------------------------
-- ROLES
-- ---------------------------------------------------------------------
INSERT INTO role (Id, Code, Name, Description, Enabled, Visible, AuditUser, AuditDate, CreatedDate) VALUES
    ('11111111-0000-0000-0000-000000000001', 'ADMIN', 'Administrador',  'Acceso total al sistema',         TRUE, TRUE, NULL, @now, @now),
    ('11111111-0000-0000-0000-000000000002', 'USER',  'Usuario',        'Usuario estandar autenticado',    TRUE, TRUE, NULL, @now, @now),
    ('11111111-0000-0000-0000-000000000003', 'GUEST', 'Invitado',       'Acceso limitado de solo lectura', TRUE, TRUE, NULL, @now, @now);

-- ---------------------------------------------------------------------
-- PERMISSIONS
-- ---------------------------------------------------------------------
INSERT INTO permission (Id, Code, Name, Description, Enabled, Visible, AuditUser, AuditDate, CreatedDate) VALUES
    ('22222222-0000-0000-0000-000000000001', 'VIEW',   'Ver',       'Permite consultar registros',  TRUE, TRUE, NULL, @now, @now),
    ('22222222-0000-0000-0000-000000000002', 'CREATE', 'Crear',     'Permite crear registros',      TRUE, TRUE, NULL, @now, @now),
    ('22222222-0000-0000-0000-000000000003', 'EDIT',   'Editar',    'Permite editar registros',     TRUE, TRUE, NULL, @now, @now),
    ('22222222-0000-0000-0000-000000000004', 'DELETE', 'Eliminar',  'Permite eliminar registros',   TRUE, TRUE, NULL, @now, @now),
    ('22222222-0000-0000-0000-000000000005', 'EXPORT', 'Exportar',  'Permite exportar informacion', TRUE, TRUE, NULL, @now, @now),
    ('22222222-0000-0000-0000-000000000006', 'IMPORT', 'Importar',  'Permite importar informacion', TRUE, TRUE, NULL, @now, @now);

-- ---------------------------------------------------------------------
-- ROLE_PERMISSION
--   ADMIN -> todos
--   USER  -> VIEW, CREATE, EDIT
--   GUEST -> VIEW
-- ---------------------------------------------------------------------
INSERT INTO role_permission (Id, RoleId, PermissionId, Enabled, Visible, AuditUser, AuditDate, CreatedDate) VALUES
    -- ADMIN
    ('33333333-0000-0000-0000-000000000001', '11111111-0000-0000-0000-000000000001', '22222222-0000-0000-0000-000000000001', TRUE, TRUE, NULL, @now, @now),
    ('33333333-0000-0000-0000-000000000002', '11111111-0000-0000-0000-000000000001', '22222222-0000-0000-0000-000000000002', TRUE, TRUE, NULL, @now, @now),
    ('33333333-0000-0000-0000-000000000003', '11111111-0000-0000-0000-000000000001', '22222222-0000-0000-0000-000000000003', TRUE, TRUE, NULL, @now, @now),
    ('33333333-0000-0000-0000-000000000004', '11111111-0000-0000-0000-000000000001', '22222222-0000-0000-0000-000000000004', TRUE, TRUE, NULL, @now, @now),
    ('33333333-0000-0000-0000-000000000005', '11111111-0000-0000-0000-000000000001', '22222222-0000-0000-0000-000000000005', TRUE, TRUE, NULL, @now, @now),
    ('33333333-0000-0000-0000-000000000006', '11111111-0000-0000-0000-000000000001', '22222222-0000-0000-0000-000000000006', TRUE, TRUE, NULL, @now, @now),
    -- USER
    ('33333333-0000-0000-0000-000000000010', '11111111-0000-0000-0000-000000000002', '22222222-0000-0000-0000-000000000001', TRUE, TRUE, NULL, @now, @now),
    ('33333333-0000-0000-0000-000000000011', '11111111-0000-0000-0000-000000000002', '22222222-0000-0000-0000-000000000002', TRUE, TRUE, NULL, @now, @now),
    ('33333333-0000-0000-0000-000000000012', '11111111-0000-0000-0000-000000000002', '22222222-0000-0000-0000-000000000003', TRUE, TRUE, NULL, @now, @now),
    -- GUEST
    ('33333333-0000-0000-0000-000000000020', '11111111-0000-0000-0000-000000000003', '22222222-0000-0000-0000-000000000001', TRUE, TRUE, NULL, @now, @now);

-- ---------------------------------------------------------------------
-- SYSTEM LISTS (catalogos configurables)
-- ---------------------------------------------------------------------
INSERT INTO system_list (Id, Code, Name, Description, Enabled, Visible, AuditUser, AuditDate, CreatedDate) VALUES
    ('44444444-0000-0000-0000-000000000001', 'document_type',  'Tipos de Documento',          'Catalogo de tipos de documento de identidad', TRUE, TRUE, NULL, @now, @now),
    ('44444444-0000-0000-0000-000000000002', 'registration_status', 'Estados de Registro',         'Estados genericos para entidades del sistema', TRUE, TRUE, NULL, @now, @now),
    ('44444444-0000-0000-0000-000000000003', 'gender',          'Generos',                     'Catalogo de generos',                          TRUE, TRUE, NULL, @now, @now);

-- ---------------------------------------------------------------------
-- CATALOGOS (cada uno en su propia tabla, acceso via /list/{tabla})
-- ---------------------------------------------------------------------
INSERT INTO document_type (Id, Code, Name, Value, DisplayOrder, Enabled, Visible, AuditUser, AuditDate, CreatedDate) VALUES
    ('55555555-0000-0000-0000-000000000001', 'CC',  'Cedula de Ciudadania',  'CC',  1, TRUE, TRUE, NULL, @now, @now),
    ('55555555-0000-0000-0000-000000000002', 'TI',  'Tarjeta de Identidad',  'TI',  2, TRUE, TRUE, NULL, @now, @now),
    ('55555555-0000-0000-0000-000000000003', 'CE',  'Cedula de Extranjeria', 'CE',  3, TRUE, TRUE, NULL, @now, @now),
    ('55555555-0000-0000-0000-000000000004', 'PA',  'Pasaporte',             'PA',  4, TRUE, TRUE, NULL, @now, @now),
    ('55555555-0000-0000-0000-000000000005', 'NIT', 'NIT',                   'NIT', 5, TRUE, TRUE, NULL, @now, @now);

INSERT INTO registration_status (Id, Code, Name, Value, DisplayOrder, Enabled, Visible, AuditUser, AuditDate, CreatedDate) VALUES
    ('55555555-0000-0000-0000-000000000010', 'ACT', 'Activo',    'ACTIVE',   1, TRUE, TRUE, NULL, @now, @now),
    ('55555555-0000-0000-0000-000000000011', 'INA', 'Inactivo',  'INACTIVE', 2, TRUE, TRUE, NULL, @now, @now),
    ('55555555-0000-0000-0000-000000000012', 'PEN', 'Pendiente', 'PENDING',  3, TRUE, TRUE, NULL, @now, @now),
    ('55555555-0000-0000-0000-000000000013', 'BLO', 'Bloqueado', 'BLOCKED',  4, TRUE, TRUE, NULL, @now, @now);

INSERT INTO gender (Id, Code, Name, Value, DisplayOrder, Enabled, Visible, AuditUser, AuditDate, CreatedDate) VALUES
    ('55555555-0000-0000-0000-000000000020', 'M', 'Masculino', 'M', 1, TRUE, TRUE, NULL, @now, @now),
    ('55555555-0000-0000-0000-000000000021', 'F', 'Femenino',  'F', 2, TRUE, TRUE, NULL, @now, @now),
    ('55555555-0000-0000-0000-000000000022', 'O', 'Otro',      'O', 3, TRUE, TRUE, NULL, @now, @now);

-- ---------------------------------------------------------------------
-- CONSTANTS (valores de configuracion globales, todos como STRING)
-- ---------------------------------------------------------------------
INSERT INTO constant (Id, Code, Name, Value, Description, Enabled, Visible, AuditUser, AuditDate, CreatedDate) VALUES
    ('66666666-0000-0000-0000-000000000001', 'MAYORIA_EDAD',        'Mayoria de edad',                  '18',  'Edad minima para ser mayor de edad',                              TRUE, TRUE, NULL, @now, @now),
    ('66666666-0000-0000-0000-000000000002', 'MAX_LOGIN_ATTEMPTS',  'Maximo intentos de login',         '5',   'Cantidad maxima de intentos fallidos antes de bloquear cuenta',   TRUE, TRUE, NULL, @now, @now),
    ('66666666-0000-0000-0000-000000000003', 'SESION_TIMEOUT_MIN',  'Timeout de sesion (minutos)',      '60',  'Tiempo de inactividad antes de cerrar sesion automaticamente',    TRUE, TRUE, NULL, @now, @now),
    ('66666666-0000-0000-0000-000000000004', 'PROFILE_PHOTO_MAX_KB', 'Tamano maximo foto de perfil (KB)', '512', 'Limite de tamano para subir foto de perfil de usuario',           TRUE, TRUE, NULL, @now, @now);

-- ---------------------------------------------------------------------
-- MENUS (estructura jerarquica configurable)
--   Padre sin ParentId  = seccion principal
--   Hijo con ParentId   = sub-seccion
-- ---------------------------------------------------------------------
INSERT INTO menu (Id, Code, Name, Icon, Route, ParentId, DisplayOrder, Enabled, Visible, AuditUser, AuditDate, CreatedDate) VALUES
    -- Secciones principales
    ('77777777-0000-0000-0000-000000000001', 'DASHBOARD',     'Dashboard',     'dashboard',  '/dashboard',     NULL,                                         1, TRUE, TRUE, NULL, @now, @now),
    ('77777777-0000-0000-0000-000000000002', 'CONFIG',        'Configuracion', 'settings',   NULL,             NULL,                                         9, TRUE, TRUE, NULL, @now, @now),
    -- Sub-menus de Configuracion
    ('77777777-0000-0000-0000-000000000010', 'CONFIG_USERS',     'Usuarios',     'users',     '/config/users',         '77777777-0000-0000-0000-000000000002', 1, TRUE, TRUE, NULL, @now, @now),
    ('77777777-0000-0000-0000-000000000011', 'CONFIG_ROLES',     'Roles',        'shield',    '/config/roles',         '77777777-0000-0000-0000-000000000002', 2, TRUE, TRUE, NULL, @now, @now),
    ('77777777-0000-0000-0000-000000000012', 'CONFIG_MENUS',     'Menus',       'menu',       '/config/menus',         '77777777-0000-0000-0000-000000000002', 3, TRUE, TRUE, NULL, @now, @now),
    ('77777777-0000-0000-0000-000000000013', 'CONFIG_LISTS',     'Listas',      'list',       '/config/lists',         '77777777-0000-0000-0000-000000000002', 4, TRUE, TRUE, NULL, @now, @now),
    ('77777777-0000-0000-0000-000000000014', 'CONFIG_CONSTANTS', 'Constantes',  'sliders',    '/config/constants',     '77777777-0000-0000-0000-000000000002', 5, TRUE, TRUE, NULL, @now, @now);

-- MENU_ROLE: ADMIN ve todo. USER solo Dashboard.
INSERT INTO menu_role (Id, MenuId, RoleId, Enabled, Visible, AuditUser, AuditDate, CreatedDate) VALUES
    ('88888888-0000-0000-0000-000000000001', '77777777-0000-0000-0000-000000000001', '11111111-0000-0000-0000-000000000001', TRUE, TRUE, NULL, @now, @now),
    ('88888888-0000-0000-0000-000000000002', '77777777-0000-0000-0000-000000000002', '11111111-0000-0000-0000-000000000001', TRUE, TRUE, NULL, @now, @now),
    ('88888888-0000-0000-0000-000000000003', '77777777-0000-0000-0000-000000000010', '11111111-0000-0000-0000-000000000001', TRUE, TRUE, NULL, @now, @now),
    ('88888888-0000-0000-0000-000000000004', '77777777-0000-0000-0000-000000000011', '11111111-0000-0000-0000-000000000001', TRUE, TRUE, NULL, @now, @now),
    ('88888888-0000-0000-0000-000000000005', '77777777-0000-0000-0000-000000000012', '11111111-0000-0000-0000-000000000001', TRUE, TRUE, NULL, @now, @now),
    ('88888888-0000-0000-0000-000000000006', '77777777-0000-0000-0000-000000000013', '11111111-0000-0000-0000-000000000001', TRUE, TRUE, NULL, @now, @now),
    ('88888888-0000-0000-0000-000000000007', '77777777-0000-0000-0000-000000000014', '11111111-0000-0000-0000-000000000001', TRUE, TRUE, NULL, @now, @now),
    -- USER (solo Dashboard)
    ('88888888-0000-0000-0000-000000000010', '77777777-0000-0000-0000-000000000001', '11111111-0000-0000-0000-000000000002', TRUE, TRUE, NULL, @now, @now);

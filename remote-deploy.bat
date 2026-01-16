@echo off
chcp 65001 > nul
setlocal enabledelayedexpansion

REM ============================================
REM REMOTE DEPLOYMENT - WINDOWS
REM Despliega usando password (SSH keys no soportadas bien en Windows)
REM ============================================

echo.
echo ╔═══════════════════════════════════════════╗
echo ║   🚀 REMOTE DEPLOYMENT - WINDOWS          ║
echo ║   Password-based SSH                      ║
echo ╚═══════════════════════════════════════════╝
echo.

REM ============================================
REM CARGAR CONFIGURACIÓN
REM ============================================

set CONFIG_FILE=deploy-config.env

if not exist "%CONFIG_FILE%" (
    echo ❌ No se encontró: %CONFIG_FILE%
    echo.
    echo 📝 Creando configuración por defecto...

    (
    echo # Ver deploy-config.env completo en artifacts anteriores
    echo VPS_HOST=72.62.174.193
    echo VPS_USER=root
    echo VPS_PASSWORD=H;v1c-#-b,9DlzMRj;L3
    echo VPS_PATH=/opt/saas-platform
    echo VPS_SSH_PORT=22
    echo.
    echo DEPLOY_MYSQL=true
    echo DEPLOY_CONFIG_SERVER=true
    echo SERVICE_PORT_CONFIG_SERVER=8888
    echo DEPLOY_DISCOVERY_SERVICE=true
    echo SERVICE_PORT_DISCOVERY_SERVICE=8761
    echo DEPLOY_AUTH_SERVICE=true
    echo SERVICE_PORT_AUTH_SERVICE=8082
    echo DEPLOY_SYSTEM_SERVICE=true
    echo SERVICE_PORT_SYSTEM_SERVICE=8083
    echo DEPLOY_GATEWAY_SERVICE=true
    echo SERVICE_PORT_GATEWAY_SERVICE=8080
    echo.
    echo FORCE_REBUILD=false
    echo SKIP_BACKUP=false
    echo AUTO_PULL=true
    echo GIT_BRANCH=main
    echo SHOW_LOGS=true
    ) > "%CONFIG_FILE%"

    echo ✅ Archivo creado: %CONFIG_FILE%
    echo.
    set /p EDIT="¿Editar ahora? (s/n): "
    if /i "!EDIT!"=="s" notepad "%CONFIG_FILE%"
)

REM Cargar variables
for /f "tokens=1,2 delims==" %%a in ('type "%CONFIG_FILE%" ^| findstr /v "^#" ^| findstr "="') do (
    set KEY=%%a
    set VALUE=%%b
    set KEY=!KEY: =!
    set VALUE=!VALUE: =!

    if "!KEY!"=="VPS_HOST" set VPS_HOST=!VALUE!
    if "!KEY!"=="VPS_USER" set VPS_USER=!VALUE!
    if "!KEY!"=="VPS_PASSWORD" set VPS_PASSWORD=!VALUE!
    if "!KEY!"=="VPS_PATH" set VPS_PATH=!VALUE!
    if "!KEY!"=="VPS_SSH_PORT" set VPS_SSH_PORT=!VALUE!
)

REM Validar
if not defined VPS_HOST (
    echo ❌ VPS_HOST no definido en %CONFIG_FILE%
    pause
    exit /b 1
)

if not defined VPS_PASSWORD (
    echo ❌ VPS_PASSWORD no definido en %CONFIG_FILE%
    pause
    exit /b 1
)

echo ✅ Configuración cargada
echo    Host: %VPS_HOST%
echo    User: %VPS_USER%
echo.

REM ============================================
REM VERIFICAR OPENSSH
REM ============================================

where ssh >nul 2>&1
if %errorlevel% neq 0 (
    echo ❌ ERROR: OpenSSH no instalado
    echo.
    echo 💡 Instala OpenSSH Client desde:
    echo    Configuración ^> Aplicaciones ^> Características opcionales
    pause
    exit /b 1
)

echo ✅ OpenSSH encontrado
echo.

REM ============================================
REM DETECTAR SERVICIOS
REM ============================================

echo 📋 Detectando servicios habilitados...
set SERVICE_COUNT=0

for /f "tokens=1,2 delims==" %%a in ('type "%CONFIG_FILE%" ^| findstr /B "DEPLOY_"') do (
    set KEY=%%a
    set VALUE=%%b
    set VALUE=!VALUE: =!
    set VALUE=!VALUE:"=!

    if /i "!VALUE!"=="true" (
        set /a SERVICE_COUNT+=1
        echo    ✓ !KEY!
    )
)

if %SERVICE_COUNT% equ 0 (
    echo ❌ No hay servicios habilitados
    pause
    exit /b 1
)

echo.
echo 📊 Total: %SERVICE_COUNT% servicios
echo.

set /p CONFIRM="¿Continuar? (s/n): "
if /i not "%CONFIRM%"=="s" (
    echo ❌ Cancelado
    exit /b 0
)

REM ============================================
REM MÉTODO: PLINK (si está disponible)
REM ============================================

where plink >nul 2>&1
if %errorlevel% equ 0 (
    set USE_PLINK=true
    echo ✅ Usando plink para automatizar password
) else (
    set USE_PLINK=false
    echo ℹ️  plink no disponible, se usará método interactivo
)

REM ============================================
REM VERIFICAR CONEXIÓN
REM ============================================

echo.
echo 1️⃣  Verificando conexión...

if "%USE_PLINK%"=="true" (
    echo | set /p="Probando con plink... "
    plink -batch -pw %VPS_PASSWORD% %VPS_USER%@%VPS_HOST% "echo OK" >nul 2>&1
    if %errorlevel% equ 0 (
        echo ✅ OK
    ) else (
        echo ⚠️  Falló, usando SSH interactivo
        set USE_PLINK=false
    )
)

if "%USE_PLINK%"=="false" (
    echo.
    echo ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
    echo ⚠️  SE PEDIRÁ PASSWORD DEL VPS VARIAS VECES
    echo    Password: %VPS_PASSWORD%
    echo ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
    echo.
    pause
)

REM ============================================
REM SUBIR ARCHIVOS
REM ============================================

echo.
echo 2️⃣  Subiendo archivos...

if "%USE_PLINK%"=="true" (
    echo    📤 deploy-config.env...
    pscp -batch -pw %VPS_PASSWORD% "%CONFIG_FILE%" %VPS_USER%@%VPS_HOST%:%VPS_PATH%/ >nul

    echo    📤 deploy.sh...
    pscp -batch -pw %VPS_PASSWORD% deploy.sh %VPS_USER%@%VPS_HOST%:%VPS_PATH%/ >nul
) else (
    echo    📤 deploy-config.env...
    echo    (Password: %VPS_PASSWORD%)
    scp "%CONFIG_FILE%" %VPS_USER%@%VPS_HOST%:%VPS_PATH%/

    echo.
    echo    📤 deploy.sh...
    echo    (Password: %VPS_PASSWORD%)
    scp deploy.sh %VPS_USER%@%VPS_HOST%:%VPS_PATH%/
)

echo ✅ Archivos subidos
echo.

REM ============================================
REM EJECUTAR DEPLOYMENT
REM ============================================

echo 3️⃣  Configurando permisos...

if "%USE_PLINK%"=="true" (
    plink -batch -pw %VPS_PASSWORD% %VPS_USER%@%VPS_HOST% "cd %VPS_PATH% && chmod +x deploy.sh" >nul
) else (
    ssh %VPS_USER%@%VPS_HOST% "cd %VPS_PATH% && chmod +x deploy.sh"
)

echo ✅ Permisos OK
echo.

echo ╔═══════════════════════════════════════════╗
echo ║   🚀 EJECUTANDO DEPLOYMENT EN VPS         ║
echo ╚═══════════════════════════════════════════╝
echo.

if "%USE_PLINK%"=="true" (
    plink -batch -pw %VPS_PASSWORD% %VPS_USER%@%VPS_HOST% "cd %VPS_PATH% && ./deploy.sh"
) else (
    echo (Password: %VPS_PASSWORD%)
    ssh %VPS_USER%@%VPS_HOST% "cd %VPS_PATH% && ./deploy.sh"
)

set DEPLOY_EXIT=%errorlevel%

REM ============================================
REM RESULTADO
REM ============================================

echo.
echo ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

if %DEPLOY_EXIT% equ 0 (
    echo ╔═══════════════════════════════════════════╗
    echo ║   ✅ DEPLOYMENT COMPLETADO                ║
    echo ╚═══════════════════════════════════════════╝
) else (
    echo ╔═══════════════════════════════════════════╗
    echo ║   ⚠️  DEPLOYMENT CON ADVERTENCIAS         ║
    echo ╚═══════════════════════════════════════════╝
)

echo ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
echo.
echo 📝 Comandos útiles:
echo.
echo 🔍 Ver logs:
echo    ssh %VPS_USER%@%VPS_HOST% "cd %VPS_PATH% && docker compose logs -f"
echo.
echo 📊 Ver estado:
echo    ssh %VPS_USER%@%VPS_HOST% "cd %VPS_PATH% && docker compose ps"
echo.
pause

exit /b %DEPLOY_EXIT%
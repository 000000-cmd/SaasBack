@echo off
chcp 65001 > nul
setlocal enabledelayedexpansion

REM ============================================
REM DESPLIEGUE RÁPIDO - WINDOWS (CON PASSWORD)
REM ============================================

echo.
echo ╔═══════════════════════════════════════════╗
echo ║   🚀 DESPLIEGUE RÁPIDO - WINDOWS          ║
echo ╚═══════════════════════════════════════════╝
echo.

REM ============================================
REM CARGAR CREDENCIALES
REM ============================================

set CREDENTIALS_FILE=credentials.conf

if not exist "%CREDENTIALS_FILE%" (
    echo ❌ No se encontró %CREDENTIALS_FILE%
    echo.
    echo 📝 Creando archivo de credenciales...

    (
    echo # ============================================
    echo # CREDENCIALES VPS
    echo # ============================================
    echo VPS_HOST=72.62.174.193
    echo VPS_USER=root
    echo VPS_PASSWORD=H;v1c-#-b,9DlzMRj;L3
    echo VPS_PATH=/opt/saas-platform
    echo VPS_SSH_PORT=22
    ) > "%CREDENTIALS_FILE%"

    echo ✅ Archivo creado: %CREDENTIALS_FILE%
    echo.
)

REM Cargar credenciales
for /f "tokens=1,2 delims==" %%a in ('type "%CREDENTIALS_FILE%" ^| findstr /v "^#" ^| findstr "="') do (
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

REM Verificar que se cargaron las credenciales
if not defined VPS_HOST (
    echo ❌ Error: No se pudo cargar VPS_HOST
    pause
    exit /b 1
)

if not defined VPS_PASSWORD (
    echo ❌ Error: No se pudo cargar VPS_PASSWORD
    pause
    exit /b 1
)

echo ✅ Credenciales cargadas
echo    Host: %VPS_HOST%
echo    User: %VPS_USER%
echo.

REM ============================================
REM VERIFICAR DEPENDENCIAS
REM ============================================

REM Verificar si sshpass está disponible (probablemente no en Windows)
where sshpass >nul 2>&1
if %errorlevel% equ 0 (
    set USE_SSHPASS=true
    echo ✅ sshpass encontrado
) else (
    set USE_SSHPASS=false
    echo ℹ️  sshpass no disponible, se usará método alternativo
)

REM Verificar OpenSSH
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
REM CARGAR CONFIGURACIÓN DE SERVICIOS
REM ============================================

set CONFIG_FILE=deploy-config.env

if not exist "%CONFIG_FILE%" (
    echo 📝 Creando configuración de servicios por defecto...

    (
    echo # Servidor VPS ^(se carga desde credentials.conf^)
    echo.
    echo # Infraestructura
    echo DEPLOY_MYSQL=true
    echo DEPLOY_CONFIG_SERVER=true
    echo SERVICE_PORT_CONFIG_SERVER=8888
    echo DEPLOY_DISCOVERY_SERVICE=true
    echo SERVICE_PORT_DISCOVERY_SERVICE=8761
    echo.
    echo # Microservicios
    echo DEPLOY_AUTH_SERVICE=true
    echo SERVICE_PORT_AUTH_SERVICE=8082
    echo DEPLOY_SYSTEM_SERVICE=true
    echo SERVICE_PORT_SYSTEM_SERVICE=8083
    echo DEPLOY_GATEWAY_SERVICE=true
    echo SERVICE_PORT_GATEWAY_SERVICE=8080
    echo.
    echo # Opciones
    echo FORCE_REBUILD=false
    echo SKIP_BACKUP=false
    echo AUTO_PULL=true
    echo GIT_BRANCH=main
    echo SHOW_LOGS=true
    ) > "%CONFIG_FILE%"

    echo ✅ Archivo creado: %CONFIG_FILE%
    echo.
    echo ⚠️  Edita el archivo si necesitas cambiar servicios
    echo.
    set /p EDIT_NOW="¿Editar ahora? (s/n): "
    if /i "!EDIT_NOW!"=="s" (
        notepad "%CONFIG_FILE%"
    )
)

REM Detectar servicios habilitados
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
echo 📊 Total servicios: %SERVICE_COUNT%
echo.

REM Confirmar
set /p CONFIRM="¿Continuar con el despliegue? (s/n): "
if /i not "%CONFIRM%"=="s" (
    echo ❌ Cancelado
    exit /b 0
)

REM ============================================
REM PREPARAR SCRIPTS PARA SUBIR
REM ============================================

echo.
echo 📦 Preparando archivos para subir...

REM Crear script temporal que usa las credenciales
set TEMP_SCRIPT=%TEMP%\deploy-with-creds.sh

(
echo #!/bin/bash
echo VPS_HOST="%VPS_HOST%"
echo VPS_USER="%VPS_USER%"
echo VPS_PATH="%VPS_PATH%"
echo cd "%VPS_PATH%"
echo ./deploy-selective.sh
) > "%TEMP_SCRIPT%"

REM ============================================
REM MÉTODO 1: Usando expect (si está disponible)
REM ============================================

echo.
echo 🔄 Intentando conexión al servidor...
echo.

REM Crear archivo expect para automatizar password
set EXPECT_SCRIPT=%TEMP%\ssh-expect.exp

(
echo #!/usr/bin/expect -f
echo set timeout 30
echo set password "%VPS_PASSWORD%"
echo spawn ssh -o StrictHostKeyChecking=no -p %VPS_SSH_PORT% %VPS_USER%@%VPS_HOST% "cd %VPS_PATH% && ./deploy-selective.sh"
echo expect {
echo   "password:" {
echo     send "$password\r"
echo     exp_continue
echo   }
echo   eof
echo }
) > "%EXPECT_SCRIPT%"

REM ============================================
REM MÉTODO 2: Usar PowerShell con SecureString
REM ============================================

echo 1️⃣  Verificando conexión...

REM Usar PowerShell para conexión con password
powershell -Command ^
  "$pass = ConvertTo-SecureString '%VPS_PASSWORD%' -AsPlainText -Force; ^
   $cred = New-Object System.Management.Automation.PSCredential ('%VPS_USER%', $pass); ^
   try { ^
     $result = ssh -o StrictHostKeyChecking=no %VPS_USER%@%VPS_HOST% 'echo OK' 2>&1; ^
     if ($result -match 'OK') { exit 0 } else { exit 1 } ^
   } catch { exit 1 }"

if %errorlevel% neq 0 (
    echo.
    echo ⚠️  Conexión SSH directa no funcionó
    echo 💡 Probando método alternativo con password interactivo...
    echo.
    echo ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
    echo ⚠️  SE TE PEDIRÁ LA PASSWORD DEL VPS
    echo    Password: %VPS_PASSWORD%
    echo    ^(Copia y pega cuando se pida^)
    echo ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
    echo.
    pause
)

REM ============================================
REM SUBIR ARCHIVOS
REM ============================================

echo.
echo 2️⃣  Subiendo archivos al servidor...

REM Crear archivo temporal con credenciales para el servidor
set SERVER_CREDS=%TEMP%\server-credentials.conf
copy /Y "%CREDENTIALS_FILE%" "%SERVER_CREDS%" >nul

echo    📤 Subiendo credentials.conf...
echo | set /p="Password: %VPS_PASSWORD%" | pscp -pw %VPS_PASSWORD% -batch "%SERVER_CREDS%" %VPS_USER%@%VPS_HOST%:%VPS_PATH%/credentials.conf 2>nul

if %errorlevel% neq 0 (
    REM Si pscp no está disponible, usar método manual
    echo.
    echo ⚠️  pscp no disponible, usando scp interactivo...
    echo    📤 Subiendo credentials.conf...
    echo    Password: %VPS_PASSWORD%
    scp -o StrictHostKeyChecking=no "%SERVER_CREDS%" %VPS_USER%@%VPS_HOST%:%VPS_PATH%/credentials.conf
)

echo    📤 Subiendo deploy-config.env...
scp -o StrictHostKeyChecking=no "%CONFIG_FILE%" %VPS_USER%@%VPS_HOST%:%VPS_PATH%/deploy-config.env 2>nul

echo    📤 Subiendo deploy-selective.sh...
scp -o StrictHostKeyChecking=no deploy-selective.sh %VPS_USER%@%VPS_HOST%:%VPS_PATH%/deploy-selective.sh 2>nul

if %errorlevel% neq 0 (
    echo.
    echo ❌ Error al subir archivos
    echo 💡 Se te pedirá la password para cada archivo:
    echo    Password: %VPS_PASSWORD%
    echo.
    pause

    echo    📤 Subiendo credentials.conf...
    scp -o StrictHostKeyChecking=no "%SERVER_CREDS%" %VPS_USER%@%VPS_HOST%:%VPS_PATH%/credentials.conf

    echo    📤 Subiendo deploy-config.env...
    scp -o StrictHostKeyChecking=no "%CONFIG_FILE%" %VPS_USER%@%VPS_HOST%:%VPS_PATH%/deploy-config.env

    echo    📤 Subiendo deploy-selective.sh...
    scp -o StrictHostKeyChecking=no deploy-selective.sh %VPS_USER%@%VPS_HOST%:%VPS_PATH%/deploy-selective.sh
)

echo ✅ Archivos subidos
echo.

REM ============================================
REM DAR PERMISOS Y EJECUTAR
REM ============================================

echo 3️⃣  Configurando permisos...
ssh -o StrictHostKeyChecking=no %VPS_USER%@%VPS_HOST% "cd %VPS_PATH% && chmod +x deploy-selective.sh" 2>nul
echo ✅ Permisos configurados
echo.

REM ============================================
REM EJECUTAR DESPLIEGUE
REM ============================================

echo.
echo ╔═══════════════════════════════════════════╗
echo ║   🚀 EJECUTANDO DESPLIEGUE EN VPS         ║
echo ╚═══════════════════════════════════════════╝
echo.
echo 💡 Se te pedirá la password del VPS:
echo    Password: %VPS_PASSWORD%
echo.
pause

ssh -o StrictHostKeyChecking=no %VPS_USER%@%VPS_HOST% "cd %VPS_PATH% && ./deploy-selective.sh"

set DEPLOY_EXIT=%errorlevel%

REM ============================================
REM RESULTADO
REM ============================================

echo.
echo ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

if %DEPLOY_EXIT% equ 0 (
    echo ╔═══════════════════════════════════════════╗
    echo ║   ✅ DESPLIEGUE COMPLETADO                ║
    echo ╚═══════════════════════════════════════════╝
) else (
    echo ╔═══════════════════════════════════════════╗
    echo ║   ⚠️  DESPLIEGUE CON ADVERTENCIAS         ║
    echo ╚═══════════════════════════════════════════╝
)

echo ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
echo.
echo 📝 Para ver logs:
echo    ssh %VPS_USER%@%VPS_HOST% "cd %VPS_PATH% && docker compose logs -f"
echo    ^(Password: %VPS_PASSWORD%^)
echo.
pause

REM Limpiar archivos temporales
if exist "%TEMP_SCRIPT%" del /F /Q "%TEMP_SCRIPT%" >nul 2>&1
if exist "%EXPECT_SCRIPT%" del /F /Q "%EXPECT_SCRIPT%" >nul 2>&1
if exist "%SERVER_CREDS%" del /F /Q "%SERVER_CREDS%" >nul 2>&1

exit /b %DEPLOY_EXIT%
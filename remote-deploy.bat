@echo off
chcp 65001 > nul
setlocal enabledelayedexpansion

REM ============================================
REM REMOTE DEPLOYMENT - WINDOWS
REM Despliega usando password (o plink para automatizar)
REM ============================================
REM Uso:
REM   remote-deploy.bat              - Deploy normal
REM   remote-deploy.bat --status     - Ver estado
REM   remote-deploy.bat --logs       - Ver logs
REM ============================================

echo.
echo ================================================================
echo    REMOTE DEPLOYMENT - SAAS PLATFORM (Windows)
echo ================================================================
echo.

REM ============================================
REM CARGAR CONFIGURACION
REM ============================================

set CONFIG_FILE=deploy-config.env

if not exist "%CONFIG_FILE%" (
    echo [ERROR] No se encontro: %CONFIG_FILE%
    echo.
    echo Creando configuracion por defecto...

    (
    echo # Configuracion de deployment
    echo VPS_HOST=tu-servidor.com
    echo VPS_USER=root
    echo VPS_PASSWORD=tu_password_aqui
    echo VPS_PATH=/opt/saas-platform
    echo VPS_SSH_PORT=22
    echo.
    echo FORCE_REBUILD=false
    echo SKIP_BACKUP=false
    echo AUTO_PULL=true
    echo GIT_BRANCH=main
    ) > "%CONFIG_FILE%"

    echo Archivo creado: %CONFIG_FILE%
    echo Por favor, edita el archivo con tu configuracion
    pause
    exit /b 1
)

REM Cargar variables del archivo de configuracion
for /f "tokens=1,2 delims==" %%a in ('type "%CONFIG_FILE%" ^| findstr /v "^#" ^| findstr "="') do (
    set "KEY=%%a"
    set "VALUE=%%b"
    REM Limpiar espacios
    set "KEY=!KEY: =!"
    for /f "tokens=* delims= " %%c in ("!VALUE!") do set "VALUE=%%c"

    if "!KEY!"=="VPS_HOST" set "VPS_HOST=!VALUE!"
    if "!KEY!"=="VPS_USER" set "VPS_USER=!VALUE!"
    if "!KEY!"=="VPS_PASSWORD" set "VPS_PASSWORD=!VALUE!"
    if "!KEY!"=="VPS_PATH" set "VPS_PATH=!VALUE!"
    if "!KEY!"=="VPS_SSH_PORT" set "VPS_SSH_PORT=!VALUE!"
)

REM Validar configuracion
if not defined VPS_HOST (
    echo [ERROR] VPS_HOST no definido en %CONFIG_FILE%
    pause
    exit /b 1
)

if not defined VPS_PASSWORD (
    echo [ERROR] VPS_PASSWORD no definido en %CONFIG_FILE%
    echo En Windows se requiere el password para conexion SSH
    pause
    exit /b 1
)

if not defined VPS_SSH_PORT set VPS_SSH_PORT=22

echo [OK] Configuracion cargada
echo     Host: %VPS_HOST%
echo     User: %VPS_USER%
echo     Path: %VPS_PATH%
echo.

REM ============================================
REM PROCESAR ARGUMENTOS
REM ============================================

if "%1"=="--status" goto :show_status
if "%1"=="--logs" goto :show_logs
if "%1"=="--help" goto :show_help
if "%1"=="-h" goto :show_help

REM Si no hay argumentos, ejecutar deployment
goto :run_deployment

REM ============================================
REM FUNCIONES
REM ============================================

:show_help
echo Uso: %~nx0 [opcion]
echo.
echo Opciones:
echo   (sin argumentos)   Ejecutar deployment completo
echo   --status           Ver estado de servicios
echo   --logs             Ver logs de todos los servicios
echo   --help, -h         Mostrar esta ayuda
echo.
goto :eof

:show_status
echo.
echo Consultando estado de servicios...
echo.

where plink >nul 2>&1
if %errorlevel% equ 0 (
    plink -batch -pw %VPS_PASSWORD% %VPS_USER%@%VPS_HOST% -P %VPS_SSH_PORT% "cd %VPS_PATH% && docker compose ps"
) else (
    echo [INFO] Se pedira el password: %VPS_PASSWORD%
    ssh -p %VPS_SSH_PORT% %VPS_USER%@%VPS_HOST% "cd %VPS_PATH% && docker compose ps"
)
goto :eof

:show_logs
echo.
echo Mostrando logs (Ctrl+C para salir)...
echo.

where plink >nul 2>&1
if %errorlevel% equ 0 (
    plink -batch -pw %VPS_PASSWORD% %VPS_USER%@%VPS_HOST% -P %VPS_SSH_PORT% "cd %VPS_PATH% && docker compose logs -f --tail=100"
) else (
    echo [INFO] Se pedira el password: %VPS_PASSWORD%
    ssh -p %VPS_SSH_PORT% %VPS_USER%@%VPS_HOST% "cd %VPS_PATH% && docker compose logs -f --tail=100"
)
goto :eof

:run_deployment

REM ============================================
REM VERIFICAR OPENSSH
REM ============================================

where ssh >nul 2>&1
if %errorlevel% neq 0 (
    echo [ERROR] OpenSSH no esta instalado
    echo.
    echo Instala OpenSSH Client desde:
    echo   Configuracion ^> Aplicaciones ^> Caracteristicas opcionales
    echo.
    echo O instala PuTTY/plink para automatizar con password
    pause
    exit /b 1
)

echo [OK] OpenSSH encontrado
echo.

REM ============================================
REM DETECTAR SERVICIOS
REM ============================================

echo Detectando servicios habilitados...
set SERVICE_COUNT=0

for /f "tokens=1,2 delims==" %%a in ('type "%CONFIG_FILE%" ^| findstr /B "DEPLOY_"') do (
    set "KEY=%%a"
    set "VALUE=%%b"
    set "VALUE=!VALUE: =!"
    set "VALUE=!VALUE:"=!"

    if /i "!VALUE!"=="true" (
        set /a SERVICE_COUNT+=1
        echo    [x] !KEY!
    )
)

if %SERVICE_COUNT% equ 0 (
    echo [ERROR] No hay servicios habilitados
    pause
    exit /b 1
)

echo.
echo Total: %SERVICE_COUNT% servicios
echo.

set /p CONFIRM="Continuar con el deployment? (s/n): "
if /i not "%CONFIRM%"=="s" (
    echo Deployment cancelado
    exit /b 0
)

REM ============================================
REM DETECTAR PLINK PARA AUTOMATIZACION
REM ============================================

where plink >nul 2>&1
if %errorlevel% equ 0 (
    set USE_PLINK=true
    echo [OK] Usando plink para automatizar password
) else (
    set USE_PLINK=false
    echo [INFO] plink no disponible, se usara metodo interactivo
    echo.
    echo ================================================================
    echo    SE PEDIRA PASSWORD VARIAS VECES
    echo    Password: %VPS_PASSWORD%
    echo ================================================================
    echo.
    pause
)

REM ============================================
REM VERIFICAR CONEXION
REM ============================================

echo.
echo [1/4] Verificando conexion...

if "%USE_PLINK%"=="true" (
    plink -batch -pw %VPS_PASSWORD% %VPS_USER%@%VPS_HOST% -P %VPS_SSH_PORT% "echo OK" >nul 2>&1
    if %errorlevel% equ 0 (
        echo [OK] Conexion verificada
    ) else (
        echo [ERROR] No se pudo conectar
        echo Verifica host, usuario y password
        pause
        exit /b 1
    )
) else (
    echo Verificando conexion (password: %VPS_PASSWORD%)
    ssh -p %VPS_SSH_PORT% %VPS_USER%@%VPS_HOST% "echo OK"
    if %errorlevel% neq 0 (
        echo [ERROR] No se pudo conectar
        pause
        exit /b 1
    )
)

REM ============================================
REM SUBIR ARCHIVOS
REM ============================================

echo.
echo [2/4] Subiendo archivos...

if "%USE_PLINK%"=="true" (
    echo    Subiendo deploy-config.env...
    pscp -batch -pw %VPS_PASSWORD% -P %VPS_SSH_PORT% "%CONFIG_FILE%" %VPS_USER%@%VPS_HOST%:%VPS_PATH%/

    if exist deploy.sh (
        echo    Subiendo deploy.sh...
        pscp -batch -pw %VPS_PASSWORD% -P %VPS_SSH_PORT% deploy.sh %VPS_USER%@%VPS_HOST%:%VPS_PATH%/
    )
) else (
    echo    Subiendo deploy-config.env (password: %VPS_PASSWORD%)
    scp -P %VPS_SSH_PORT% "%CONFIG_FILE%" %VPS_USER%@%VPS_HOST%:%VPS_PATH%/

    if exist deploy.sh (
        echo    Subiendo deploy.sh (password: %VPS_PASSWORD%)
        scp -P %VPS_SSH_PORT% deploy.sh %VPS_USER%@%VPS_HOST%:%VPS_PATH%/
    )
)

echo [OK] Archivos subidos
echo.

REM ============================================
REM CONFIGURAR PERMISOS
REM ============================================

echo [3/4] Configurando permisos...

if "%USE_PLINK%"=="true" (
    plink -batch -pw %VPS_PASSWORD% %VPS_USER%@%VPS_HOST% -P %VPS_SSH_PORT% "cd %VPS_PATH% && chmod +x deploy.sh"
) else (
    ssh -p %VPS_SSH_PORT% %VPS_USER%@%VPS_HOST% "cd %VPS_PATH% && chmod +x deploy.sh"
)

echo [OK] Permisos configurados
echo.

REM ============================================
REM EJECUTAR DEPLOYMENT
REM ============================================

echo [4/4] Ejecutando deployment en VPS...
echo.
echo ================================================================
echo    EJECUTANDO DEPLOYMENT EN VPS
echo ================================================================
echo.

if "%USE_PLINK%"=="true" (
    plink -batch -pw %VPS_PASSWORD% %VPS_USER%@%VPS_HOST% -P %VPS_SSH_PORT% "cd %VPS_PATH% && ./deploy.sh"
) else (
    ssh -p %VPS_SSH_PORT% %VPS_USER%@%VPS_HOST% "cd %VPS_PATH% && ./deploy.sh"
)

set DEPLOY_EXIT=%errorlevel%

REM ============================================
REM RESULTADO
REM ============================================

echo.
echo ================================================================

if %DEPLOY_EXIT% equ 0 (
    echo    DEPLOYMENT COMPLETADO EXITOSAMENTE
) else (
    echo    DEPLOYMENT COMPLETADO CON ADVERTENCIAS
)

echo ================================================================
echo.
echo Comandos utiles:
echo   Ver estado: %~nx0 --status
echo   Ver logs:   %~nx0 --logs
echo.

pause
exit /b %DEPLOY_EXIT%
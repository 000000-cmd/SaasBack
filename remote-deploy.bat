@echo off
REM ============================================
REM REMOTE DEPLOY - SAAS PLATFORM (Windows)
REM ============================================

setlocal EnableDelayedExpansion

REM Configuración - MODIFICAR SEGÚN TU SERVIDOR
set "REMOTE_HOST=tu-servidor.com"
set "REMOTE_USER=root"
set "REMOTE_PORT=22"
set "REMOTE_DIR=/opt/saas-platform"

REM Cargar configuración desde archivo si existe
if exist "deploy-config.env" (
    for /f "usebackq tokens=1,2 delims==" %%a in ("deploy-config.env") do (
        set "%%a=%%b"
    )
)

REM Colores (usando ANSI codes)
set "GREEN=[92m"
set "RED=[91m"
set "YELLOW=[93m"
set "CYAN=[96m"
set "NC=[0m"

if "%1"=="" goto :help
if "%1"=="help" goto :help
if "%1"=="--help" goto :help
if "%1"=="-h" goto :help
if "%1"=="full" goto :full_deploy
if "%1"=="quick" goto :quick_deploy
if "%1"=="status" goto :status
if "%1"=="logs" goto :logs
if "%1"=="stop" goto :stop
if "%1"=="restart" goto :restart
if "%1"=="rebuild" goto :rebuild

echo %RED%Comando desconocido: %1%NC%
goto :help

:full_deploy
echo %CYAN%[DEPLOY] Iniciando FULL DEPLOY en %REMOTE_HOST%...%NC%
echo.

ssh -p %REMOTE_PORT% %REMOTE_USER%@%REMOTE_HOST% "cd %REMOTE_DIR% && git fetch origin && git reset --hard origin/main && git clean -fd && docker compose down --rmi all --volumes --remove-orphans && docker system prune -f && docker compose build --no-cache && docker compose up -d"

echo.
echo %CYAN%[DEPLOY] Esperando que los servicios inicien...%NC%
timeout /t 30 /nobreak > nul

call :status
echo.
echo %GREEN%FULL DEPLOY completado!%NC%
goto :end

:quick_deploy
echo %CYAN%[DEPLOY] Iniciando QUICK DEPLOY en %REMOTE_HOST%...%NC%
echo.

ssh -p %REMOTE_PORT% %REMOTE_USER%@%REMOTE_HOST% "cd %REMOTE_DIR% && git fetch origin && git reset --hard origin/main && git clean -fd && docker compose down && docker compose up -d"

echo.
echo %CYAN%[DEPLOY] Esperando que los servicios inicien...%NC%
timeout /t 20 /nobreak > nul

call :status
echo.
echo %GREEN%QUICK DEPLOY completado!%NC%
goto :end

:rebuild
if "%2"=="" (
    echo %RED%Error: Especifica un servicio%NC%
    echo Uso: %0 rebuild ^<service^>
    echo Ejemplo: %0 rebuild auth-service
    goto :end
)
echo %CYAN%[DEPLOY] Reconstruyendo %2 en %REMOTE_HOST%...%NC%
ssh -p %REMOTE_PORT% %REMOTE_USER%@%REMOTE_HOST% "cd %REMOTE_DIR% && git fetch origin && git reset --hard origin/main && docker compose build --no-cache %2 && docker compose up -d %2"
echo %GREEN%Servicio %2 reconstruido!%NC%
goto :end

:status
echo %CYAN%[STATUS] Estado de los servicios:%NC%
echo.
ssh -p %REMOTE_PORT% %REMOTE_USER%@%REMOTE_HOST% "docker ps --format 'table {{.Names}}\t{{.Status}}\t{{.Ports}}' | grep -E 'saas-|NAMES'"
echo.
echo %CYAN%URLs de acceso:%NC%
echo   - Gateway:   http://%REMOTE_HOST%:8080
echo   - Eureka:    http://%REMOTE_HOST%:8761
echo   - Config:    http://%REMOTE_HOST%:8888
goto :end

:logs
echo %CYAN%[LOGS] Mostrando logs...%NC%
if "%2"=="" (
    ssh -p %REMOTE_PORT% %REMOTE_USER%@%REMOTE_HOST% "cd %REMOTE_DIR% && docker compose logs -f --tail=50"
) else (
    ssh -p %REMOTE_PORT% %REMOTE_USER%@%REMOTE_HOST% "cd %REMOTE_DIR% && docker compose logs -f --tail=100 %2"
)
goto :end

:stop
echo %CYAN%[STOP] Deteniendo servicios...%NC%
ssh -p %REMOTE_PORT% %REMOTE_USER%@%REMOTE_HOST% "cd %REMOTE_DIR% && docker compose down"
echo %GREEN%Servicios detenidos%NC%
goto :end

:restart
echo %CYAN%[RESTART] Reiniciando servicios...%NC%
if "%2"=="" (
    ssh -p %REMOTE_PORT% %REMOTE_USER%@%REMOTE_HOST% "cd %REMOTE_DIR% && docker compose restart"
) else (
    ssh -p %REMOTE_PORT% %REMOTE_USER%@%REMOTE_HOST% "cd %REMOTE_DIR% && docker compose restart %2"
)
echo %GREEN%Servicios reiniciados%NC%
goto :end

:help
echo.
echo %CYAN%SAAS Platform - Remote Deploy (Windows)%NC%
echo.
echo Uso: %0 ^<comando^> [opciones]
echo.
echo Comandos:
echo   full              - Deploy completo (pull + rebuild + start)
echo   quick             - Quick deploy (pull + restart sin rebuild)
echo   rebuild ^<service^> - Reconstruir un servicio específico
echo   status            - Ver estado de los servicios
echo   logs [service]    - Ver logs (todos o de un servicio)
echo   stop              - Detener todos los servicios
echo   restart [service] - Reiniciar servicios
echo.
echo Configuracion:
echo   Crea un archivo 'deploy-config.env' con:
echo     REMOTE_HOST=tu-servidor.com
echo     REMOTE_USER=root
echo     REMOTE_PORT=22
echo     REMOTE_DIR=/opt/saas-platform
echo.
goto :end

:end
endlocal
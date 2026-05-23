@echo off
REM 使用方式:
REM   基础模式:     start.bat basic
REM   热更新模式:   start.bat hot
REM   静态加载:     start.bat static DcWtf.class
REM   静态+热更:    start.bat both DcWtf.class

set JVM_OPTS=-Xms256m -Xmx256m
set JAR_FILE=easy-unidbg-boot-server-*.jar

if "%1"=="" goto hot
if "%1"=="basic" goto basic
if "%1"=="hot" goto hot
if "%1"=="static" goto static
if "%1"=="both" goto both
goto hot

:basic
echo Starting in basic mode (no modules)...
java %JVM_OPTS% -jar %JAR_FILE%
goto end

:hot
echo Starting in hot-reload mode (watch .\tasks)...
java %JVM_OPTS% -jar %JAR_FILE% --U=.\tasks
goto end

:static
if "%2"=="" (
  echo Usage: start.bat static ^<class_file^> [class_file2 ...]
  goto end
)
set STATIC_ARGS=
:static_loop
if "%2"=="" goto static_run
set STATIC_ARGS=%STATIC_ARGS% --F=%2
shift
goto static_loop
:static_run
echo Starting with static files...
java %JVM_OPTS% -jar %JAR_FILE% %STATIC_ARGS%
goto end

:both
if "%2"=="" (
  echo Usage: start.bat both ^<class_file^> [class_file2 ...]
  goto end
)
set BOTH_ARGS=
:both_loop
if "%2"=="" goto both_run
set BOTH_ARGS=%BOTH_ARGS% --F=%2
shift
goto both_loop
:both_run
echo Starting with static files + hot reload...
java %JVM_OPTS% -jar %JAR_FILE% %BOTH_ARGS% --U=.\tasks
goto end

:end

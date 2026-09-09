@echo off
rem ------------------------------------------------------------------
rem 轻量 Maven Wrapper(Windows CMD / PowerShell)
rem 无需预装 Maven,仅需 JDK 17;优先复用 PATH 上已有的 mvn
rem 用法与 mvn 相同:mvnw.cmd clean package
rem ------------------------------------------------------------------
setlocal EnableExtensions

set MAVEN_VERSION=3.9.6
set DIST_ROOT=%USERPROFILE%\.m2\wrapper\dists
set DIST_DIR=%DIST_ROOT%\apache-maven-%MAVEN_VERSION%\apache-maven-%MAVEN_VERSION%

rem Maven 需要 JAVA_HOME;若未设置则从 PATH 上的 java 推导
if "%JAVA_HOME%"=="" (
  for /f "delims=" %%i in ('powershell -NoProfile -Command "(Get-Item (Get-Command java).Source).Directory.Parent.FullName"') do set JAVA_HOME=%%i
)

where mvn >nul 2>nul
if %ERRORLEVEL%==0 (
  call mvn %*
  exit /b %ERRORLEVEL%
)

if exist "%DIST_DIR%\bin\mvn.cmd" goto run

echo [mvnw] 本地未安装 Maven,开始下载 apache-maven-%MAVEN_VERSION%(约 9MB,仅首次)...
if not exist "%DIST_ROOT%\apache-maven-%MAVEN_VERSION%" mkdir "%DIST_ROOT%\apache-maven-%MAVEN_VERSION%"
set ZIP=%DIST_ROOT%\apache-maven-%MAVEN_VERSION%\apache-maven-%MAVEN_VERSION%-bin.zip
powershell -NoProfile -Command "[Net.ServicePointManager]::SecurityProtocol=[Net.SecurityProtocolType]::Tls12; try { Invoke-WebRequest -Uri 'https://maven.aliyun.com/repository/central/org/apache/maven/apache-maven/%MAVEN_VERSION%/apache-maven-%MAVEN_VERSION%-bin.zip' -OutFile '%ZIP%' } catch { Invoke-WebRequest -Uri 'https://repo.maven.apache.org/maven2/org/apache/maven/apache-maven/%MAVEN_VERSION%/apache-maven-%MAVEN_VERSION%-bin.zip' -OutFile '%ZIP%' }"
if errorlevel 1 (
  echo [mvnw] 下载失败,请检查网络或手动安装 Maven 3.8+
  exit /b 1
)
powershell -NoProfile -Command "Expand-Archive -Force '%ZIP%' '%DIST_ROOT%\apache-maven-%MAVEN_VERSION%'"
del "%ZIP%"

:run
call "%DIST_DIR%\bin\mvn.cmd" %*
endlocal

@echo off
rem Compile with only the JDK, then run.
rem   build.bat            start-up menu (demo / interactive / self-check)
rem   build.bat --demo     the scripted simulated day      (old form: build.bat demo)
rem   build.bat --check    SelfCheck                       (old form: build.bat check)
rem   build.bat --cli      interactive mode
rem   build.bat all        the day, then SelfCheck
rem Replay a session:  build.bat --cli < walkthroughs\01_egmore_to_tambaram.txt
setlocal
cd /d "%~dp0"
chcp 65001 >nul

if exist out rmdir /s /q out
mkdir out

set SRC=src\com\ridehailing
javac -encoding UTF-8 --release 17 -d out ^
  %SRC%\exception\*.java ^
  %SRC%\model\common\*.java ^
  %SRC%\model\vehicle\*.java ^
  %SRC%\model\user\*.java ^
  %SRC%\model\ride\*.java ^
  %SRC%\model\payment\*.java ^
  %SRC%\time\*.java ^
  %SRC%\notification\*.java ^
  %SRC%\service\*.java ^
  %SRC%\app\*.java ^
  %SRC%\cli\*.java ^
  %SRC%\check\*.java
if errorlevel 1 exit /b 1
echo Compiled into out\

set RUN=java -Dfile.encoding=UTF-8 -Dstdout.encoding=UTF-8 -Dsun.stdout.encoding=UTF-8 -cp out com.ridehailing.app.Launcher
if /i "%~1"=="--demo" goto demo
if /i "%~1"=="demo" goto demo
if /i "%~1"=="--check" goto check
if /i "%~1"=="check" goto check
if /i "%~1"=="--cli" goto cli
if /i "%~1"=="cli" goto cli
if /i "%~1"=="all" goto all
if "%~1"=="" goto menu
echo Unknown option %1 (use --demo, --check, --cli or nothing)
exit /b 2

:menu
%RUN%
goto end
:demo
%RUN% --demo
goto end
:check
%RUN% --check
goto end
:cli
%RUN% --cli
goto end
:all
%RUN% --demo
%RUN% --check
:end
endlocal

@echo off

set CALENDAR_PATH=%~dp0
cd %CALENDAR_PATH%
REM Go to main path
cd ..
set ROOT_PATH=%cd%

echo You should configure the http and https proxy to
echo localhost:1081 to appreciate the example

pause

Rem start ham
cd %ROOT_PATH%\ham
dir /b app*.jar > .temp.txt
set /p JAR_NAME=<.temp.txt

set DEBUG_AGENT=-agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=0.0.0.0:5025
IF "%DO_DEBUG%"=="" set DEBUG_AGENT=

REM Start the application
start java "-Dloader.path=%ROOT_PATH%\ham\libs"  -Dloader.main=org.kendar.Main  ^
	  	%DEBUG_AGENT% ^
	  	"-Djsonconfig=%CALENDAR_PATH%\calendar.external.json" ^
		-jar %JAR_NAME% org.springframework.boot.loader.PropertiesLauncher
REM Wait for startup
timeout /t 10 /nobreak
cd %ROOT_PATH%

Rem start be
set DEBUG_AGENT=-agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=0.0.0.0:5026
IF "%DO_DEBUG%"=="" set DEBUG_AGENT=

cd %CALENDAR_PATH%\be
dir /b be*.jar > .temp.txt
set /p JAR_NAME=<.temp.txt
start java %DEBUG_AGENT% -jar %JAR_NAME% --spring.config.location=file:///%cd%\application.properties
REM Wait for startup
timeout /t 10 /nobreak
cd %ROOT_PATH%

Rem start gateway
cd %CALENDAR_PATH%\gateway
dir /b gateway*.jar > .temp.txt
set /p JAR_NAME=<.temp.txt
start java -jar %JAR_NAME% --spring.config.location=file:///%cd%\application.properties
REM Wait for startup
timeout /t 10 /nobreak
cd %ROOT_PATH%

Rem start fe
cd %CALENDAR_PATH%\fe
dir /b fe*.jar > .temp.txt
set /p JAR_NAME=<.temp.txt
start java -jar %JAR_NAME% --spring.config.location=file:///%cd%\application.properties
REM Wait for startup
timeout /t 10 /nobreak
cd %ROOT_PATH%


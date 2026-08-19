@REM ----------------------------------------------------------------------------
@REM Licensed to the Apache Software Foundation (ASF) under one
@REM or more contributor license agreements.  See the NOTICE file
@REM distributed with this work for additional information
@REM regarding copyright ownership.  The ASF licenses this file
@REM to you under the Apache License, Version 2.0 (the
@REM "License"); you may not use this file except in compliance
@REM with the License.  You may obtain a copy of the License at
@REM
@REM    https://www.apache.org/licenses/LICENSE-2.0
@REM
@REM Unless required by applicable law or agreed to in writing,
@REM software distributed under the License is distributed on an
@REM "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
@REM KIND, either express or implied.  See the License for the
@REM specific language governing permissions and limitations
@REM under the License.
@REM ----------------------------------------------------------------------------

@REM ----------------------------------------------------------------------------
@REM Maven Start Up Batch script
@REM ----------------------------------------------------------------------------

@if "%MAVEN_DEBUG_ADDRESS%"=="" (
  @set debug=
) else (
  @set debug=-agentlib:jdwp=transport=dt_socket,server=y,suspend=y,address=%MAVEN_DEBUG_ADDRESS%
)

@set ERROR_CODE=0

@set MAVEN_PROJECTBASEDIR=%MAVEN_BASEDIR%
@if "%MAVEN_PROJECTBASEDIR%"=="" set MAVEN_PROJECTBASEDIR=%~dp0

@set MAVEN_CONFIG=%MAVEN_PROJECTBASEDIR%\.mvn

@set WRAPPER_JAR="%MAVEN_PROJECTBASEDIR%\.mvn\wrapper\maven-wrapper.jar"
@set WRAPPER_LAUNCHER=org.apache.maven.wrapper.MavenWrapperMain

@set DOWNLOAD_URL="https://repo.maven.apache.org/maven2/org/apache/maven/wrapper/maven-wrapper/3.2.0/maven-wrapper-3.2.0.jar"

@if exist %WRAPPER_JAR% goto run

@echo Extension settings will not be applied. Writing to %WRAPPER_JAR%
powershell -Command "& { [Net.ServicePointManager]::SecurityProtocol = [Net.SecurityProtocolType]::Tls12; (New-Object Net.WebClient).DownloadFile('%DOWNLOAD_URL%', '%WRAPPER_JAR%') }"

:run
@set EXEC_DIR=%CD%
@cd /d "%MAVEN_PROJECTBASEDIR%"

@set JAVA_EXE="%JAVA_HOME%\bin\java.exe"
@if "%JAVA_HOME%"=="" set JAVA_EXE=java

%JAVA_EXE% %JVM_CONFIG_MAVEN_PROPERTIES% %MAVEN_OPTS% %debug% "-Dmaven.multiModuleProjectDirectory=%MAVEN_PROJECTBASEDIR%." -classpath %WRAPPER_JAR% %WRAPPER_LAUNCHER% %*
@if ERRORLEVEL 1 set ERROR_CODE=1

@cd /d "%EXEC_DIR%"

@cmd /C exit /B %ERROR_CODE%

@REM ----------------------------------------------------------------------------
@REM Maven Wrapper
@REM ----------------------------------------------------------------------------

@set MAVEN_PROJECTBASEDIR=%CD%
@set MAVEN_OPTS=-Xmx1024m

@if "%JAVA_HOME%" == "" goto error

@java %MAVEN_OPTS% -classpath ".mvn/wrapper/maven-wrapper.jar" ^
    org.apache.maven.wrapper.MavenWrapperMain %*

:error
@echo "JAVA_HOME not set"
exit /b 1
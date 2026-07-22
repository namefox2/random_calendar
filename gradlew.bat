@rem
@rem Copyright 2015 the original author or authors.
@rem
@rem Gradle startup script for Windows

@set JAVA_EXE=java.exe
@set CLASSPATH=%APP_HOME%\gradle\wrapper\gradle-wrapper.jar

@"%JAVA_EXE%" -classpath "%CLASSPATH%" org.gradle.wrapper.GradleWrapperMain %*

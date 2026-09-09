@ECHO OFF

set CLASSPATH=..\ui\target\jitwatch-ui-shaded.jar

"%JAVA_HOME%\bin\java" -cp "%CLASSPATH%" com.chrisnewland.jitwatch.launch.LaunchHeadless %*

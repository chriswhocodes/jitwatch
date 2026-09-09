#!/bin/sh

unamestr=`uname`
if [ "$JAVA_HOME" = '' ]; then
  if [ "$unamestr" = 'Darwin' ]; then
     export JAVA_HOME=`/usr/libexec/java_home`
  else
     echo "JAVA_HOME has not been set."
     exit 0;
  fi
fi

export CLASSPATH=../ui/target/jitwatch-ui-shaded.jar

"$JAVA_HOME/bin/java" -cp "$CLASSPATH" com.chrisnewland.jitwatch.launch.LaunchHeadless $@

@echo off
@rem --------------------------------------------------------------------------------------------------
@rem  Finds the difference between versions on different branch
@rem --------------------------------------------------------------------------------------------------
set REPOS=https://svn.apache.org/repos/asf/openjpa/trunk
set BRANCH=1.0.x
set OLD_ROOT=https://svn.apache.org/repos/asf/openjpa/trunk
set NEW_ROOT=https://svn.apache.org/repos/asf/openjpa/branches/%BRANCH%
rem set FILE=openjpa-kernel/src/main/java/org/apache/openjpa/kernel/BrokerImpl.java
set FILE=%1
@rem openjpa-jdbc/src/main/java/org/apache/openjpa/jdbc/meta/strats/RelationToManyInverseKeyFieldStrategy.java
set NEW_URL=%NEW_ROOT%/%FILE%
set OLD_URL=%OLD_ROOT%/%FILE%
@echo svn diff %1% between trunk AND %BRANCH%
svn diff --old=%OLD_URL% --new=%NEW_URL%
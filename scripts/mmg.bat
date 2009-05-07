@rem ---------------------------------------------------------------------------
@rem Example Batch script to generate canonical meta-model classes
@rem 
@rem The canonical meta-model classes are generated during compilation of
@rem domain classes.
@rem 
@rem See also 
@rem    mmg.options       : The options to Javac compiler 
@rem    domain-class.list : The domain classes to be compiled
@rem ---------------------------------------------------------------------------
@echo off
setlocal
set JAVA_HOME=c:\java\jdk1.6.0_10
set JAVAC=%JAVA_HOME%\bin\javac

set M_REPO="C:\Documents and Settings\Administrator\.m2\repository"
set SPEC=geronimo-jpa_2.0_spec
set VERSION=1.0-EA2-SNAPSHOT
set JPA_LIB=%M_REPO%\org\apache\geronimo\specs\%SPEC%\%VERSION%\%SPEC%-%VERSION%.jar

set CLASSPATH=%JPA_LIB%
set CLASSPATH=%CLASSPATH%;.\openjpa-lib\target\classes
set CLASSPATH=%CLASSPATH%;.\openjpa-persistence\src\main\resources
set CLASSPATH=%CLASSPATH%;.\openjpa-persistence\target\classes
set CLASSPATH=%CLASSPATH%;.\openjpa-kernel\target\classes

%JAVAC% -cp %CLASSPATH% @mmg.options @domain-class.list 

endlocal
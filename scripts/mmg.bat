@rem
@rem Licensed to the Apache Software Foundation (ASF) under one
@rem or more contributor license agreements.  See the NOTICE file
@rem distributed with this work for additional information
@rem regarding copyright ownership.  The ASF licenses this file
@rem to you under the Apache License, Version 2.0 (the
@rem "License"); you may not use this file except in compliance
@rem with the License.  You may obtain a copy of the License at
@rem
@rem  http://www.apache.org/licenses/LICENSE-2.0
@rem
@rem Unless required by applicable law or agreed to in writing,
@rem software distributed under the License is distributed on an
@rem "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
@rem KIND, either express or implied.  See the License for the
@rem specific language governing permissions and limitations
@rem under the License.
@rem

@rem ---------------------------------------------------------------------------
@rem Example Batch script to generate canonical meta-model classes
@rem
@rem Usage
@rem   $ mmg.bat <options.file> <class.list>
@rem 
@rem The canonical meta-model classes can be generated during compilation of
@rem domain classes. This batch file compiles a set of classes (X.java) listed 
@rem in <class.list> file. The compiler is invoked with an annotation
@rem processor which generates a meta-model class X_.java for each X.java. 
@rem The options for annotation processor is specified in <options.file>.
@rem 
@rem See also 
@rem    mmg.options       : The options to Javac compiler 
@rem    domain-class.list : The domain classes to be compiled
@rem ---------------------------------------------------------------------------
@echo off
setlocal
set JAVAC=%JAVA_HOME%\bin\javac

set M_REPO="C:\Documents and Settings\Administrator\.m2\repository"
set SPEC=geronimo-jpa_2.0_spec
set VERSION=1.0-EA9-SNAPSHOT
set JPA_LIB=%M_REPO%\org\apache\geronimo\specs\%SPEC%\%VERSION%\%SPEC%-%VERSION%.jar

set CLASSPATH=%JPA_LIB%
set CLASSPATH=%CLASSPATH%;..\openjpa\src\main\resources
set CLASSPATH=%CLASSPATH%;..\openjpa\target\classes

echo Using Java Compiler %JAVAC%
%JAVAC% -version
%JAVAC% -cp %CLASSPATH% @%1 @%2

endlocal

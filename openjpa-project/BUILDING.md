# Building Apache OpenJPA
<!-- Licensed under Apache License 2.0 - http://www.apache.org/licenses/LICENSE-2.0 -->

These instructions describe how to build OpenJPA from source code using the
Apache Maven 3 build tool. They are written for use from the console, and are
known to work on Windows, Linux and Mac OSX.

The most up to date version of these instructions may be found on the OpenJPA
website at http://openjpa.apache.org/building.html

1. Ensure that Java SE 6 is installed and on your path by running:
        java -version
    OpenJPA requires Java 17 or later to build and for runtime.
2. Install the build tool Apache Maven 3.9.12 or later, from:
        http://maven.apache.org/
    If it is installed correctly, typing mvn -v from the console will result
    in the text like "Apache Maven 3.9.12"
3. Extract the source archive if you have not already done so.
    The source will create a new directory named:
    openjpa-<version>-source. Where <version> is the OpenJPA
    version, for example apache-openjpa-4.2.0-source.
4. Change to the OpenJPA source directory, which has already been created in
    the previous step.
5. Build OpenJPA by running: mvn package or better mvn install.
    The first time you run the build, many dependencies are automatically
    resolved and downloaded. It is common for dependency downloading to fail
    the first time, which will fail the build. If any of these dependency
    downloads fail, just re-run the command. You may also add the following
    to your ~/.m2/setting.xml file
    (see http://maven.apache.org/guides/mini/guide-mirror-settings.html)

       <settings>
           <mirrors>
               <mirror>
                   <id>repo.mergere.com</id>
                   <url>http://repo.mergere.com/maven2</url>
                   <mirrorOf>central</mirrorOf>
                   </mirror>
           </mirrors>
        </settings>

If any tests fail, and you want to ignore the failures, instead run:
`mvn package -DskipTests`

An example session is as follows:

```
$ cd /tmp/

$ java -version
openjdk version "17.0.20.1" 2026-08-18
OpenJDK Runtime Environment Temurin-17.0.20.1+1 (build 17.0.20.1+1)
OpenJDK 64-Bit Server VM Temurin-17.0.20.1+1 (build 17.0.20.1+1, mixed mode, sharing)

$ mvn -v
Apache Maven 3.10.0 (c43a36b8d67be7e0805a411bc0898af1a51f5472)
 ...

$ git --version
git version 2.53.0

$ git clone https://github.com/apache/openjpa.git
Cloning into 'openjpa'...
remote: Enumerating objects: ...

$ cd openjpa

$ git checkout 4.2.0

$ mvn clean install -DskipTests

[INFO] Scanning for projects...
[INFO] Reactor build order:
[INFO]   OpenJPA Parent POM
[INFO]   OpenJPA Utilities Library
[INFO]   OpenJPA Kernel
[INFO]   OpenJPA JDBC
[INFO]   OpenJPA Persistence
[INFO]   OpenJPA Persistence JDBC
[INFO]   OpenJPA Persistence Locking Tests
[INFO]   OpenJPA tools
[INFO]   OpenJPA Maven Plugin
[INFO]   OpenJPA XML Store
[INFO]   OpenJPA Slice
[INFO]   OpenJPA JEST
[INFO]   OpenJPA Aggregate Jar
[INFO]   OpenJPA Aggregate Jar with Dependencies
[INFO]   OpenJPA Project Docs and Assemblies
[INFO]   OpenJPA Examples
[INFO]   OpenJPA Examples - Simple
[INFO]   OpenJPA Examples - image-gallery
[INFO]   OpenJPA Examples - OpenBooks
[INFO]   OpenJPA Integration Tests
[INFO]   OpenJPA Integration Tests - Daytrader
[INFO]   OpenJPA Integration Tests - Examples
[INFO]   OpenJPA Integration Tests - SLF4JLogFactory
[INFO]   OpenJPA Integration Tests - JPA TCK
[INFO]   OpenJPA Integration Tests - Bean Validation
[INFO]   OpenJPA Integration Tests - JMX Platform MBeans
[INFO] ------------------------------------------------------------------------
[INFO] Building OpenJPA Parent POM
[INFO]    task-segment: [clean, install]
[INFO] ------------------------------------------------------------------------

 ...

[INFO]
[INFO] ------------------------------------------------------------------------
[INFO] Reactor Summary:
[INFO] ------------------------------------------------------------------------
[INFO] OpenJPA Parent POM .................................... SUCCESS [2:11.649s]
[INFO] OpenJPA Utilities Library ............................. SUCCESS [31.576s]
[INFO] OpenJPA Kernel ........................................ SUCCESS [39.863s]
[INFO] OpenJPA JDBC .......................................... SUCCESS [27.326s]
[INFO] OpenJPA Persistence ................................... SUCCESS [23.530s]
[INFO] OpenJPA Persistence JDBC .............................. SUCCESS [1:14.696s]
[INFO] OpenJPA Persistence Locking Tests ..................... SUCCESS [30.881s]
[INFO] OpenJPA tools ......................................... SUCCESS [0.968s]
[INFO] OpenJPA Maven Plugin .................................. SUCCESS [2:45.320s]
[INFO] OpenJPA XML Store ..................................... SUCCESS [25.335s]
[INFO] OpenJPA Slice ......................................... SUCCESS [10.456s]
[INFO] OpenJPA JEST .......................................... SUCCESS [8.861s]
[INFO] OpenJPA Aggregate Jar ................................. SUCCESS [50.297s]
[INFO] OpenJPA Aggregate Jar with Dependencies ............... SUCCESS [13.871s]
[INFO] OpenJPA Project Docs and Assemblies ................... SUCCESS [1:12.215s]
[INFO] OpenJPA Examples ...................................... SUCCESS [1.241s]
[INFO] OpenJPA Examples - Simple ............................. SUCCESS [5.165s]
[INFO] OpenJPA Examples - image-gallery ...................... SUCCESS [6.933s]
[INFO] OpenJPA Examples - OpenBooks .......................... SUCCESS [26.761s]
[INFO] OpenJPA Integration Tests ............................. SUCCESS [0.957s]
[INFO] OpenJPA Integration Tests - Daytrader ................. SUCCESS [21.631s]
[INFO] OpenJPA Integration Tests - Examples .................. SUCCESS [0.143s]
[INFO] OpenJPA Integration Tests - SLF4JLogFactory ........... SUCCESS [6.690s]
[INFO] OpenJPA Integration Tests - JPA TCK ................... SUCCESS [0.321s]
[INFO] OpenJPA Integration Tests - Bean Validation ........... SUCCESS [10.694s]
[INFO] OpenJPA Integration Tests - JMX Platform MBeans ....... SUCCESS [19.434s]
[INFO] ------------------------------------------------------------------------
[INFO] ------------------------------------------------------------------------
[INFO] BUILD SUCCESSFUL
[INFO] ------------------------------------------------------------------------
[INFO] Total time: 13 minutes 33 seconds
[INFO] Finished at: Thu Feb 09 10:25:05 CST 2012
[INFO] Final Memory: 118M/232M
[INFO] ------------------------------------------------------------------------

$ mvn package -Dbuilddocs

$ ls openjpa-project/target/site/downloads/

apache-openjpa-4.2.0-binary-nodocs.zip
apache-openjpa-4.2.0-binary.zip
apache-openjpa-4.2.0-source.zip

$ ls openjpa-project/target/site/docs/

html
javadoc
manual.pdf
multihtml
```

`SNAPSHOT` documentation will be uploaded to https://nightlies.apache.org/ by
Jenkins: https://ci-builds.apache.org/job/OpenJPA/job/openjpa/

`RELEASE` documentation should be manually added to correspondent folder at
https://github.com/apache/openjpa-site/tree/builds

# Advanced Build Options


To speed up the local build time you might want to exclude the tests which
check locking timeouts as they take a really long time (mostly waiting for
timeout expiration).

This can be achieved by building the project with
`mvn clean install -Dsurefire.excludes.locking=**/*`

# Building and deploying the Site

The site (including docbook documentation) is prepared during `mvn package -Dbuilddocs` step above

The generated resources are available at `openjpa-project/target/site/docs/`


# Running unit tests in the Debugger

By default all tests run against a derby database.
To run the tests in the debugger simply add the following JVM properties

`-ea -Dopenjpa.ConnectionURL=jdbc:derby:target/database/openjpa-derby-database;create=true -Dopenjpa.ConnectionDriverName=org.apache.derby.jdbc.EmbeddedDriver`

For running against a MySQL Docker installation:
`-ea -Dopenjpa.ConnectionDriverName=com.mysql.jdbc.Driver -Dopenjpa.ConnectionURL=jdbc:mysql://localhost:3306/openjpatst -Dopenjpa.ConnectionUserName=openjpatst -Dopenjpa.ConnectionPassword=openjpatst`

Running against a PostgreSQL Docker installation:
`-ea -Dopenjpa.ConnectionDriverName=org.postgresql.Driver -Dopenjpa.ConnectionURL=jdbc:postgresql://localhost:5432/openjpatst -Dopenjpa.ConnectionUserName=postgres -Dopenjpa.ConnectionPassword=postgres`

For running against a MariaDB Docker installation:
`-ea -Dopenjpa.ConnectionDriverName=org.mariadb.jdbc.Driver -Dopenjpa.ConnectionURL=jdbc:mariadb://localhost:3306/openjpatst -Dopenjpa.ConnectionUserName=root -Dopenjpa.ConnectionPassword=openjpatst`

For running against a h2 based installation:
`-ea -Dopenjpa.ConnectionDriverName=org.h2.Driver -Dopenjpa.ConnectionURL=jdbc:h2:./target/database/openjpa-h2-database -Dopenjpa.ConnectionUserName=root -Dopenjpa.ConnectionPassword=openjpatst`

For running against a hsqldb based installation:
`-ea -Dopenjpa.ConnectionDriverName=org.hsqldb.jdbcDriver -Dopenjpa.ConnectionURL=jdbc:hsqldb:mem:openjpa20-hsqldb-database -Dopenjpa.ConnectionUserName=sa -Dopenjpa.ConnectionPassword=`

For running tests against a Microsoft SQLServer Docker based installation:
`-ea -Dopenjpa.ConnectionDriverName=com.microsoft.sqlserver.jdbc.SQLServerDriver -Dopenjpa.ConnectionURL=jdbc:sqlserver://localhost:1433;sendTimeAsDatetime=false -Dopenjpa.ConnectionUserName=SA -Dopenjpa.ConnectionPassword=OpenJP8tst`

For running against a Oracle Docker installation:
`-ea -Dopenjpa.ConnectionDriverName=oracle.jdbc.driver.OracleDriver -Dopenjpa.ConnectionURL=jdbc:oracle:thin:@localhost:1521:xe -Dopenjpa.ConnectionUserName=openjpatst -Dopenjpa.ConnectionPassword=openjpatst`


For starting tests in `openjpa-persistence-jdbc` inside a compiler you can also trigger all the enhancement manually via:

`mvn process-test-classes`

# Building in a Docker container

Sometimes it is preferred to build the project outside your local environment, that may be working with a different version of JDK, maven etc.
In that case, you may build the project using a maven docker container. For that, you may replace the

`mvn clean install -DskipTests -DfailIfNoTests=false`

command for

`docker run --rm --name openjpa-build -u $(id -u):$(id -g) -v ~/.m2:/opt/openjpa/.m2 -v $PWD:/opt/openjpa -e MAVEN_CONFIG=/opt/openjpa/.m2 -w /opt/openjpa maven:3-eclipse-temurin-17 mvn -Duser.home=/opt/openjpa clean install -DskipTests -DfailIfNoTests=false`

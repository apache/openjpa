# OpenJPA JPA 3.2 TCK Runner

This module runs the Jakarta Persistence 3.2 Technology Compatibility Kit (TCK)
against OpenJPA. PostgreSQL is the default database; MySQL, MS SQL Server, Derby and
Oracle are also supported via `DB_TYPE`.

## Prerequisites

- Java 17+
- Maven 3.9.x (NOTE build will fail with Maven 3.10.0)
- Docker (unless you will add parameter `--no-docker` or will select Derby)
- `curl`, `unzip`, `grep`, `sed` (the script checks for all required commands at startup)

## Quick Start

### 1. Build OpenJPA

From the **project root**:

```bash
mvn install -DskipTests
```

### 2. Run the TCK

```bash
cd openjpa-integration/tck
./run-tck32.sh
```

The script will:
1. Stop any dockerized databases left over from a previous run (please specify `--stop-all` for this)
2. Start a dockerized PostgreSQL via the `test-postgresql-docker` profile
3. Download the JPA 3.2 TCK (cached in `target/tck32/`)
4. Extract it and patch its `pom.xml` with the OpenJPA provider profile
5. Install the TCK artifacts into the local Maven repository
6. Run the TCK

To skip starting the database container please add `--no-docker` command line option.

## Configuration

All settings have sensible defaults matching the dockerized database the script
starts. Override via environment variables:

| Variable           | Default              | Description                          |
|--------------------|----------------------|--------------------------------------|
| `DB_TYPE`          | `postgresql`         | `postgresql`, `mysql`, `mssql`, `derby`, `oracle` |
| `DB_VERSION`       | `15` (postgresql)    | Database image version               |
| `DB_HOST`          | `localhost:5555`     | Database host:port — set to skip Docker |
| `DB_USER`          | `tcktest`            | Database user                        |
| `DB_PASSWORD`      | `TcktestPwd13`       | Database password                    |
| `DB_NAME`          | `tcktest`            | Database name                        |
| `OPENJPA_VERSION`  | `4.2.0-SNAPSHOT`     | OpenJPA version to test              |
| `TCK_VERSION`      | `3.2.0`              | TCK version to download              |
| `GF_VERSION`       | `8.0.0`              | GlassFish version for runtime        |

Examples:

```bash
DB_VERSION=18 ./run-tck32.sh                                    # PostgreSQL 18 instead of 15
DB_TYPE=mysql ./run-tck32.sh                                    # MySQL 8.4.8
DB_HOST=dbserver:5432 DB_NAME=mydb ./run-tck32.sh --no-docker   # existing database, no Docker
```

Arguments after the script name are passed through to the TCK Maven build.

## Running a Single Test

After the initial `./run-tck32.sh` has set up the TCK, you can run individual
tests directly:

```bash
cd target/tck32/persistence-tck/bin
mvn verify -P "openjpa,postgresql" \
  -Dglassfish.container.version=8.0.0 \
  -Dopenjpa.version=4.2.0-SNAPSHOT \
  -Djakarta.persistence.jdbc.user=tcktest \
  -Djakarta.persistence.jdbc.password=tcktest \
  "-Djakarta.persistence.jdbc.url=jdbc:postgresql://localhost:5555/tcktest" \
  -Dit.test='ee.jakarta.tck.persistence.core.derivedid.ex2b.Client#DIDTest'
```

## Re-running After Code Changes

After modifying and rebuilding OpenJPA, you can either:

**Option A** — Re-run the full script (rebuilds everything):
```bash
mvn install -DskipTests
./run-tck32.sh
```

**Option B** — Copy the jar and re-run from the TCK directory (faster):
```bash
mvn install -pl openjpa -DskipTests
cp ~/.m2/repository/org/apache/openjpa/openjpa/4.2.0-SNAPSHOT/openjpa-4.2.0-SNAPSHOT.jar \
  target/tck32/persistence-tck/bin/target/glassfish8/glassfish/modules/openjpa.jar
cd target/tck32/persistence-tck/bin
mvn verify -P "openjpa,postgresql" \
  -Dglassfish.container.version=8.0.0 \
  -Dopenjpa.version=4.2.0-SNAPSHOT \
  -Djakarta.persistence.jdbc.user=tcktest \
  -Djakarta.persistence.jdbc.password=TcktestPwd13 \
  "-Djakarta.persistence.jdbc.url=jdbc:postgresql://localhost:5555/tcktest"
```

## Test Results

Results are written to:
- `target/tck32/persistence-tck/bin/target/failsafe-reports/` — individual test XML reports
- Console output — summary with pass/fail counts

## Maven Profile

The TCK can also be triggered via the Maven profile defined in this module's
`pom.xml`:

```bash
mvn verify -Ptck32-profile
```

This invokes `run-tck32.sh` automatically.

## Key Files

| File                          | Purpose                                              |
|-------------------------------|------------------------------------------------------|
| `run-tck32.sh`               | Main TCK runner script                               |
| `tck32-openjpa-profile.xml`  | Maven profile fragment injected into TCK pom.xml     |
| `pom.xml`                    | Module POM with `tck32-profile` for Maven invocation |

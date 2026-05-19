# OpenJPA JPA 3.2 TCK Runner

This module runs the Jakarta Persistence 3.2 Technology Compatibility Kit (TCK)
against OpenJPA using PostgreSQL.

## Prerequisites

- Java 17+
- Maven 3.x
- PostgreSQL (via Docker or a standalone instance)
- `psql` command-line client (for database preparation)
- `python3`, `curl`, `unzip` (the script checks for all required commands at startup)

## Quick Start

### 1. Start PostgreSQL

From the **project root**:

```bash
docker compose up -d
```

This starts PostgreSQL on `localhost:5433` with database `openjpa_tck`,
user `openjpa`, password `openjpa` (see `docker-compose.yml`).

### 2. Build OpenJPA

```bash
mvn install -DskipTests
```

### 3. Run the TCK

```bash
cd openjpa-integration/tck
./run-tck32.sh
```

The script will:
1. Download the JPA 3.2 TCK (cached in `target/tck32/`)
2. Extract and patch the TCK with the OpenJPA provider profile
3. Prepare the PostgreSQL database (drop stale tables, create stored procedures)
4. Run all 2134 TCK tests

## Configuration

All settings have sensible defaults matching the `docker-compose.yml` in the
project root. Override via environment variables:

| Variable           | Default              | Description                     |
|--------------------|----------------------|---------------------------------|
| `DB_HOST`          | `localhost:5433`     | PostgreSQL host:port            |
| `DB_USER`          | `openjpa`            | Database user                   |
| `DB_PASSWORD`      | `openjpa`            | Database password               |
| `DB_NAME`          | `openjpa_tck`        | Database name                   |
| `OPENJPA_VERSION`  | `4.2.0-SNAPSHOT`     | OpenJPA version to test         |
| `TCK_VERSION`      | `3.2.0`              | TCK version to download         |
| `GF_VERSION`       | `8.0.0`              | GlassFish version for runtime   |

Example with a custom database host:

```bash
DB_HOST=dbserver:5432 DB_NAME=mydb ./run-tck32.sh
```

## Running a Single Test

After the initial `./run-tck32.sh` has set up the TCK, you can run individual
tests directly:

```bash
cd target/tck32/persistence-tck/bin
mvn verify -P "openjpa,postgresql" \
  -Dglassfish.container.version=8.0.0 \
  -Dopenjpa.version=4.2.0-SNAPSHOT \
  -Djakarta.persistence.jdbc.user=openjpa \
  -Djakarta.persistence.jdbc.password=openjpa \
  "-Djakarta.persistence.jdbc.url=jdbc:postgresql://localhost:5433/openjpa_tck" \
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
  -Djakarta.persistence.jdbc.user=openjpa \
  -Djakarta.persistence.jdbc.password=openjpa \
  "-Djakarta.persistence.jdbc.url=jdbc:postgresql://localhost:5433/openjpa_tck"
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

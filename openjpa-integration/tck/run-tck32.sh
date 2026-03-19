#!/bin/bash
#
# Licensed to the Apache Software Foundation (ASF) under one
# or more contributor license agreements.  See the NOTICE file
# distributed with this work for additional information
# regarding copyright ownership.  The ASF licenses this file
# to you under the Apache License, Version 2.0 (the
# "License"); you may not use this file except in compliance
# with the License.  You may obtain a copy of the License at
#
# http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing,
# software distributed under the License is distributed on an
# "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
# KIND, either express or implied.  See the License for the
# specific language governing permissions and limitations
# under the License.
#

#
# Script to download, setup and run the JPA 3.2 TCK against OpenJPA.
#
# Usage:
#   ./run-tck32.sh                          # Run with Derby (default)
#   ./run-tck32.sh postgresql               # Run with PostgreSQL
#   OPENJPA_VERSION=4.2.0-SNAPSHOT ./run-tck32.sh
#
# Prerequisites:
#   - OpenJPA must be built and installed in local Maven repo
#     (run 'mvn install -DskipTests' from the project root)
#   - Java 17+ and Maven 3.x
#

set -e

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
TCK_VERSION="${TCK_VERSION:-3.2.0}"
TCK_URL="https://download.eclipse.org/jakartaee/persistence/3.2/jakarta-persistence-tck-${TCK_VERSION}.zip"
TCK_DIR="${SCRIPT_DIR}/target/tck32"
TCK_ZIP="${TCK_DIR}/jakarta-persistence-tck-${TCK_VERSION}.zip"
TCK_HOME="${TCK_DIR}/persistence-tck"
OPENJPA_VERSION="${OPENJPA_VERSION:-4.2.0-SNAPSHOT}"
RDBMS="${1:-derby}"

GF_VERSION="${GF_VERSION:-8.0.0}"

echo "=== JPA 3.2 TCK Runner for OpenJPA ==="
echo "OpenJPA version: ${OPENJPA_VERSION}"
echo "TCK version:     ${TCK_VERSION}"
echo "GlassFish:       ${GF_VERSION}"
echo "Database:        ${RDBMS}"
echo ""

# Step 1: Download TCK if not present
mkdir -p "${TCK_DIR}"
if [ ! -f "${TCK_ZIP}" ]; then
    echo "Downloading JPA 3.2 TCK..."
    curl -sL -o "${TCK_ZIP}" "${TCK_URL}"
    echo "Downloaded."
else
    echo "TCK zip already present, skipping download."
fi

# Step 2: Extract TCK
if [ ! -d "${TCK_HOME}" ]; then
    echo "Extracting TCK..."
    cd "${TCK_DIR}" && unzip -q "${TCK_ZIP}"
    echo "Extracted."
else
    echo "TCK already extracted, skipping."
fi

# Step 3: Install TCK artifacts into local Maven repo
echo "Installing TCK artifacts..."
cd "${TCK_HOME}/artifacts" && bash artifact-install.sh "${TCK_VERSION}"
echo "TCK artifacts installed."

# Step 4: Patch TCK pom.xml to add OpenJPA profile (if not already patched)
TCK_POM="${TCK_HOME}/bin/pom.xml"
if ! grep -q '<id>openjpa</id>' "${TCK_POM}"; then
    echo "Adding OpenJPA profile to TCK pom.xml..."
    # Read the profile fragment
    PROFILE_XML=$(cat "${SCRIPT_DIR}/tck32-openjpa-profile.xml" | grep -v '^<!--' | grep -v '^\-\->' | grep -v '^$' | sed '/^<!--/,/-->$/d')

    # Insert before the closing </profiles> tag
    # Use a temp file for portability
    TEMP_POM="${TCK_POM}.tmp"
    sed '/<\/profiles>/i\
<!-- OpenJPA profile - auto-inserted by run-tck32.sh -->
' "${TCK_POM}" > "${TEMP_POM}"

    # Actually, let's use python for reliable XML insertion
    python3 -c "
import re
with open('${TCK_POM}', 'r') as f:
    content = f.read()
with open('${SCRIPT_DIR}/tck32-openjpa-profile.xml', 'r') as f:
    profile = f.read()
# Strip XML comments from profile (the license header)
profile_lines = profile.split('\n')
in_comment = False
clean_lines = []
for line in profile_lines:
    if '<!--' in line and '-->' in line:
        continue
    if '<!--' in line:
        in_comment = True
        continue
    if '-->' in line:
        in_comment = False
        continue
    if not in_comment:
        clean_lines.append(line)
profile_clean = '\n'.join(clean_lines)
# Insert before </profiles>
content = content.replace('</profiles>', profile_clean + '\n    </profiles>')
with open('${TCK_POM}', 'w') as f:
    f.write(content)
"
    rm -f "${TEMP_POM}"
    echo "OpenJPA profile added."
else
    echo "OpenJPA profile already present in TCK pom.xml."
fi

# Step 5: Run TCK
echo ""
echo "=== Running JPA 3.2 TCK ==="
echo ""

EXTRA_ARGS=""

if [ "$RDBMS" == "derby" ] || [ -z "$RDBMS" ]; then
    mvn -e -f "${TCK_POM}" -P "openjpa,derby" verify \
        "-Dopenjpa.version=${OPENJPA_VERSION}" \
        "-Dglassfish.container.version=${GF_VERSION}" \
        "-Djakarta.persistence.jdbc.user=cts1" \
        "-Djakarta.persistence.jdbc.password=cts1" \
        "-Djakarta.persistence.jdbc.url=jdbc:derby://localhost:1527/derbyDB;create=true" \
        ${EXTRA_ARGS}
elif [ "$RDBMS" == "postgresql" ]; then
    # Execute stored procedure DDL if available
    SP_DDL="${TCK_HOME}/sql/postgresql/postgresql.ddl.persistence.sprocs.sql"
    if [ -f "$SP_DDL" ]; then
        echo "Creating stored procedures on PostgreSQL..."
        DB_HOST_ONLY="${DB_HOST%%:*}"
        DB_PORT="${DB_HOST##*:}"
        PGPASSWORD="${DB_PASSWORD:-openjpa}" psql -h "${DB_HOST_ONLY}" -p "${DB_PORT}" \
            -U "${DB_USER:-openjpa}" -d openjpa_tck -f "$SP_DDL" 2>/dev/null || \
            echo "Warning: Failed to create stored procedures (non-fatal)"
    fi
    mvn -e -f "${TCK_POM}" -P "openjpa,postgresql" verify \
        "-Dopenjpa.version=${OPENJPA_VERSION}" \
        "-Dglassfish.container.version=${GF_VERSION}" \
        "-Djakarta.persistence.jdbc.user=${DB_USER:-openjpa}" \
        "-Djakarta.persistence.jdbc.password=${DB_PASSWORD:-openjpa}" \
        "-Djakarta.persistence.jdbc.url=jdbc:postgresql://${DB_HOST:-localhost}/openjpa_tck" \
        ${EXTRA_ARGS}
else
    echo "Unsupported RDBMS: ${RDBMS}"
    echo "Supported: derby, postgresql"
    exit 1
fi

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

TCK_VERSION="${TCK_VERSION:-3.2.0}"
TCK_BASE="${TCK_VERSION:0:3}"

SCRIPT_ROOT=$(cd -- $(dirname "${0}") && pwd)
PROJECT_ROOT="$(cd "${SCRIPT_ROOT}/../.." && pwd)"

# derby is removed for now
# mysql 8.0 is broken! db.delimiter='!' breaks connection
declare -A FULL_DB_LIST=(
    [mysql]=mysql
    [postgresql]=postgresql
    [mssql]=mssqlserver
    [oracle]=oracle
)

declare -A VERSIONS=(
    [mysql]='8.4.8' #  8.4.8 9.4.0
    [postgresql]='16'
)

#
# Script to download, setup and run the JPA 3.2 TCK against OpenJPA
# using DB selected.
#
# Usage:
#   ./run-tck32.sh                                 # Run full TCK against postgres:16
#   DB_VERSION=18 ./run-tck32.sh                   # Run full TCK against postgres:18
#   DB_TYPE=mysql ./run-tck32.sh                   # Run full TCK against mysql:8.4.8
#   DB_HOST=myhost:5432 ./run-tck32.sh             # Custom DB host
#   OPENJPA_VERSION=4.2.0-SNAPSHOT ./run-tck32.sh  # Custom OpenJPA version
#
# Prerequisites:
#   - OpenJPA must be built and installed in local Maven repo
#     (run 'mvn install -DskipTests' from the project root)
#   - Java 17+ and Maven 3.x
#

set -e

# Check required commands
MISSING=""
for cmd in java mvn curl unzip grep sed; do
    if ! command -v "$cmd" &>/dev/null; then
        MISSING="${MISSING}  - ${cmd}\n"
    fi
done
if [[ -n "$MISSING" ]]; then
    echo "ERROR: The following required commands are not available:"
    echo -e "$MISSING"
    echo "Please install them before running this script."
    exit 1
fi

stopAll() {
    for profile in ${!FULL_DB_LIST[@]}; do
        echo "Stopping dockerized ${profile}"
        mvn -N -Ptest-${profile}-docker -f ${PROJECT_ROOT} -Ddocker.stopNamePattern=${profile:0:8}* docker:stop -Ddocker.showLogs
    done
}

stopAll

TCK_URL="https://download.eclipse.org/jakartaee/persistence/${TCK_BASE}/jakarta-persistence-tck-${TCK_VERSION}.zip"
TCK_DIR="${SCRIPT_ROOT}/target/tck32"
TCK_ZIP="${TCK_DIR}/jakarta-persistence-tck-${TCK_VERSION}.zip"
TCK_HOME="${TCK_DIR}/persistence-tck"
OPENJPA_VERSION="${OPENJPA_VERSION:-4.2.0-SNAPSHOT}"
START_DOCKER=

if [[ -z "${DB_HOST}" ]]; then
    START_DOCKER=1
fi

DB_TYPE="${DB_TYPE:-postgresql}"
DB_HOST="${DB_HOST:-localhost:5555}"
DB_USER="${DB_USER:-tcktest}"
DB_PASSWORD="${DB_PASSWORD:-tcktest}"
DB_NAME="${DB_NAME:-tcktest}"
DB_URL=""
declare -a JPROPS=()

DB_HOST_ONLY="${DB_HOST%%:*}"
DB_PORT="${DB_HOST##*:}"

TCK_DB_TYPE="${FULL_DB_LIST[${DB_TYPE}]}"

setProps() {
    local ver="${VERSIONS[${DB_TYPE}]}"
    if [[ -n "${DB_VERSION}" ]]; then
        ver="${DB_VERSION}"
    fi
    if [[ -n "${VERSIONS[${DB_TYPE}]}" ]]; then
        JPROPS+=("-D${DB_TYPE}.server.version=${ver}")
    fi

    case ${DB_TYPE} in
        mysql)
            DB_URL="jdbc:mysql://${DB_HOST}/${DB_NAME}?useSSL=false&allowPublicKeyRetrieval=true&transformedBitIsBoolean=true"
            ;;
        postgresql)
            DB_URL="jdbc:postgresql://${DB_HOST}/${DB_NAME}"
            ;;
        mssql)
            DB_NAME=""
            DB_URL="jdbc:sqlserver://${DB_HOST};sendTimeAsDatetime=false;trustServerCertificate=true"
            ;;
        oracle)
            JPROPS+=("-Dopenjpa.oracle.SID=${DB_NAME}")
            DB_NAME="XE"
            DB_URL="jdbc:oracle:thin:@${DB_HOST}:${DB_NAME}"
            ;;
    esac
    JPROPS+=("-Ddocker.external.${DB_TYPE}.port=${DB_PORT}")
    JPROPS+=("-Dopenjpa.${DB_TYPE}.dbname=${DB_NAME}")
    JPROPS+=("-Dopenjpa.${DB_TYPE}.username=${DB_USER}")
    JPROPS+=("-Dopenjpa.${DB_TYPE}.password=${DB_PASSWORD}")
    JPROPS+=("-Dopenjpa.${DB_TYPE}.url=${DB_URL}")
}

setProps

if [[ -n "${START_DOCKER}" ]]; then
    echo "Starting dockerized ${DB_TYPE}"
    mvn -N -f ${PROJECT_ROOT} -Ptest-${DB_TYPE}-docker "${JPROPS[@]}" docker:start -Ddocker.showLogs
fi

GF_VERSION="${GF_VERSION:-8.0.0}"

echo "=== JPA ${TCK_BASE} TCK Runner for OpenJPA ==="
echo "OpenJPA version: ${OPENJPA_VERSION}"
echo "TCK version:     ${TCK_VERSION}"
echo "GlassFish:       ${GF_VERSION}"
echo "Database:        ${DB_TYPE} (${DB_HOST}/${DB_NAME})"
echo ""

# Step 1: Download TCK if not present
mkdir -p "${TCK_DIR}"
if [[ ! -f "${TCK_ZIP}" ]]; then
    echo "Downloading JPA ${TCK_BASE} TCK..."
    curl -sL -o "${TCK_ZIP}" "${TCK_URL}"
    echo "Downloaded."
else
    echo "TCK zip already present, skipping download."
fi

# Step 2: Extract TCK
if [[ ! -d "${TCK_HOME}" ]]; then
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
if [[ -z "$(grep '<id>openjpa</id>' "${TCK_POM}")" ]]; then
    echo "Adding OpenJPA profile to TCK pom.xml..."

    sed '/<\/profiles>/,$d' "${TCK_POM}" > temp
    cat "${SCRIPT_ROOT}/tck32-openjpa-profile.xml" >> temp
    sed '/<\/profiles>/,$!d' "${TCK_POM}" >> temp
    mv temp "${TCK_POM}"

    echo "OpenJPA profile added."
else
    echo "OpenJPA profile already present in TCK pom.xml."
fi

echo ""
echo "=== Preparing ${DB_TYPE} database ==="

# Step 6: Run TCK
echo ""
echo "=== Running JPA ${TCK_BASE} TCK ==="
echo ""

#    "-Dmysql.jdbc.version=9.7.0" \
mvn -e -f "${TCK_POM}" -P "openjpa,${TCK_DB_TYPE}" verify \
    "-Dopenjpa.version=${OPENJPA_VERSION}" \
    "-Dglassfish.container.version=${GF_VERSION}" \
    "-Djakarta.persistence.jdbc.user=${DB_USER}" \
    "-Djakarta.persistence.jdbc.password=${DB_PASSWORD}" \
    "-Djakarta.persistence.jdbc.url=${DB_URL}" \
    "$@"

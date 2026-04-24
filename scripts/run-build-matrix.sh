#!/usr/bin/env bash
#
# Licensed to the Apache Software Foundation (ASF) under one or more
# contributor license agreements.  See the NOTICE file distributed with
# this work for additional information regarding copyright ownership.
# The ASF licenses this file to you under the Apache License, Version 2.0
# (the "License"); you may not use this file except in compliance with
# the License.  You may obtain a copy of the License at:
#
#   http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or
# implied.  See the License for the specific language governing
# permissions and limitations under the License.
#
# Build OpenJPA against one or more dockerised databases and collect
# Surefire/Failsafe reports. Mirrors what the Ubuntu build host runs.
#
# Usage:
#   scripts/run-build-matrix.sh                  # run the full matrix
#   scripts/run-build-matrix.sh pg17 mariadb     # run a subset
#
# Output layout (per flavor):
#   builds/<flavor>/...                          # copied surefire reports
#   build-log-<flavor>-<yyyymmdd>.txt            # console log
#
# Pin TZ=UTC so the JVM and any wall-clock-sensitive tests agree with
# the Postgres/MariaDB/MySQL session (all of which default to UTC in
# the stock docker images). Set TZ in the environment before invoking
# this script to override.

set -u -o pipefail

export TZ="${TZ:-UTC}"

PROJECT_ROOT="$(cd "$(dirname "$0")/.." && pwd)"
BUILDS_DIR="${BUILDS_DIR:-$PROJECT_ROOT/builds}"
LOG_DIR="${LOG_DIR:-$PROJECT_ROOT}"
MVN_TIMEOUT="${MVN_TIMEOUT:-300m}"
DB_READY_WAIT="${DB_READY_WAIT:-30}"
WIPE_M2="${WIPE_M2:-0}"   # 1 = rm -rf ~/.m2/repository before each run

declare -A COMPOSE_FILES=(
    [pg17]="$PROJECT_ROOT/docker-compose-test-pg17.yml"
    [pg18]="$PROJECT_ROOT/docker-compose-test-pg18.yml"
    [mariadb]="$PROJECT_ROOT/docker-compose-test-mariadb.yml"
    [mysql]="$PROJECT_ROOT/docker-compose-test-mysql.yml"
)

declare -A MVN_PROFILES=(
    [pg17]="test-postgresql-docker"
    [pg18]="test-postgresql-docker"
    [mariadb]="test-mariadb-docker"
    [mysql]="test-mysql-docker"
)

declare -A MVN_EXTRA_ARGS=(
    [pg17]=""
    [pg18]=""
    [mariadb]=""
    [mysql]="-Ddocker.skip=true -Ddocker.external.mysql.port=3307"
)

run_flavor() {
    local flavor="$1"
    local compose="${COMPOSE_FILES[$flavor]:-}"
    local profile="${MVN_PROFILES[$flavor]:-}"
    local extra="${MVN_EXTRA_ARGS[$flavor]:-}"

    if [[ -z "$compose" || ! -f "$compose" ]]; then
        echo "!!! unknown or missing compose file for flavor '$flavor' — skipping" >&2
        return 2
    fi

    local stamp; stamp="$(date +%Y%m%d)"
    local log="$LOG_DIR/build-log-${flavor}-${stamp}.txt"
    local collect="$BUILDS_DIR/$flavor"
    mkdir -p "$collect"

    {
        echo "===== OpenJPA install build against $flavor ====="
        echo "start:   $(date -Iseconds)"
        echo "host:    $(hostname)"
        echo "TZ:      $TZ"
        echo "java:    $(java -version 2>&1 | head -n1)"
        echo "maven:   $(mvn --version | head -n1)"
        echo "compose: $compose"
        echo "profile: $profile"
        echo "timeout: $MVN_TIMEOUT"
        echo

        echo "----- tearing down any previous DB containers -----"
        docker compose -f "$compose" down -v --remove-orphans || true

        echo "----- starting $flavor container -----"
        docker compose -f "$compose" up -d

        echo "----- waiting ${DB_READY_WAIT}s for DB to become ready -----"
        sleep "$DB_READY_WAIT"

        if [[ "$WIPE_M2" == "1" ]]; then
            echo "----- wiping ~/.m2/repository -----"
            rm -rf "$HOME/.m2/repository"
        fi

        local mvn_cmd=(mvn clean install "-P$profile" -fae)
        [[ -n "$extra" ]] && mvn_cmd+=($extra)

        echo "----- running: ${mvn_cmd[*]} (timeout $MVN_TIMEOUT) -----"
        (cd "$PROJECT_ROOT" && timeout "$MVN_TIMEOUT" "${mvn_cmd[@]}")
        local rc=$?

        if [[ $rc -eq 0 ]]; then
            echo
            echo "===== BUILD OK for $flavor ====="
        else
            echo
            echo "===== BUILD FAILED for $flavor with exit code $rc ====="
        fi

        echo "----- collecting surefire/failsafe reports into $collect -----"
        local count=0
        while IFS= read -r -d '' report; do
            local rel="${report#$PROJECT_ROOT/}"
            local dest="$collect/$rel"
            mkdir -p "$(dirname "$dest")"
            cp "$report" "$dest"
            count=$((count + 1))
        done < <(find "$PROJECT_ROOT" \
                    -type d \( -name surefire-reports -o -name failsafe-reports \) \
                    -prune -print0 \
                | while IFS= read -r -d '' dir; do
                    find "$dir" -type f -print0
                  done)
        echo "  collected $count files"

        echo "----- tearing down $flavor container -----"
        docker compose -f "$compose" down -v --remove-orphans || true

        echo "end: $(date -Iseconds)"
        exit $rc
    } >"$log" 2>&1
    return $?
}

FLAVORS=("$@")
if [[ ${#FLAVORS[@]} -eq 0 ]]; then
    FLAVORS=(mariadb mysql pg17 pg18)
fi

overall_rc=0
for flavor in "${FLAVORS[@]}"; do
    run_flavor "$flavor"
    rc=$?
    if [[ $rc -ne 0 ]]; then
        overall_rc=$rc
    fi
done
exit "$overall_rc"

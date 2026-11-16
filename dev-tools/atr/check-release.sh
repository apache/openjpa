#!/usr/bin/env bash
# Licensed to the Apache Software Foundation (ASF) under one
# or more contributor license agreements.  See the NOTICE file
# distributed with this work for additional information
# regarding copyright ownership.  The ASF licenses this file
# to you under the Apache License, Version 2.0 (the
# "License"); you may not use this file except in compliance
# with the License.  You may obtain a copy of the License at
#
#   http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing,
# software distributed under the License is distributed on an
# "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
# KIND, either express or implied.  See the License for the
# specific language governing permissions and limitations
# under the License.

# Runs the main checks of Apache Trusted Releases (ATR, https://releases.apache.org) on release
# archives before they are uploaded, so that blockers and concerns show up locally first:
#
#   - companion files: .asc and .sha512 (or .sha256) present, no .md5, version in the file name
#   - the .sha512/.sha256 checksum and the OpenPGP signature
#   - LICENSE and NOTICE (source: in the root directory; binary: anywhere in the archive)
#   - license headers of source files, with ATR's file patterns
#   - Apache RAT on source archives, with the RAT version and the command line that ATR uses,
#     and the .rat-excludes file of the archive
#
# The logic follows atr/tasks/checks/*.py of https://github.com/apache/tooling-trusted-releases.
# ATR runs more checks (archive structure, SBOM, file paths); its results stay authoritative.
#
# Usage: dev-tools/atr/check-release.sh [-v VERSION] [-k KEYS] ARCHIVE...
#
#   -v VERSION  require VERSION in each archive file name (ATR blocker otherwise)
#   -k KEYS     verify the signatures only against the keys in KEYS, a file or a URL such as
#               https://downloads.apache.org/openjpa/KEYS (default: your own gpg keyring)
#
# Environment:
#   RAT_VERSION  Apache RAT version, default 0.18 (_RAT_VERSION in atr/config.py)
#   RAT_JAR      path to apache-rat-<version>.jar; default: the local Maven repository, otherwise
#                it is downloaded from Maven Central into ~/.cache/openjpa-atr/
#
# Needs bash, java, python3, unzip, tar, curl and gpg. Exits with 1 if anything would be an ATR
# blocker or concern.

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
CHECKS="$SCRIPT_DIR/atr_checks.py"
RAT_VERSION="${RAT_VERSION:-0.18}"
# SHA-512 of apache-rat-0.18.jar from Maven Central; it is the same jar as in apache-rat-0.18-bin.tar.gz
RAT_018_SHA512="d77c12bfdc0cc2277bc51945a62619f3b7581bd6d6bf6ce3e029a001ff59760be0f2f2c112058d6902a7701a17c3cdd860e026d914b417639666a8a3d42fe73b"

VERSION=""
KEYS=""
while getopts "v:k:h" opt; do
    case "$opt" in
        v) VERSION="$OPTARG" ;;
        k) KEYS="$OPTARG" ;;
        *) sed -n '/^# Usage:/,/^# blocker or concern/p' "$0" | sed 's/^# \{0,1\}//'; exit 2 ;;
    esac
done
shift $((OPTIND - 1))
if [ $# -eq 0 ]; then
    echo "Usage: $0 [-v VERSION] [-k KEYS] ARCHIVE..." >&2
    exit 2
fi

FAILED=0
WORK="$(mktemp -d "${TMPDIR:-/tmp}/openjpa-atr-check.XXXXXX")"
trap 'rm -rf "$WORK"' EXIT

blocker() { echo "BLOCKER  $*"; FAILED=1; }
concern() { echo "CONCERN  $*"; FAILED=1; }
ok() { echo "OK       $*"; }
run_check() { python3 -I "$CHECKS" "$@" || FAILED=1; }

sha512_of() {
    if command -v sha512sum > /dev/null; then sha512sum "$1" | cut -d' ' -f1; else shasum -a 512 "$1" | cut -d' ' -f1; fi
}

rat_jar() {
    if [ -n "${RAT_JAR:-}" ]; then echo "$RAT_JAR"; return; fi
    local m2="$HOME/.m2/repository/org/apache/rat/apache-rat/$RAT_VERSION/apache-rat-$RAT_VERSION.jar"
    if [ -f "$m2" ]; then echo "$m2"; return; fi
    local cache="$HOME/.cache/openjpa-atr/apache-rat-$RAT_VERSION.jar"
    if [ ! -f "$cache" ]; then
        local url="https://repo1.maven.org/maven2/org/apache/rat/apache-rat/$RAT_VERSION/apache-rat-$RAT_VERSION.jar"
        mkdir -p "$(dirname "$cache")"
        echo "Downloading $url" >&2
        curl -fsSL -o "$cache.part" "$url"
        local expected
        if [ "$RAT_VERSION" = "0.18" ]; then
            expected="$RAT_018_SHA512"
            [ "$(sha512_of "$cache.part")" = "$expected" ] || { echo "SHA-512 mismatch for $url" >&2; rm -f "$cache.part"; exit 1; }
        else
            expected="$(curl -fsSL "$url.sha1")"
            [ "$(shasum -a 1 "$cache.part" | cut -d' ' -f1)" = "${expected%% *}" ] || { echo "SHA-1 mismatch for $url" >&2; rm -f "$cache.part"; exit 1; }
        fi
        mv "$cache.part" "$cache"
    fi
    echo "$cache"
}

# A GNUPGHOME with only the keys of the KEYS file, if one was given
GPG=(gpg)
if [ -n "$KEYS" ]; then
    export GNUPGHOME="$WORK/gnupg"
    mkdir -m 700 "$GNUPGHOME"
    if [ -f "$KEYS" ]; then cp "$KEYS" "$WORK/KEYS"; else curl -fsSL -o "$WORK/KEYS" "$KEYS"; fi
    gpg --batch --quiet --import "$WORK/KEYS" 2> /dev/null || true
    GPG=(gpg --batch)
fi

# ATR's classification of an archive by its file name (atr/classify.py, simplified): a source token
# makes it a source archive, otherwise a binary token makes it a binary archive, and a name with
# neither is a source archive. ATR also counts platform and distribution tokens and directory names,
# which are left out here.
is_source() {
    local name
    name="$(basename "$1")"
    shopt -s nocasematch
    local rc=0
    if [[ "$name" =~ (^|[-_.])(project|source|sources|src)([-_.]|$) ]]; then
        rc=0
    elif [[ "$name" =~ (^|[-_.])(binary-assembly|binary|bin)([-_.]|$) ]]; then
        rc=1
    fi
    shopt -u nocasematch
    return "$rc"
}

# atr/tasks/checks/rat.py: _build_rat_command and _synchronous_core
run_rat() {
    local dir="$1"
    local excludes=()
    while IFS= read -r -d '' f; do excludes+=("$f"); done < <(find "$dir" -type f -name .rat-excludes -print0)
    if [ "${#excludes[@]}" -gt 1 ]; then
        concern "Multiple .rat-excludes files not allowed (found ${#excludes[@]})"
        return
    fi
    local scan_root="$dir" extended=1
    if [ "${#excludes[@]}" -eq 1 ]; then
        scan_root="$(dirname "${excludes[0]}")"
        extended=0
        # ATR refuses archives with files outside the directory of .rat-excludes
        local outside
        outside="$(find "$dir" -type f ! -path "$scan_root/*" | wc -l | tr -d ' ')"
        if [ "$outside" -gt 0 ]; then
            concern "Files exist outside .rat-excludes directory ($outside found)"
            return
        fi
        echo "Using .rat-excludes from ${scan_root#"$dir"/}"
    else
        echo "No .rat-excludes in the archive; ATR would use the project's RAT excludes policy, if set"
    fi
    local jar report="$WORK/.atr-rat-report.xml"
    jar="$(rat_jar)"
    rm -f "$report"
    local cmd=(java -Xmx64m -XX:MaxMetaspaceSize=32m -XX:+UseSerialGC -XX:CompressedClassSpaceSize=16m
        -jar "$jar" --output-style xml --output-file "$report"
        --counter-max UNAPPROVED:-1 --counter-min LICENSE_CATEGORIES:0 LICENSE_NAMES:0 STANDARDS:0
        --input-exclude-std MISC --input-exclude-std HIDDEN_DIR --input-exclude-std MAC)
    if [ "$extended" -eq 1 ]; then
        cmd+=(--input-exclude-std MAVEN --input-exclude-std IDEA --input-exclude-std GIT)
    fi
    local suffix
    for suffix in .bundle.js .chunk.js .css.map .js.map .min.css .min.js .min.map; do
        cmd+=(--input-exclude "**/*$suffix")
    done
    cmd+=(--input-exclude .atr-rat-report.xml)
    if [ "$extended" -eq 0 ]; then
        cmd+=(--input-exclude .rat-excludes --input-exclude-file .rat-excludes)
    fi
    cmd+=(-- .)
    if ! (cd "$scan_root" && "${cmd[@]}" > "$WORK/rat.log" 2>&1); then
        concern "Apache RAT failed, see the output below"
        cat "$WORK/rat.log"
        return
    fi
    run_check rat-report "$report"
}

for archive in "$@"; do
    echo
    echo "=== $(basename "$archive")"
    if [ ! -f "$archive" ]; then
        blocker "$archive: no such file"
        continue
    fi
    archive="$(cd "$(dirname "$archive")" && pwd)/$(basename "$archive")"
    name="$(basename "$archive")"

    # atr/tasks/checks/paths.py
    [ -f "$archive.md5" ] && blocker "The use of .md5 is forbidden, please use .sha512 ($name.md5)"
    [ -f "$archive.sig" ] && blocker "Binary signature files (.sig) are forbidden, please use .asc"
    [ -z "$VERSION" ] || run_check filename-version "$name" "$VERSION"

    hash_found=0
    for alg in sha512 sha256; do
        if [ -f "$archive.$alg" ]; then
            hash_found=1
            run_check hash "$archive.$alg"
        fi
    done
    [ "$hash_found" -eq 1 ] || blocker "Missing corresponding checksum file ($name.sha256 or $name.sha512)"

    if [ -f "$archive.asc" ]; then
        if "${GPG[@]}" --status-fd 3 --verify "$archive.asc" "$archive" > "$WORK/gpg.log" 2>&1 3> "$WORK/gpg.status" \
            && grep -q '^\[GNUPG:\] GOODSIG ' "$WORK/gpg.status"; then
            ok "$name.asc: good signature from $(grep -m1 '^\[GNUPG:\] GOODSIG ' "$WORK/gpg.status" | cut -d' ' -f3-)"
        else
            blocker "$name.asc: signature does not verify"
            sed 's/^/         /' "$WORK/gpg.log"
        fi
    else
        blocker "Missing corresponding signature file ($name.asc)"
    fi

    dir="$WORK/extract/$name"
    mkdir -p "$dir"
    case "$name" in
        *.zip) unzip -q "$archive" -d "$dir" ;;
        *.tar.gz | *.tgz) tar -xzf "$archive" -C "$dir" ;;
        *) concern "Unknown archive type, not extracted"; continue ;;
    esac

    if is_source "$archive"; then
        echo "--- source archive: LICENSE and NOTICE"
        run_check license-files "$dir" source
        echo "--- license headers"
        run_check headers "$dir"
        echo "--- Apache RAT $RAT_VERSION"
        run_rat "$dir"
    else
        echo "--- binary archive: LICENSE and NOTICE"
        run_check license-files "$dir" binary
        echo "--- license headers"
        run_check headers "$dir"
    fi
    rm -rf "$dir"
done

echo
if [ "$FAILED" -ne 0 ]; then
    echo "Found problems that ATR would report as blockers or concerns."
    exit 1
fi
echo "No problems found."

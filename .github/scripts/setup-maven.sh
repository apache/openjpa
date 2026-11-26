#!/bin/bash
# Downloads a pinned Maven distribution into /opt and symlinks mvn onto PATH.
# Usage: bash .github/scripts/setup-maven.sh 3.9.16
set -euo pipefail

MVN_VER="${1:?usage: setup-maven.sh <maven-version>}"
cd /opt
wget https://archive.apache.org/dist/maven/maven-3/${MVN_VER}/binaries/apache-maven-${MVN_VER}-bin.tar.gz
tar -xzf "apache-maven-${MVN_VER}-bin.tar.gz"
ln -sf "/opt/apache-maven-${MVN_VER}/bin/mvn" /usr/local/bin/mvn
echo "M2_HOME=/opt/apache-maven-${MVN_VER}" >> "${GITHUB_ENV:-/dev/null}"

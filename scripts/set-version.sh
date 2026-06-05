#!/usr/bin/env bash
# Write the version semantic-release computed into gradle.properties, which is
# the single source of truth read by app/build.gradle.kts. Invoked from the
# @semantic-release/exec prepareCmd in .releaserc.json.
set -euo pipefail

ver="${1:?usage: set-version.sh <version>}"

if grep -qE '^VERSION_NAME=' gradle.properties; then
  sed -i -E "s/^VERSION_NAME=.*/VERSION_NAME=${ver}/" gradle.properties
else
  printf '\nVERSION_NAME=%s\n' "$ver" >> gradle.properties
fi

echo "Set VERSION_NAME=${ver} in gradle.properties"

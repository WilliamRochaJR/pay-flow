#!/usr/bin/env bash

set -euo pipefail

script_dir=$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")" && pwd)
resolver="$script_dir/resolve-artifact-context.sh"

expect_success() {
  output_file=$(mktemp)
  GITHUB_OUTPUT="$output_file" "$resolver" "$1"
  grep --fixed-strings --quiet "revision=$1" "$output_file"
  grep --fixed-strings --quiet "source_type=$2" "$output_file"
  rm -f "$output_file"
}

expect_failure() {
  output_file=$(mktemp)
  if GITHUB_OUTPUT="$output_file" "$resolver" "$1" 2>/dev/null; then
    rm -f "$output_file"
    echo "Expected artifact policy rejection for revision '$1'." >&2
    exit 1
  fi
  rm -f "$output_file"
}

expect_success develop development
expect_success release/0.6.0 release
expect_success release/1.0.0 release
expect_success v0.6.0 version
expect_success v1.0.0 version

expect_failure main
expect_failure feature/example
expect_failure release/01.2.3
expect_failure release/not-semver
expect_failure 0.6.0
expect_failure v01.2.3
expect_failure ""

echo "Artifact policy tests passed."

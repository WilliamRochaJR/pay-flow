#!/usr/bin/env bash

set -euo pipefail

script_dir=$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")" && pwd)
resolver="$script_dir/resolve-deployment-context.sh"

expect_success() {
  GITHUB_OUTPUT=$(mktemp) "$resolver" "$1" "$2"
}

expect_failure() {
  if GITHUB_OUTPUT=$(mktemp) "$resolver" "$1" "$2" 2>/dev/null; then
    echo "Expected policy rejection for environment '$1' and revision '$2'." >&2
    exit 1
  fi
}

expect_success development develop
expect_success homologation release/0.2.0
expect_success production v0.1.0

expect_failure development main
expect_failure homologation develop
expect_failure homologation release/not-semver
expect_failure production main
expect_failure production 0.1.0
expect_failure unknown develop

echo "Deployment policy tests passed."

#!/usr/bin/env bash

set -euo pipefail

script_dir=$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")" && pwd)
resolver="$script_dir/resolve-deployment-context.sh"

expect_success() {
  output_file=$(mktemp)
  GITHUB_OUTPUT="$output_file" "$resolver" "$1" "$2"
  grep --fixed-strings --quiet "target_environment=$1" "$output_file"
  grep --fixed-strings --quiet "tf_working_dir=infra/environments/production" "$output_file"
  grep --fixed-strings --quiet "tf_state_key=payflow/$1/terraform.tfstate" "$output_file"
  grep --fixed-strings --quiet "runtime_parameter=/payflow/$1/runtime-env" "$output_file"
  grep --fixed-strings --quiet "lease_key=payflow/leases/$1.json" "$output_file"
  rm -f "$output_file"
}

expect_failure() {
  output_file=$(mktemp)
  if GITHUB_OUTPUT="$output_file" "$resolver" "$1" "$2" 2>/dev/null; then
    rm -f "$output_file"
    echo "Expected policy rejection for environment '$1' and revision '$2'." >&2
    exit 1
  fi
  rm -f "$output_file"
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

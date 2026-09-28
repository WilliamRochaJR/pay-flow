#!/usr/bin/env bash

set -euo pipefail

target_environment=${1:-}
revision=${2:-}

fail() {
  echo "Deployment policy rejected: $1" >&2
  exit 1
}

case "$target_environment" in
  development)
    test "$revision" = "develop" || fail "development accepts only the develop branch."
    ;;
  homologation)
    [[ "$revision" =~ ^release/[0-9]+\.[0-9]+\.[0-9]+$ ]] ||
      fail "homologation accepts only release/X.Y.Z branches."
    ;;
  production)
    [[ "$revision" =~ ^v(0|[1-9][0-9]*)\.(0|[1-9][0-9]*)\.(0|[1-9][0-9]*)$ ]] ||
      fail "production accepts only vX.Y.Z tags."
    ;;
  *)
    fail "unknown environment '$target_environment'."
    ;;
esac

output_file=${GITHUB_OUTPUT:-/dev/stdout}
{
  echo "target_environment=$target_environment"
  echo "tf_working_dir=infra/environments/production"
  echo "tf_state_key=payflow/$target_environment/terraform.tfstate"
  echo "runtime_parameter=/payflow/$target_environment/runtime-env"
  echo "lease_key=payflow/leases/$target_environment.json"
  echo "plan_file=$target_environment.tfplan"
} >> "$output_file"

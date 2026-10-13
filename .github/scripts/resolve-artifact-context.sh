#!/usr/bin/env bash

set -euo pipefail

revision=${1:-}

fail() {
  echo "Artifact policy rejected: $1" >&2
  exit 1
}

case "$revision" in
  develop)
    source_type=development
    ;;
  release/*)
    [[ "$revision" =~ ^release/(0|[1-9][0-9]*)\.(0|[1-9][0-9]*)\.(0|[1-9][0-9]*)$ ]] ||
      fail "release artifacts accept only release/X.Y.Z branches."
    source_type=release
    ;;
  v*)
    [[ "$revision" =~ ^v(0|[1-9][0-9]*)\.(0|[1-9][0-9]*)\.(0|[1-9][0-9]*)$ ]] ||
      fail "production artifacts accept only vX.Y.Z tags."
    source_type=version
    ;;
  *)
    fail "use develop, release/X.Y.Z or vX.Y.Z."
    ;;
esac

output_file=${GITHUB_OUTPUT:-/dev/stdout}
{
  echo "revision=$revision"
  echo "source_type=$source_type"
} >>"$output_file"

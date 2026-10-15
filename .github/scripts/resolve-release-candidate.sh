#!/usr/bin/env bash

set -euo pipefail

version=${1:-}
main_revision=${2:-}
remote=${3:-origin}

fail() {
  echo "Release candidate rejected: $1" >&2
  exit 1
}

[[ "$version" =~ ^(0|[1-9][0-9]*)\.(0|[1-9][0-9]*)\.(0|[1-9][0-9]*)$ ]] ||
  fail "version must use MAJOR.MINOR.PATCH without a v prefix."

test -n "$main_revision" || fail "the main revision is required."
git rev-parse --verify --quiet "${main_revision}^{commit}" >/dev/null ||
  fail "main revision '$main_revision' is not available in the repository."

release_branch="release/$version"
release_ref="refs/remotes/$remote/$release_branch"

git fetch --quiet --force "$remote" \
  "+refs/heads/$release_branch:$release_ref" ||
  fail "remote branch '$release_branch' does not exist. Keep it until the release is published."

release_sha=$(git rev-parse --verify "${release_ref}^{commit}")
git merge-base --is-ancestor "$release_sha" "$main_revision" ||
  fail "'$release_branch' at $release_sha is not contained in main revision $main_revision."

output_file=${GITHUB_OUTPUT:-/dev/stdout}
{
  echo "release_branch=$release_branch"
  echo "release_sha=$release_sha"
  echo "tag=v$version"
} >>"$output_file"

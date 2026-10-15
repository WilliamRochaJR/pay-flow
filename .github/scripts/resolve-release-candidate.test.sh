#!/usr/bin/env bash

set -euo pipefail

script_dir=$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")" && pwd)
resolver="$script_dir/resolve-release-candidate.sh"
test_dir=$(mktemp -d)
trap 'rm -rf "$test_dir"' EXIT

git init --quiet --bare "$test_dir/origin.git"
git init --quiet --initial-branch=main "$test_dir/work"
git -C "$test_dir/work" config user.name 'Release Policy Test'
git -C "$test_dir/work" config user.email 'release-policy@example.invalid'
git -C "$test_dir/work" remote add origin "$test_dir/origin.git"

touch "$test_dir/work/base"
git -C "$test_dir/work" add base
git -C "$test_dir/work" commit --quiet -m 'base'
git -C "$test_dir/work" push --quiet --set-upstream origin main

git -C "$test_dir/work" switch --quiet -c release/0.7.0
touch "$test_dir/work/release"
git -C "$test_dir/work" add release
git -C "$test_dir/work" commit --quiet -m 'release candidate'
release_sha=$(git -C "$test_dir/work" rev-parse HEAD)
git -C "$test_dir/work" push --quiet --set-upstream origin release/0.7.0

git -C "$test_dir/work" switch --quiet main
git -C "$test_dir/work" merge --quiet --no-ff release/0.7.0 -m 'merge release'
main_sha=$(git -C "$test_dir/work" rev-parse HEAD)

output_file="$test_dir/output"
(
  cd "$test_dir/work"
  GITHUB_OUTPUT="$output_file" "$resolver" 0.7.0 "$main_sha"
)
grep --fixed-strings --quiet 'release_branch=release/0.7.0' "$output_file"
grep --fixed-strings --quiet "release_sha=$release_sha" "$output_file"
grep --fixed-strings --quiet 'tag=v0.7.0' "$output_file"

expect_failure() {
  if (
    cd "$test_dir/work"
    GITHUB_OUTPUT="$test_dir/rejected-output" "$resolver" "$@" 2>/dev/null
  ); then
    echo "Expected release candidate policy rejection for: $*" >&2
    exit 1
  fi
}

expect_failure 01.7.0 "$main_sha"
expect_failure 0.8.0 "$main_sha"

git -C "$test_dir/work" switch --quiet -c release/0.8.0
touch "$test_dir/work/unmerged"
git -C "$test_dir/work" add unmerged
git -C "$test_dir/work" commit --quiet -m 'unmerged candidate'
git -C "$test_dir/work" push --quiet --set-upstream origin release/0.8.0
expect_failure 0.8.0 "$main_sha"

echo 'Release candidate policy tests passed.'

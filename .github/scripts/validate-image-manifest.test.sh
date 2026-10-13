#!/usr/bin/env bash

set -euo pipefail

script_dir=$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")" && pwd)
validator="$script_dir/validate-image-manifest.sh"
temp_dir=$(mktemp -d)
trap 'rm -rf "$temp_dir"' EXIT

revision_sha=aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa
api_digest=sha256:bbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbb
frontend_digest=sha256:cccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccc

write_manifest() {
  jq -n \
    --arg revision_sha "${1:-$revision_sha}" \
    --arg api_reference "${2:-123456789012.dkr.ecr.us-east-1.amazonaws.com/payflow-api@$api_digest}" \
    --arg api_digest "${3:-$api_digest}" \
    --arg frontend_reference "${4:-123456789012.dkr.ecr.us-east-1.amazonaws.com/payflow-frontend@$frontend_digest}" \
    --arg frontend_digest "${5:-$frontend_digest}" \
    '{
      schemaVersion: 1,
      revisionSha: $revision_sha,
      repository: "example/pay-flow",
      workflowRunId: "38483955527",
      images: {
        api: {reference: $api_reference, digest: $api_digest},
        frontend: {reference: $frontend_reference, digest: $frontend_digest}
      }
    }' >"$temp_dir/manifest.json"
}

expect_failure() {
  if GITHUB_OUTPUT="$temp_dir/output" "$validator" \
    "$temp_dir/manifest.json" "$revision_sha" 38483955527 example/pay-flow 2>/dev/null; then
    echo "Expected image manifest validation to fail: $1" >&2
    exit 1
  fi
}

write_manifest
GITHUB_OUTPUT="$temp_dir/output" "$validator" \
  "$temp_dir/manifest.json" "$revision_sha" 38483955527 example/pay-flow
grep --fixed-strings --quiet "api_image=123456789012.dkr.ecr.us-east-1.amazonaws.com/payflow-api@$api_digest" "$temp_dir/output"
grep --fixed-strings --quiet "frontend_image=123456789012.dkr.ecr.us-east-1.amazonaws.com/payflow-frontend@$frontend_digest" "$temp_dir/output"
grep --fixed-strings --quiet 'artifact_workflow_run_id=38483955527' "$temp_dir/output"

write_manifest dddddddddddddddddddddddddddddddddddddddd
expect_failure "different revision SHA"

write_manifest
jq '.workflowRunId = "999"' "$temp_dir/manifest.json" >"$temp_dir/altered.json"
mv "$temp_dir/altered.json" "$temp_dir/manifest.json"
expect_failure "different workflow run"

write_manifest
jq '.repository = "another/project"' "$temp_dir/manifest.json" >"$temp_dir/altered.json"
mv "$temp_dir/altered.json" "$temp_dir/manifest.json"
expect_failure "different repository"

write_manifest "$revision_sha" "123456789012.dkr.ecr.us-east-1.amazonaws.com/payflow-api:latest"
expect_failure "mutable API tag"

write_manifest "$revision_sha" "123456789012.dkr.ecr.us-east-1.amazonaws.com/another-api@$api_digest"
expect_failure "unexpected API repository"

write_manifest "$revision_sha" "123456789012.dkr.ecr.us-east-1.amazonaws.com/payflow-api@$api_digest" "sha256:dddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddd"
expect_failure "API reference and digest mismatch"

write_manifest "$revision_sha" \
  "123456789012.dkr.ecr.us-east-1.amazonaws.com/payflow-api@$api_digest" \
  "$api_digest" \
  "111111111111.dkr.ecr.us-east-1.amazonaws.com/payflow-frontend@$frontend_digest"
expect_failure "different registries"

echo "Image manifest validation tests passed."

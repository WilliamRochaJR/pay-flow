#!/usr/bin/env bash

set -euo pipefail

manifest_file=${1:-}
expected_revision_sha=${2:-}
expected_workflow_run_id=${3:-}
expected_repository=${4:-}

fail() {
  echo "Image manifest rejected: $1" >&2
  exit 1
}

test -n "$manifest_file" || fail "provide the manifest file."
test -f "$manifest_file" || fail "manifest file does not exist."
[[ "$expected_revision_sha" =~ ^[a-f0-9]{40}$ ]] || fail "expected revision must be a full Git SHA."
jq empty "$manifest_file" 2>/dev/null || fail "manifest must contain valid JSON."

schema_version=$(jq -r '.schemaVersion // empty' "$manifest_file")
revision_sha=$(jq -r '.revisionSha // empty' "$manifest_file")
workflow_run_id=$(jq -r '.workflowRunId // empty' "$manifest_file")
repository=$(jq -r '.repository // empty' "$manifest_file")
api_reference=$(jq -r '.images.api.reference // empty' "$manifest_file")
api_digest=$(jq -r '.images.api.digest // empty' "$manifest_file")
frontend_reference=$(jq -r '.images.frontend.reference // empty' "$manifest_file")
frontend_digest=$(jq -r '.images.frontend.digest // empty' "$manifest_file")

test "$schema_version" = 1 || fail "unsupported schema version."
test "$revision_sha" = "$expected_revision_sha" || fail "revision SHA does not match the selected deployment."
test "$workflow_run_id" = "$expected_workflow_run_id" || fail "workflow run ID does not match the artifact origin."
test "$repository" = "$expected_repository" || fail "repository does not match the current GitHub project."
[[ "$api_digest" =~ ^sha256:[a-f0-9]{64}$ ]] || fail "API digest is invalid."
[[ "$frontend_digest" =~ ^sha256:[a-f0-9]{64}$ ]] || fail "front-end digest is invalid."
[[ "$api_reference" =~ ^[0-9]{12}\.dkr\.ecr\.[a-z0-9-]+\.amazonaws\.com/payflow-api@sha256:[a-f0-9]{64}$ ]] ||
  fail "API reference must use the PayFlow ECR repository and a digest."
[[ "$frontend_reference" =~ ^[0-9]{12}\.dkr\.ecr\.[a-z0-9-]+\.amazonaws\.com/payflow-frontend@sha256:[a-f0-9]{64}$ ]] ||
  fail "front-end reference must use the PayFlow ECR repository and a digest."
test "${api_reference##*@}" = "$api_digest" || fail "API reference and digest differ."
test "${frontend_reference##*@}" = "$frontend_digest" || fail "front-end reference and digest differ."

api_registry=${api_reference%%/*}
frontend_registry=${frontend_reference%%/*}
test "$api_registry" = "$frontend_registry" || fail "images must belong to the same ECR registry."

output_file=${GITHUB_OUTPUT:-/dev/stdout}
{
  echo "api_image=$api_reference"
  echo "api_digest=$api_digest"
  echo "frontend_image=$frontend_reference"
  echo "frontend_digest=$frontend_digest"
  echo "registry=$api_registry"
  echo "artifact_workflow_run_id=$workflow_run_id"
} >>"$output_file"

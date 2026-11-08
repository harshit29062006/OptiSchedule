#!/usr/bin/env bash
# Verifies that the committed repository builds and passes tests on its own.
# Usage: scripts/verify_clean_clone.sh <repo-url-or-path> [tag]
set -euo pipefail

REPO="${1:?usage: verify_clean_clone.sh <repo-url-or-path> [tag]}"
TAG="${2:-}"
WORK="$(mktemp -d)"

echo "Cloning $REPO into $WORK"
git clone --quiet "$REPO" "$WORK/repo"
cd "$WORK/repo"

if [ -n "$TAG" ]; then
  echo "Checking out $TAG"
  git checkout --quiet "$TAG"
fi

echo "Running the test suite on a clean copy"
mvn -q test

echo "CLEAN CLONE BUILD PASSED at $(git rev-parse --short HEAD) ($(git describe --tags --always))"
echo "Removing $WORK"
rm -rf "$WORK"

#!/usr/bin/env bash
set -euo pipefail

# ============================================================================
# Helper script to publish the local Git repository to your GitHub account.
# Prerequisite: Install GitHub CLI (`gh auth login`) OR pass a remote URL.
# ============================================================================

REPO_NAME="${1:-rupeek-maid-booking-sde3}"

if command -v gh >/dev/null 2>&1; then
    echo "Creating public GitHub repository '${REPO_NAME}' and pushing branches..."
    gh repo create "${REPO_NAME}" --public --source=. --remote=origin --push
    git push -u origin --all
    echo "Published! Repository URL:"
    gh repo view --web || gh repo view --json url -q .url
else
    echo "GitHub CLI (gh) not detected."
    echo "Run the following commands after creating an empty repo on https://github.com/new :"
    echo "  git remote add origin https://github.com/<your-username>/${REPO_NAME}.git"
    echo "  git push -u origin main"
    echo "  git push -u origin feat/sde3-maid-booking-core"
fi

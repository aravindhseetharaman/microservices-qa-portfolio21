#!/usr/bin/env bash
# Full contract-testing flow against a local Pact Broker:
#   consumer test -> publish pact -> provider verify -> can-i-deploy -> record deployment
#
# Usage: ./pact-flow.sh                     (version = current git commit, branch = current git branch)
#        CONSUMER_VERSION=1.1.0 PROVIDER_VERSION=1.1.0 ./pact-flow.sh
set -euo pipefail
cd "$(dirname "$0")"

# Default version = short commit SHA, like CI. If there are uncommitted changes, the code
# isn't the committed code, so add "-dirty-<timestamp>" to keep every run's version unique.
git_version() {
    local sha
    sha=$(git rev-parse --short HEAD 2>/dev/null) || { echo "1.0.0"; return; }
    if [ -n "$(git status --porcelain)" ]; then
        echo "${sha}-dirty-$(date +%Y%m%d%H%M%S)"
    else
        echo "$sha"
    fi
}
GIT_VERSION=$(git_version)
GIT_BRANCH=$(git rev-parse --abbrev-ref HEAD 2>/dev/null || echo main)

CONSUMER_VERSION=${CONSUMER_VERSION:-$GIT_VERSION}
PROVIDER_VERSION=${PROVIDER_VERSION:-$GIT_VERSION}
BRANCH=${BRANCH:-$GIT_BRANCH}
ENVIRONMENT=${ENVIRONMENT:-production}

printf 'Consumer version: %s\nProvider version: %s\nBranch:           %s\n' \
    "$CONSUMER_VERSION" "$PROVIDER_VERSION" "$BRANCH"

cli() { docker compose run --rm pact-cli "$@"; }
step() { printf '\n\033[1;34m==> %s\033[0m\n' "$1"; }

step "1. Start Pact Broker (http://localhost:9292, pact/pact)"
docker compose up -d pact-broker
until curl -sf -u pact:pact http://localhost:9292/diagnostic/status/heartbeat >/dev/null; do sleep 2; done

step "2. Consumer: run Pact tests (generates the contract)"
(cd checkout-service && mvn -q -B test)

step "3. Publish pact: CheckoutService $CONSUMER_VERSION ($BRANCH)"
cli publish /pacts --consumer-app-version "$CONSUMER_VERSION" --branch "$BRANCH"

step "4. Provider: verify pact and publish results: InventoryService $PROVIDER_VERSION"
(cd inventory-service && mvn -q -B verify \
    -Dpact.provider.version="$PROVIDER_VERSION" \
    -Dpact.provider.branch="$BRANCH" \
    -Dpact.verifier.publishResults=true)

step "5. can-i-deploy InventoryService $PROVIDER_VERSION -> $ENVIRONMENT"
cli broker can-i-deploy --pacticipant InventoryService --version "$PROVIDER_VERSION" --to-environment "$ENVIRONMENT"
cli broker record-deployment --pacticipant InventoryService --version "$PROVIDER_VERSION" --environment "$ENVIRONMENT"

step "6. can-i-deploy CheckoutService $CONSUMER_VERSION -> $ENVIRONMENT"
cli broker can-i-deploy --pacticipant CheckoutService --version "$CONSUMER_VERSION" --to-environment "$ENVIRONMENT"
cli broker record-deployment --pacticipant CheckoutService --version "$CONSUMER_VERSION" --environment "$ENVIRONMENT"

step "Done. Matrix: http://localhost:9292/matrix/provider/InventoryService/consumer/CheckoutService"

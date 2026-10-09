# Pact Contract Testing: Checkout ↔ Inventory

A minimal, working example of **consumer-driven contract testing** with [Pact](https://docs.pact.io),
a local **Pact Broker**, and a **`can-i-deploy`** release gate.

| Service | Role | What it does |
|---|---|---|
| `checkout-service` | **Consumer** | Asks InventoryService whether an item is in stock |
| `inventory-service` | **Provider** | Serves `GET /items/{id}` → `200` (item JSON) or `404` |

## How it works

```
 checkout-service                Pact Broker                 inventory-service
 ────────────────                ───────────                 ─────────────────
 1. Pact test runs  ──pact──▶  2. stores contract  ──pact──▶  3. verifies against
    (mock provider)              + results         ◀─result──    real app
                                     │
                       4. can-i-deploy ──▶ yes / no
                       5. record-deployment
```

1. **Consumer test** (`InventoryClientPactTest`) runs `InventoryClient` against a Pact mock server
   and writes the contract to `checkout-service/target/pacts/CheckoutService-InventoryService.json`.
2. **Publish**: the contract is uploaded to the broker with a version and branch.
3. **Provider verification** (`InventoryServicePactIT`) boots the real InventoryService, replays
   every interaction, and publishes the result to the broker.
4. **`can-i-deploy`** asks the broker: *is this version compatible with what's in production?*
5. **`record-deployment`** tells the broker what is now live, so the next check is accurate.

## The contract

| Provider state | Request | Expected response |
|---|---|---|
| `item 42 exists` | `GET /items/42` | `200`, JSON body with `id` (int), `name` (string), `stock` (int) |
| `item 99 does not exist` | `GET /items/99` | `404` |

The body uses **type matchers**: the provider must return the right fields and types, not the exact values.

## Prerequisites

- Java 21+ and Maven 3.9+
- Docker Desktop (running)

## Run everything

```bash
./pact-flow.sh
```

By default, both services are versioned with the **current git commit** (e.g. `2c3f3b4`), the same as CI,
and the branch is the current git branch. If you have uncommitted changes, the version becomes
`<sha>-dirty-<timestamp>`, so a run of uncommitted code never reuses a commit's version.

To set versions yourself:

```bash
CONSUMER_VERSION=1.1.0 PROVIDER_VERSION=1.1.0 ./pact-flow.sh
```

Pact Broker UI: <http://localhost:9292> (login `pact` / `pact`)

## Run steps manually

```bash
# Start the broker
docker compose up -d pact-broker

# 1. Consumer test → generates the pact file
cd checkout-service && mvn test && cd ..

# 2. Publish the pact
docker compose run --rm pact-cli publish /pacts --consumer-app-version 1.0.0 --branch main

# 3. Provider verification (publishes results)
cd inventory-service && mvn verify \
  -Dpact.provider.version=1.0.0 -Dpact.provider.branch=main -Dpact.verifier.publishResults=true && cd ..

# 4. Can I deploy?
docker compose run --rm pact-cli broker can-i-deploy \
  --pacticipant CheckoutService --version 1.0.0 --to-environment production

# 5. Record the deployment
docker compose run --rm pact-cli broker record-deployment \
  --pacticipant CheckoutService --version 1.0.0 --environment production
```

## CI/CD pipeline

`.github/workflows/contract-tests.yml` runs the same flow on GitHub Actions, using the **commit SHA**
as the version of both services.

| Trigger | What runs |
|---|---|
| Pull request / any branch | Consumer test → publish → provider verify → `can-i-deploy` (the two versions together) |
| Push to `main` | All of the above, then: `can-i-deploy` InventoryService → deploy → record → `can-i-deploy` CheckoutService → deploy → record |

If any step fails (for example, the provider breaks the contract), the pipeline stops and nothing is deployed.

The **Deploy** steps are placeholders (`echo`). Replace them with your real deployment.

**Note:** the pipeline starts a throwaway Pact Broker inside each run, so production history doesn't
carry over between runs. For a real setup, use a persistent broker such as [PactFlow](https://pactflow.io)
(free tier available): remove the `services:` block and set `PACT_BROKER_BASE_URL` and the credentials from
repository secrets.

## Provider verification setup

`InventoryServicePactIT` follows Pact's recommended configuration:

- **Consumer version selectors**: `mainBranch`, `matchingBranch`, `deployedOrReleased`, so the
  provider always verifies against what consumers have live in production
- **Pending pacts** (`enablePendingPacts = "true"`): a brand-new consumer expectation can't break
  the provider build until it has been verified once
- **Provider states**: `@State` methods set up test data in `ItemRepository` before each interaction

The verification test is named `*IT`, so it runs in `mvn verify` (failsafe), not `mvn test`. That's
because it needs a running broker.

## Try breaking the contract

Rename `stock` to `quantity` in `inventory-service/.../Item.java`, then run:

```bash
./pact-flow.sh
```

Provider verification fails at step 4, the failure is published to the broker, and the script stops.
If you then ask the gate directly, it answers **"Computer says no"**:

```bash
docker compose run --rm pact-cli broker can-i-deploy \
  --pacticipant InventoryService --version <the version printed at the start of the run> --to-environment production
```

The breaking change is caught before it reaches production.

## Project layout

```
.
├── .github/workflows/
│   └── contract-tests.yml     # CI/CD pipeline
├── checkout-service/          # consumer
│   └── src/test/.../InventoryClientPactTest.java
├── inventory-service/         # provider
│   └── src/test/.../InventoryServicePactIT.java
├── docker-compose.yml         # Pact Broker + Pact CLI
└── pact-flow.sh               # end-to-end flow
```

## Clean up

```bash
docker compose down
```

The broker uses an in-container SQLite database, so its data resets when the container is removed.

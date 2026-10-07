"""Turns the Pact Broker /matrix JSON (read from stdin) into a Markdown table."""
import json
import sys

matrix = json.load(sys.stdin).get("matrix", [])

print("## Pact matrix\n")
if not matrix:
    print("No contracts found in the broker.")
    sys.exit()

print("| Consumer | Consumer version | Provider | Provider version | Verified? |")
print("|---|---|---|---|---|")
for row in matrix:
    consumer = row["consumer"]
    provider = row.get("provider") or {}
    result = row.get("verificationResult")
    status = "—" if result is None else ("✅ passed" if result["success"] else "❌ failed")
    print(
        f"| {consumer['name']} | `{consumer['version']['number'][:7]}` "
        f"| {provider.get('name', '—')} | `{((provider.get('version') or {}).get('number') or '—')[:7]}` "
        f"| {status} |"
    )

# Troubleshooting Guide – EDC Data Space PoC (dsazure / file-server)

This guide helps you diagnose and fix common issues when running the EDC PoC with:
- provider connector
- vault + vault-init seeding
- nginx file-server (HTTP)
- optional consumer connector and MinIO (if used as destination)

Golden rule:
1) Check logs
2) Check network reachability (especially callbacks)
3) Check configuration values (URLs, ports, secrets)


## 0. Quick Smoke test

### Containers running
docker ps

Expected:
- provider
- vault
- vault-init (runs once, then exits)
- file-server

If something is restarting:
    
    docker compose logs <service>


### Vault reachable and seeded
Check Vault health from host:

    curl -s http://localhost:8200/v1/sys/health

Check vault-init completed successfully:

    docker compose logs vault-init

Vault-init must end with:
"Vault init done."

If provider depends on Vault secrets, restart provider after seeding:

    docker compose restart provider


### File-server reachable (IMPORTANT)
Current mapping:
127.0.0.1:8080 -> nginx:80

From host:

    curl -I http://127.0.0.1:8080/
    curl -I http://127.0.0.1:8080/<file-name>


### Provider ports reachable
Provider exposes:
19191
19192
19193 (management)
19194 (protocol)
19291

From host:

    curl http://localhost:19194/protocol
    curl http://localhost:19193/management/v3/health || true

If unsure, inspect provider logs:

    docker compose logs provider | tail -n 100


## 1. Where to Look First (Log Map)

Provider logs:
- asset bootstrap
- catalog offers
- contract negotiation
- transfer processes
- HTTP source fetch errors

Vault logs:
- secret not found
- permission denied

Vault-init logs:
- missing or failed secret seeding

File-server (nginx) logs:
- incoming requests
- 404 vs 200 responses

Follow logs live:

    docker compose logs -f provider
    docker compose logs -f vault
    docker compose logs -f file-server


## 2. Common Problems & Fast Fixes


### File-server not reachable from provider container

Symptoms:
- provider logs show connection refused or timeout
- nginx logs show no requests

Cause:
- file-server bound to 127.0.0.1 on host

Fix (recommended):
Change port mapping to:
8080:80

Then restart:

    docker compose up -d

Inside provider container, test:

    docker exec -it provider sh -lc "wget -S -O- http://file-server/ | head"

Use URL in EDC config:

    http://file-server/<file-name>


### File-server returns 404

Symptoms:
- curl returns 404
- provider logs show resource not found

Checks:

    ls -lah /home/Dataspace/shared

Remember:
- nginx root = /usr/share/nginx/html
- files in /home/Dataspace/shared map to /

Example:

    http://<host>:8080/test.txt


### File-server returns 403 Forbidden

Cause:
- file permissions

Fix:

    chmod -R a+rX /home/Dataspace/shared


### Catalog request returns empty

Symptoms:
- catalog/request returns no datasets

Likely causes:
- wrong counterPartyAddress
- provider bootstrap not executed
- wrong protocol port

Fix:
- check provider logs for bootstrap
- verify protocol endpoint reachable:

      curl http://<provider-host>:19194/protocol
- ensure management vs protocol ports are not mixed


### Contract negotiation stuck in REQUESTED

Symptoms:
- negotiation never finalizes

Likely causes:
- callback URL unreachable
- using localhost in callback
- firewall / NSG blocking inbound traffic

Test:

    curl http://<consumer-callback-host>:<port>/<path>

Fix:
- use routable IP or hostname
- open inbound ports
- restart negotiation


### Transfer fails (HTTP source)

Symptoms:
- transfer process fails
- error mentions HTTP GET failed

Likely causes:
- source URL points to localhost
- file-server unreachable from provider
- wrong file path

Fix:
Test from inside provider container:

    docker exec -it provider sh -lc "wget -S -O- http://file-server/<file-name> | head"

If this fails, EDC will fail.


### Vault secrets not found

Symptoms:
- provider logs mention missing Vault secrets

Fix:

    docker compose logs vault-init
    docker compose restart provider

Optional manual check:

    docker exec -it vault sh
    export VAULT_ADDR=http://127.0.0.1:8200
    export VAULT_TOKEN=<root-token>
    vault kv get secret/provider-key


### 401 Unauthorized on management API

Symptoms:
- 401 on /management/v3/*

Causes:
- missing X-Api-Key
- wrong port

Fix:
Use header:
X-Api-Key: password

Ensure calls go to management port (19193)


## 3. EDC State Machine Cheat Sheet

ContractNegotiation:
REQUESTED → CONFIRMED → FINALIZED

If stuck:
- check callback reachability
- inspect provider logs


TransferProcess:
REQUESTED → STARTED → COMPLETED (or FAILED)

If failed:
- source URL reachability
- destination endpoint config
- data plane logs


## 4. Reset & Recover

Soft reset:

    docker compose down
    docker compose up -d --build

Hard reset (removes all state):

    docker compose down -v
    docker system prune -f
    docker compose up -d --build


## 5. Debug Pack (when asking for help)

Provide:
- provider logs (last 200 lines)
- nginx logs
- exact JSON used
- network topology

Commands:

    docker compose logs --tail=200 provider
    docker compose logs --tail=200 file-server

# EDC Proof of Concept (EDC-PoC)

This project demonstrates a simple setup of **Provider** and **Consumer** connectors.

---

## Build Project

Generate the Gradle wrapper.

./gradlew wrapper


Build JARs:

./gradlew build


Or build only shadow JARs:


./gradlew :consumer:shadowJar  
./gradlew :provider:shadowJar

---

## Start Docker Containers

### Provider
docker compose -f provider/resources/docker-compose-provider.yaml up -d

### Consumer
docker compose -f consumer/resources/docker-compose-consumer.yaml up -d

---


## Configure Vault

export VAULT_ADDR='http://0.0.0.0:8200'  
export VAULT_TOKEN='<root-token>'  
vault kv put secret/accessKeyId content=consumer  
vault kv put secret/secretAccessKey content=password  
vault kv put secret/provider-key content=password

---

## Boot Connectors

### Provider Connector
java -Dedc.fs.config=provider/config.properties -jar provider/build/libs/provider-all.jar

### Consumer Connector
java -Dedc.fs.config=consumer/config.properties -jar consumer/build/libs/consumer-all.jar

---

## Provider Catalog Bootstrapping

The provider automatically creates the following at startup:
- **Asset**: `id=1`
- **PolicyDefinition**: `id=1` with `USE`
- **ContractDefinition**: `id=1` selecting asset `1`

---

# Consumer Steps

## 1. Setup Minio Bucket
- Login to Minio at http://localhost:9001
- Use credentials:
    - **Username**: `consumer`
    - **Password**: `password`
- Create a bucket named: `src-bucket`

---

## 2. Consumer API Calls

### Fetch Catalog
curl -X POST "http://100.78.21.5:29193/management/v3/catalog/request" -H "X-Api-Key: password" -H "Content-Type: application/json" -d @consumer/resources/fetch-catalog.json -s | jq

### Negotiate Contract
curl -d @consumer/resources/negotiate-contract.json -H "X-Api-Key: password" -H "Content-Type: application/json" -X POST http://100.78.21.5:29193/management/v3/contractnegotiations -s | jq

### Get Contract ID
curl -X GET "http://100.78.21.5:29193/management/v3/contractnegotiations/<contract-id>" -H "X-Api-Key: password" -H "Content-Type: application/json" -s | jq

### Start Transfer
curl -X POST "http://100.78.21.5:29193/management/v3/transferprocesses" -H "X-Api-Key: password" -H "Content-Type: application/json" -d @consumer/resources/start-transfer.json -s | jq

---

## Postgres → Postgres transfer (via EDC negotiation)

Prerequisites:
- The Provider DB has a table named `users`.
- The Consumer DB has connectivity and credentials to write to a table `users_incoming` (it will be created by your sink depending on the JDBC data plane module; otherwise pre-create it).
- Your runtimes include a JDBC/SQL-capable data plane extension. If not, use the HTTP fallback in EDC samples.

Enable JDBC data-plane in both runtimes:
- Open consumer/build.gradle.kts and provider/build.gradle.kts
- In dependencies, uncomment ONE of the JDBC DP dependency lines under "JDBC Data-Plane extension" that matches your distribution. If you own a private JDBC DP module, add its coordinates there.
- Rebuild both shadow JARs.

Named datasource configuration is already present in both config.properties:
- Provider: edc.datasource.pg-ds.*
- Consumer: edc.datasource.consumer-pg-ds.*

If your JDBC DP expects inline connection properties instead, the included JSONs already provide inline keys (jdbc:url, jdbc:user, ...), and you can leave the named datasource props unused.

1) Create Provider catalog entries (Asset, Policy, ContractDefinition)

curl -X POST "http://100.93.225.17:19193/management/v3/assets" -H "X-Api-Key: password" -H "Content-Type: application/json" -d @provider/resources/asset-users.json -s | jq

curl -X POST "http://100.93.225.17:19193/management/v3/policydefinitions" -H "X-Api-Key: password" -H "Content-Type: application/json" -d @provider/resources/policy-allow-all.json -s | jq

curl -X POST "http://100.93.225.17:19193/management/v3/contractdefinitions" -H "X-Api-Key: password" -H "Content-Type: application/json" -d @provider/resources/contractdefinition-users.json -s | jq

2) From Consumer, fetch Catalog and locate the offer for asset `asset-users`

curl -X POST "http://100.78.21.5:29193/management/v3/catalog/request" -H "X-Api-Key: password" -H "Content-Type: application/json" -d @consumer/resources/fetch-catalog.json -s | jq

3) Start Contract Negotiation (replace <OFFER_ID_FROM_CATALOG> first)

curl -X POST "http://100.78.21.5:29193/management/v3/contractnegotiations" -H "X-Api-Key: password" -H "Content-Type: application/json" -d @consumer/resources/negotiate-contract.json -s | jq

4) Poll negotiation until FINALIZED and copy `contractAgreementId`

curl -X GET "http://100.78.21.5:29193/management/v3/contractnegotiations/<NEGOTIATION_ID>" -H "X-Api-Key: password" -H "Content-Type: application/json" -s | jq

5) Start Transfer (replace <CONTRACT_AGREEMENT_ID>)

curl -X POST "http://100.78.21.5:29193/management/v3/transferprocesses" -H "X-Api-Key: password" -H "Content-Type: application/json" -d @consumer/resources/start-transfer-postgres.json -s | jq

Notes:
- The JSONs use a "JdbcData" dataAddress with common keys (edc:jdbc:url, edc:sql:query/table). Adjust to the exact keys expected by your JDBC data-plane extension.
- If you don’t have a JDBC data-plane, use the HTTP-based sample from EDC and ingest into Postgres with your own job.

---

## Clean Up

### Stop Provider Containers
docker compose -f provider/resources/docker-compose-provider.yaml down

### Stop Consumer Containers
docker compose -f consumer/resources/docker-compose-consumer.yaml down
---

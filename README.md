# EDC Proof of Concept (EDC-PoC)

This project demonstrates a simple setup of Provider and Consumer connectors. Originally built around blob storage, it now also showcases querying a Postgres database on the Provider and transferring the query result via EDC.

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
- Starts Azurite (kept for compatibility), Vault, and Postgres.

docker compose -f provider/resources/docker-compose-provider.yaml up -d

### Consumer

docker compose -f consumer/resources/docker-compose-consumer.yaml up -d

---

## Initialize Postgres (Provider side)

Postgres is preloaded with a small sample of the Online Retail II dataset at startup via docker-entrypoint init scripts. No manual step is required.

- DB URL: postgresql://pocuser:pocpass@127.0.0.1:5432/pocdb
- Sample table: online_retail_ii (invoice_no, stock_code, description, quantity, invoice_date, price, customer_id, country)

If you want to verify manually once the container is up:

psql postgresql://pocuser:pocpass@127.0.0.1:5432/pocdb -c "SELECT * FROM online_retail_ii LIMIT 5;"

The Provider exposes an HTTP endpoint at http://<provider-host>:19191/api/sql that accepts a q query parameter with a read-only SELECT statement and returns JSON rows.

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

At startup, the Provider automatically creates:
- Asset: id=1
- PolicyDefinition: id=1 with USE
- ContractDefinition: id=1 selecting asset 1

The asset now points to an HttpData source that calls the Provider's SQL endpoint with a demo query (select now() as now).

## Create an asset for any SELECT query
To let the consumer run an arbitrary SELECT over the Online Retail II table, first create a query-specific asset on the provider:

curl -X POST "http://<provider-host>:19191/api/sql/asset" \
  -H "Content-Type: application/json" \
  -d '{
        "query": "select country, count(*) as cnt from online_retail_ii group by country order by cnt desc limit 5"
      }'

The response contains assetId and contractDefinitionId. Then proceed with the usual catalog fetch, contract negotiation, and transfer using that contract.

---

# Consumer Steps

## 1. Setup Minio Bucket
- Login to Minio at http://localhost:9001
- Use credentials:
  - Username: consumer
  - Password: password
- Create a bucket named: src-bucket

---

## 2. Consumer API Calls

### Fetch Catalog
curl -X POST "http://100.78.21.5:29193/management/v3/catalog/request" -H "X-Api-Key: password" -H "Content-Type: application/json" -d @consumer/resources/fetch-catalog.json -s | jq

### Negotiate Contract
curl -d @consumer/resources/negotiate-contract.json -H "X-Api-Key: password" -H "Content-Type: application/json" -X POST http://100.78.21.5:29193/management/v3/contractnegotiations -s | jq

### Get Contract ID
curl -X GET "http://100.78.21.5:29193/management/v3/contractnegotiations/<contract-id>" -H "X-Api-Key: password" -H "Content-Type: application/json" -s | jq

### Start Transfer (S3 PUSH)
The transfer uses the Provider's SQL-backed HttpData asset as the source and pushes the result to your Minio bucket based on consumer/resources/start-transfer.json.

curl -X POST "http://100.78.21.5:29193/management/v3/transferprocesses" -H "X-Api-Key: password" -H "Content-Type: application/json" -d @consumer/resources/start-transfer.json -s | jq

Resulting object content will be a JSON array of rows.

---

## Clean Up

### Stop Provider Containers

docker compose -f provider/resources/docker-compose-provider.yaml down

### Stop Consumer Containers

docker compose -f consumer/resources/docker-compose-consumer.yaml down

---

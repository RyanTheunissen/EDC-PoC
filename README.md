# EDC Proof of Concept (EDC-PoC)

This project demonstrates a simple setup of **Provider** and **Consumer** connectors.

---

## Starting Sandbox

To run the whole setup and build with Docker:

```bash
docker-compose up --build
```
---

## Build Project

Generate the Gradle wrapper.

```bash
./gradlew wrapper
```


Build JARs:

```bash
./gradlew build
```

Or build only shadow JARs:

```bash
./gradlew :consumer:shadowJar
./gradlew :provider:shadowJar
```
---

## Boot Connectors


### Consumer Connector
```bash
java -Dedc.fs.config=consumer/config.properties -jar consumer/build/libs/consumer-all.jar
```
---

## 2. Consumer API Calls

### Fetch Catalog

```bash
curl -X POST "http://localhost:29193/management/v3/catalog/request" \
  -H "X-Api-Key: password" \
  -H "Content-Type: application/json" \
  -d @consumer/resources/fetch-catalog.json | jq
```

### Negotiate Contract

```bash
curl -X POST "http://localhost:29193/management/v3/contractnegotiations" \
  -H "X-Api-Key: password" \
  -H "Content-Type: application/json" \
  -d @consumer/resources/negotiate-contract.json | jq
```
### Get Contract ID
```bash
curl -X GET "http://localhost:29193/management/v3/contractnegotiations/<contract-id>" \
  -H "X-Api-Key: password" \
  -H "Content-Type: application/json" \
  -s | jq
```
### Start Transfer
```bash
curl -X POST "http://localhost:29193/management/v3/transferprocesses" \
  -H "X-Api-Key: password" \
  -H "Content-Type: application/json" \
  -d @consumer/resources/start-transfer.json | jq
```
### Transfer Status
```bash
curl -X GET "http://localhost:29193/management/v3/transferprocesses/<transfer-id>" \
  -H "X-Api-Key: password" \
  -H "Content-Type: application/json" \
  -s | jq
```

---

## Clean Up

```bash
docker compose down -v --remove-orphans
```

---

## Provider API Calls

### Create asset

```bash
curl -X POST "http://<provider-ip>:19193/management/v3/assets" \
  -H "X-Api-Key: password" \
  -H "Content-Type: application/json" \
  -d @provider/resources/create-asset.json \
  -s | jq
```

### Create policy definition

```bash
curl -X POST "http://<provider-ip>:19193/management/v3/policydefinitions" \
  -H "X-Api-Key: password" \
  -H "Content-Type: application/json" \
  -d @provider/resources/create-policy.json \
  -s | jq
```

### Create contract definition

```bash
curl -X POST "http://<provider-ip>:19193/management/v3/contractdefinitions" \
  -H "X-Api-Key: password" \
  -H "Content-Type: application/json" \
  -d @provider/resources/contract-definition.json \
  -s | jq
```

### Remove asset from catalog

```bash
curl -X DELETE "http://<provider-ip>:19193/management/v3/assets/1" \
  -H "X-Api-Key: password" | jq
```
### Get contract definition for a specific contract definition id

```bash
curl -X GET "http://<provider-ip>:19193/management/v3/contractdefinitions/12" \
  -H "X-Api-Key: password" | jq
```

### Get assets from provider

```bash
curl -i -X POST "http://<provider-ip>:19193/management/v3/assets/request" \
  -H "X-Api-Key: password" -H "Content-Type: application/json" \
  -d @provider/resources/get-assets.json | jq
```

### Update specific assets (description etc)

```bash
curl -i -X PUT "http://<provider-ip>:19193/management/v3/assets" \
  -H "X-Api-Key: password" \
  -H "Content-Type: application/json" \
  -d @provider/resources/update-asset.json | jq
```

### Get specific asset

```bash
curl -X GET "http://<provider-ip>:19193/management/v3/assets/12" \
-H "X-Api-Key: password" | jq
```

## Recommended Reading Order

1. [Prerequisites Provider](prerequisites-provider.md) **TODO**
2. [Prerequisites Consumer](prerequisites-consumer.md)
3. [Tunneling for Consumer](consumer_ports.md)
4. [This README](README.md)

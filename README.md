# EDC Proof of Concept (EDC-PoC)

This project demonstrates a simple setup of **Provider** and **Consumer** connectors.

## Recommended Reading Order

1. [Prerequisites Provider](prerequisites-provider.md)
2. [Prerequisites Consumer](prerequisites-consumer.md)
3. [Tunneling for Consumer](consumer_ports.md)
4. [Azure VM Setup](Azure-setup.md)
5. [This README](README.md)

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

To fetch the catalog on the provider you need to use the provider IP address in the request body.

replace `<provider-ip>` with the IP address of the provider in the [fetch-catalog](consumer/resources/fetch-catalog.json) file.

```json
{
  "@context": {
    "@vocab": "https://w3id.org/edc/v0.0.1/ns/"
  },
  "counterPartyAddress": "http://<provider-ip>:19194/protocol",
  "protocol": "dataspace-protocol-http"
}
```

After that send the request:

```bash
curl -X POST "http://localhost:29193/management/v3/catalog/request" \
  -H "X-Api-Key: password" \
  -H "Content-Type: application/json" \
  -d @consumer/resources/fetch-catalog.json | jq
```

You will get a catalog response with the assets from the provider.

### Negotiate Contract

The response will look something like this:

```json
{
  "@id": "3c753a86-ec51-4edf-a6d8-698b16efc87c",
  "@type": "dcat:Catalog",
  "dcat:dataset": {
    "@id": "1",
    "@type": "dcat:Dataset",
    "odrl:hasPolicy": {
      "@id": "MQ==:MQ==:YTc4M2ZhN2ItOGZhYS00Yzk2LWJjMzEtNGI3YTlmMmE4ZGE3",
      "@type": "odrl:Offer",
      "odrl:permission": [],
      "odrl:prohibition": [],
      "odrl:obligation": []
    }
  }
}
```

You will need the `@id` of the asset to start the contract negotiation. In this example it is `MQ==:MQ==:YTc4M2ZhN2ItOGZhYS00Yzk2LWJjMzEtNGI3YTlmMmE4ZGE3`

Replace `<asset-id>` with the `@id` of the asset in the [negotiate-contract](consumer/resources/negotiate-contract.json) file.

After that send the following POST request:

```bash
curl -X POST "http://localhost:29193/management/v3/contractnegotiations" \
  -H "X-Api-Key: password" \
  -H "Content-Type: application/json" \
  -d @consumer/resources/negotiate-contract.json | jq
```

You will get a response containing the contract ID.

### Get Contract Agreement ID

With the received contract ID you can check on the status of the contract negotiation. 
If this is successful, you will get a contract agreement ID in the response of this GET request:

```bash
curl -X GET "http://localhost:29193/management/v3/contractnegotiations/<contract-id>" \
  -H "X-Api-Key: password" \
  -H "Content-Type: application/json" \
  -s | jq
```
### Start Transfer

When the contract negotiation is successful, you can start the transfer process.
Replace `<contract-agreement-id>` with the contract agreement ID and the <provider-ip> with the provider IP in the [start-transfer](consumer/resources/start-transfer.json) file.

Make sure to also set the header `X-Filename` to the filename and extension you want it to have in the data destination.

Example:

```json
{
  "@context": {
    "@vocab": "https://w3id.org/edc/v0.0.1/ns/"
  },
  "@type": "TransferRequestDto",
  "connectorId": "provider",
  "counterPartyAddress": "http://<provider-ip>:19194/protocol",
  "contractId": "<contract-agreement-id>",
  "protocol": "dataspace-protocol-http",
  "transferType": "HttpData-PUSH",
  "dataDestination": {
    "type": "HttpData",
    "baseUrl": "https://edc-receiver.hsleiden.com/upload",
    "header:X-Filename": "test.txt"
  }
}
```

Also make sure the data destination is reachable from the consumer. In this example it is the tunneled port on the consumer.
If you are port forwarding, this should be port 7070/upload on your local machine for the receiver container that sends the data to the minio container.

```bash
curl -X POST "http://localhost:29193/management/v3/transferprocesses" \
  -H "X-Api-Key: password" \
  -H "Content-Type: application/json" \
  -d @consumer/resources/start-transfer.json | jq
```
The response will contain the transfer ID.

### Transfer Status

When checking the status of the transfer, you need the transfer ID from the previous step. 
Replace `<transfer-id>` with the transfer ID in the following GET request:

```bash
curl -X GET "http://localhost:29193/management/v3/transferprocesses/<transfer-id>" \
  -H "X-Api-Key: password" \
  -H "Content-Type: application/json" \
  -s | jq
```

---

## Clean Up

To stop the sandbox and remove all containers and volumes:

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

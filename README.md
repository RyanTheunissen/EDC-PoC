# EDC Proof of Concept (EDC-PoC)

This project demonstrates a simple setup of **Provider** and **Consumer** connectors.

---

## Starting Sandbox

To just setup everything in docker containers and seed the assets etc. use:


```bash
docker-compose up --build
```
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

## Upload File to Azure Blob Storage

1. Set the connection string:  
   conn_str="DefaultEndpointsProtocol=http;AccountName=provider;AccountKey=password;BlobEndpoint=http://localhost:10000/provider;"

2. Create a container:  
   az storage container create --name src-container --connection-string $conn_str

3. Upload a test file:  
   az storage blob upload -f ./provider/resources/test-document.txt --container-name src-container --name test-document.txt --connection-string $conn_str

4. Verify the upload:  
   az storage blob list --container-name src-container --connection-string "$conn_str" --query "[].{name:name}" --output table

---

## Configure Vault

[//]: # (export VAULT_ADDR='http://0.0.0.0:8200' )
[//]: # (below is for running in docker container)
export VAULT_ADDR='http://127.0.0.1:8200'
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
curl -X POST "http://localhost:29193/management/v3/catalog/request" -H "X-Api-Key: password" -H "Content-Type: application/json" -d @consumer/resources/fetch-catalog.json -s | jq

### Negotiate Contract
curl -d @consumer/resources/negotiate-contract.json -H "X-Api-Key: password" -H "Content-Type: application/json" -X POST http://localhost:29193/management/v3/contractnegotiations -s | jq

### Get Contract ID
curl -X GET "http://localhost:29193/management/v3/contractnegotiations/63f79bdb-1779-4f2b-aaee-9a1edd5892ca" -H "X-Api-Key: password" -H "Content-Type: application/json" -s | jq

### Start Transfer
curl -X POST "http://localhost:29193/management/v3/transferprocesses" -H "X-Api-Key: password" -H "Content-Type: application/json" -d @consumer/resources/start-transfer.json -s | jq

### Transfer Status
curl -X GET "http://localhost:29193/management/v3/transferprocesses/e0009a57-a42e-478d-b9c4-6ab42bc51754" -H "X-Api-Key: password" -H "Content-Type: application/json" -s | jq


---

## Clean Up

### Stop Provider Containers
docker compose -f provider/resources/docker-compose-provider.yaml down

### Stop Consumer Containers
docker compose -f consumer/resources/docker-compose-consumer.yaml down

---

### Create asset

curl -X POST "http://100.101.111.95:19193/management/v3/assets" -H "X-Api-Key: password" -H "Content-Type: application/json" -d @provider/resources/create-asset.json -s | jq

### Create policy definition

curl -X POST "http://100.101.111.95:19193/management/v3/policydefinitions" -H "X-Api-Key: password" -H "Content-Type: application/json" -d @provider/resources/create-policy.json -s | jq

### Create contract definition

curl -X POST "http://100.101.111.95:19193/management/v3/contractdefinitions" -H "X-Api-Key: password" -H "Content-Type: application/json" -d @provider/resources/contract-definition.json -s | jq
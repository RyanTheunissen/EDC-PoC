# EDC-PoC

**gradle wrapper to generate the gradle wrapper for the project**

./gradlew :consumer:shadowJar

./gradlew :provider:shadowJar


**Start docker containers on provider PC:**

docker compose -f provider/resources/docker-compose-provider.yaml up -d

**Start docker containers on consumer PC:**

docker compose -f consumer/resources/docker-compose-consumer.yaml up -d


**Upload file to azure blob storage:**

conn_str="DefaultEndpointsProtocol=http;AccountName=provider;AccountKey=password;BlobEndpoint=http://100.93.225.17:10000/provider;"
az storage container create --name src-container --connection-string $conn_str

az storage blob upload -f ./provider/resources/test-document.txt --container-name src-container --name test-document.txt --connection-string $conn_str

**Check upload complete:**

az storage blob list --container-name src-container --connection-string "DefaultEndpointsProtocol=http;AccountName=provider;AccountKey=password;BlobEndpoint=http://100.93.225.17:10000/provider;" --query "[].{name:name}" --output table

**Configure Vault:**

export VAULT\_ADDR='http://0.0.0.0:8200'
export VAULT\_TOKEN='<root-token>'
vault kv put secret/accessKeyId content=consumer
vault kv put secret/secretAccessKey content=password
vault kv put secret/provider-key content=password

**Boot provider connector:**

java -Dedc.fs.config=provider/config.properties -jar provider/build/libs/provider-all.jar

**Boot consumer connector:**

java -Dedc.fs.config=consumer/config.properties -jar consumer/build/libs/consumer-all.jar

**Provider catalog bootstrapping:**

The provider now auto-creates the Asset(id=1), PolicyDefinition(id=1 with USE), and ContractDefinition(id=1 selecting asset 1) at startup via a ServiceExtension. No manual POSTs are required.

# Consumer steps

**Setting up the Minio bucket**

Use username:consumer and password:password to login to the Minio blobstorage from localhost:9001.

Create a bucket named 'src-bucket' for the sake of this example.

**Consumer calls**

Fetch catalog
curl -X POST "http://100.78.21.5:29193/management/v3/catalog/request" \
-H 'X-Api-Key: password' -H 'Content-Type: application/json' \
-d @consumer/resources/fetch-catalog.json -s | jq

Negotiate contract
curl -d @consumer/resources/negotiate-contract.json \
-H 'X-Api-Key: password' X POST -H 'content-type: application/json' http://100.78.21.5:29193/management/v3/contractnegotiations \
-s | jq

get contract id
curl -X GET "http://100.78.21.5:29193/management/v3/contractnegotiations/6a80a549-38e2-43f6-8d78-ac9e9c7bd66b" \
-H 'X-Api-Key: password' --header 'Content-Type: application/json' \
-s | jq

start transfer
curl -X POST "http://100.78.21.5:29193/management/v3/transferprocesses" \
-H 'X-Api-Key: password' -H "Content-Type: application/json" \
-d @consumer/resources/start-transfer.json \
-s | jq

# Clear all docker containers

**provider**

docker compose -f provider/resources/docker-compose-provider.yaml down

**consumer**

docker compose -f consumer/resources/docker-compose-consumer.yaml down
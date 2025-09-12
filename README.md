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

java -Dedc.fs.config=transfer/transfer-05-file-transfer-cloud/cloud-transfer-consumer/config.properties -jar transfer/transfer-05-file-transfer-cloud/cloud-transfer-consumer/build/libs/consumer.jar

**Set provider rules:**

# Asset
curl -s -X POST "http://100.93.225.17:19193/management/v3/assets" \
-H "X-Api-Key: password" -H "Content-Type: application/json" \
-d @provider/resources/asset.json | jq

# Policy
curl -s -X POST "http://100.93.225.17:19193/management/v3/policydefinitions" \
-H "X-Api-Key: password" -H "Content-Type: application/json" \
-d @provider/resources/policy.json | jq

# Contract definition
curl -s -X POST "http://100.93.225.17:19193/management/v3/contractdefinitions" \
-H "X-Api-Key: password" -H "Content-Type: application/json" \
-d @provider/resources/contract-definition.json | jq

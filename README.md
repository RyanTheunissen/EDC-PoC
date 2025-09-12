# EDC-PoC

gradle wrapper to generate the gradle wrapper for the project

generate secrets for the vault:
export VAULT_ADDR=localhost:8200
export VAULT_TOKEN=<token>

vault kv put secret/provider/accessKeyId     content=consumer
vault kv put secret/provider/secretAccessKey content=password
vault kv put secret/provider/provider-key    content=password

docker compose -f provider/resources/docker-compose-provider.yaml up -d
docker compose -f consumer/resources/docker-compose-consumer.yaml up -d

./gradlew :consumer:shadowJar
./gradlew :provider:shadowJar

java -Dedc.fs.config=provider/config.properties -jar provider/build/libs/provider-all.jar
java -Dedc.fs.config=consumer/config.properties -jar consumer/build/libs/consumer-all.jar

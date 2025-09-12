# EDC-PoC

gradle wrapper to generate the gradle wrapper for the project

generate secrets for the vault:
export VAULT_ADDR=localhost:8200
export VAULT_TOKEN=<token>

vault kv put secret/provider/accessKeyId     content=consumer
vault kv put secret/provider/secretAccessKey content=password
vault kv put secret/provider/provider-key    content=password

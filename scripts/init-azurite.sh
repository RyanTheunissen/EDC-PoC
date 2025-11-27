#!/usr/bin/env sh
set -e

conn_str="DefaultEndpointsProtocol=http;AccountName=provider;AccountKey=password;BlobEndpoint=http://azurite:10000/provider;"

echo "Waiting for Azurite..."
until az storage container list --connection-string "$conn_str" --auth-mode key >/dev/null 2>&1; do
  sleep 1
done

echo "Creating container src-container..."
az storage container create \
  --name src-container \
  --connection-string "$conn_str" \
  --auth-mode key

echo "Uploading blob test-document.txt..."
az storage blob upload \
  -f /resources/test-document.txt \
  --container-name src-container \
  --name test-document.txt \
  --connection-string "$conn_str" \
  --auth-mode key \
  --overwrite true

echo "Listing blobs in src-container..."
az storage blob list \
  --container-name src-container \
  --connection-string "$conn_str" \
  --auth-mode key \
  --query "[].{name:name}" \
  --output table

echo "Azurite init done."

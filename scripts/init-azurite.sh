#!/usr/bin/env sh
set -eu

ACCOUNT_NAME="provider"
ACCOUNT_KEY="password"
BLOB_ENDPOINT="http://azurite:10000/provider"

CONTAINER="src-container"
BLOB_NAME="test-document.txt"
FILE_PATH="/resources/test-document.txt"

echo "Waiting for Azurite TCP port..."
# Pure TCP readiness (no Azurite API calls that can 400)
for i in $(seq 1 120); do
  if (echo > /dev/tcp/azurite/10000) >/dev/null 2>&1; then
    echo "Azurite port is open."
    break
  fi
  sleep 1
  if [ "$i" -eq 120 ]; then
    echo "ERROR: Azurite port 10000 not open after 120s"
    exit 1
  fi
done

echo "Creating container ${CONTAINER} (ignore if exists)..."
az storage container create \
  --name "${CONTAINER}" \
  --account-name "${ACCOUNT_NAME}" \
  --account-key "${ACCOUNT_KEY}" \
  --blob-endpoint "${BLOB_ENDPOINT}" \
  --auth-mode key >/dev/null

echo "Uploading blob ${BLOB_NAME}..."
az storage blob upload \
  --file "${FILE_PATH}" \
  --container-name "${CONTAINER}" \
  --name "${BLOB_NAME}" \
  --account-name "${ACCOUNT_NAME}" \
  --account-key "${ACCOUNT_KEY}" \
  --blob-endpoint "${BLOB_ENDPOINT}" \
  --auth-mode key \
  --overwrite true >/dev/null

echo "Listing blobs in ${CONTAINER}..."
az storage blob list \
  --container-name "${CONTAINER}" \
  --account-name "${ACCOUNT_NAME}" \
  --account-key "${ACCOUNT_KEY}" \
  --blob-endpoint "${BLOB_ENDPOINT}" \
  --auth-mode key \
  --query "[].{name:name}" \
  --output table

echo "Azurite init done."

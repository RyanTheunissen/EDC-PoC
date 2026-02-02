# EDC Guide
This guide provides an overview of the EDC components and their interactions.
It also explains the build files and configurations required to run a connector.


---

## 1. Roles in a Dataspace

### Provider
The provider is the participant that offers data within the dataspace.  
This connector:
- owns the underlying data sources
- publishes assets, policies, and contract definitions
- exposes a catalog that consumers can query

### Consumer
The consumer requests data from the dataspace by:
- querying catalogs
- starting contract negotiations
- initiating transfer requests

---

## 2. Assets

An asset is a metadata object that describes a dataset.  
The asset does not contain the data itself, but references a data source via a `dataAddress`.

### Example asset (Postgres)

```json
{
  "@context": {
    "@edc": "https://w3id.org/edc/v0.0.1/ns/"
  },
  "@id": "2",
  "properties": {
    "name": "example.txt",
    "description": "This is the description of the example data.",
    "type": "text"
  },
  "dataAddress": {
    "@type": "DataAddress",
    "type": "HttpData",
    "baseUrl": "http://file-server/example.txt"
  }
}
```

**Key elements**
- `@id`: unique identifier (preferably a UUID, but for simplicity a number is used here)
- `properties`: descriptive metadata (name, description, type)
- `dataAddress`: technical reference to the data source  


---

## 3. Policies (ODRL)

EDC policies are based on ODRL and define under which conditions data may be used.

There are three rule types:
- **permission** – *may*
- **duty** – *must*
- **prohibition** – *must not*

### Example policy

```json
{
  "@context": {
    "@vocab": "https://w3id.org/edc/v0.0.1/ns/",
    "odrl": "http://www.w3.org/ns/odrl/2/"
  },
  "@id": "2",
  "policy": {
    "@context": "http://www.w3.org/ns/odrl.jsonld",
    "@type": "Set",
    "duty": [
      {
        "target": "2",
        "action": "use",
        "constraint": [
          {
            "leftOperand": "purpose",
            "operator": "eq",
            "rightOperand": "example-purpose"
          }
        ]
      }
    ]
  }
}
```

In this example, the asset may only be used if the specified purpose matches.

---

## 4. Contract Definitions

A contract definition links:
- one or more assets
- an access policy
- a contract policy

It determines which contract offers are visible in the catalog.

```json
{
  "@context": {
    "@vocab": "https://w3id.org/edc/v0.0.1/ns/"
  },
  "@id": "2",
  "accessPolicyId": "2",
  "contractPolicyId": "2",
  "assetsSelector": [
    {
      "operandLeft": "https://w3id.org/edc/v0.0.1/ns/id",
      "operator": "=",
      "operandRight": "2"
    }
  ]
}
```

---

## 5. Build & Gradle Structure

EDC connectors are Gradle projects.

| File                  | Purpose              |
|-----------------------|----------------------|
| `build.gradle.kts`    | Build configuration  |
| `settings.gradle.kts` | Project index        |
| `./gradlew build`     | building the project |

Each connector:
- uses the `application` plugin
- starts via `BaseRuntime`
- runs on Java 17

---

## 6. Base EDC Dependencies

### Control Plane
```kotlin
implementation("org.eclipse.edc:control-plane-core:$edc")
implementation("org.eclipse.edc:control-plane-api:$edc")
implementation("org.eclipse.edc:control-plane-api-client:$edc")
```

### Management API
```kotlin
implementation("org.eclipse.edc:management-api:$edc")
```

### Dataspace Protocol & HTTP
```kotlin
implementation("org.eclipse.edc:dsp:$edc")
implementation("org.eclipse.edc:http:$edc")
```

### Other required components
```kotlin
implementation("org.eclipse.edc:configuration-filesystem:$edc")
implementation("org.eclipse.edc:iam-mock:$edc")
implementation("org.eclipse.edc:edr-store-core:$edc")
implementation("org.eclipse.edc:transfer-data-plane-signaling:$edc")
implementation("org.eclipse.edc:validator-data-address-http-data:$edc")
```

---

## 7. Provider Configuration

```properties
edc.participant.id=provider1
edc.hostname=provider
edc.dsp.callback.address=http://<ip>:19194/protocol

web.http.port=19191
web.http.path=/api

web.http.management.port=19193
web.http.management.path=/management
web.http.management.auth.key=password

web.http.protocol.port=19194
web.http.protocol.path=/protocol
```

---

## 8. Vault Configuration

```properties
edc.vault.hashicorp.url=http://vault:8200
edc.vault.hashicorp.token=<root-token>
edc.vault.hashicorp.api.secret.path=/v1/secret
edc.vault.hashicorp.secret.key=content
```

Vault secrets have to follow this structure:

```json
{
  "data": {
    "content": "password"
  }
}
```

---

## 9. Consumer Configuration (MinIO / S3)

```properties
edc.aws.access.key=consumer
edc.aws.secret.access.key=password
edc.aws.endpoint.override=http://minio:9000
edc.aws.region=us-east-1
edc.aws.s3.path-style-enabled=true
```

Provider and consumer must use the same region to avoid transfer issues when using S3 push. 
There are some problems with sessions not closing, so for now the PoC is using Http push instead.

---

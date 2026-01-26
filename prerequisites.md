# Prerequisites – EDC Data Space PoC (VM Setup)

This document lists all prerequisites needed to run the EDC Data Space PoC 
inside a virtual machine (local VM or cloud VM such as Azure).


## 1. VM Requirements

Minimum:
- 2 vCPU
- 4 GB RAM
- 20 GB disk
- Ubuntu Server 22.04 or 24.04 LTS

Recommended:
- 4 vCPU
- 8 GB RAM
- 40 GB disk
- Ubuntu Server 22.04 or 24.04 LTS

Notes:
- PoC works on a Raspberry Pi 3B (1GB) so 4GB is comfortable for most setups
- More CPU/RAM needed if running both provider and consumer on same VM


## 2. Operating System

Supported / tested:
- Ubuntu Server 22.04 LTS
- Ubuntu Server 24.04 LTS

Not recommended:
- Windows Server (possible, but couldn't get it to work in Azure on a VM)


## 3. Network Requirements

Inbound network access to the VM must allow:
- Provider protocol port: 19194
- Provider management port: 19193
- Provider api port: 19191 (if used)
- Provider control port: 19192 (if used)
- Provider public port: 19291 (if used)

If using Azure:
- Configure Network Security Group (NSG) inbound port rules
- Ensure inbound TCP access is allowed for required ports
- Make sure SSH (port 22) is open for connecting to the VM

Important rules:
- No localhost (127.0.0.1) in cross-machine URLs
- All callback URLs must be routable from the other party
- VM must have a stable IP or DNS name


## 4. Required Software

The following software must be installed on the VM.


### Docker Engine

Install:
sudo apt update
sudo apt install -y docker.io
sudo systemctl enable docker
sudo systemctl start docker

Verify:
docker --version


### Docker Compose

Either of the following is required:
- docker compose (plugin)
- docker-compose (legacy)

Check:
docker compose version || docker-compose version

If missing:
sudo apt install -y docker-compose-plugin


### Docker Permissions

Your user must be allowed to run Docker commands without sudo. This solves problems where for example docker compose 
works and sudo docker compose breaks. Do this only for PoC, not for production.

Add your user to the docker group:
sudo usermod -aG docker $USER

IMPORTANT:
Log out and log back in (or reboot) for this to take effect.

Verify:
docker ps


## 5. Required CLI Tools

Recommended tools for running and debugging the PoC:
- curl
- wget
- jq
- netcat (nc)
- git

Install:
sudo apt install -y curl wget jq netcat unzip git


## 6. Java (Only if Building Connectors Locally)

Java is required ONLY if you build the EDC connectors from source.

Required:
- Java 17 (Temurin is used inside the Docker images)

Add Adoptium repository:
```sudo apt update
sudo apt install -y wget apt-transport-https gnupg
wget -O - https://packages.adoptium.net/artifactory/api/gpg/key/public \
  | sudo gpg --dearmor -o /usr/share/keyrings/adoptium.gpg
echo "deb [signed-by=/usr/share/keyrings/adoptium.gpg] \
https://packages.adoptium.net/artifactory/deb \
$(lsb_release -cs) main" \
  | sudo tee /etc/apt/sources.list.d/adoptium.list
sudo apt update
```

Install (Only if building without Docker):
```sudo apt install -y temurin-17-jdk```

Verify:
```java -version```

The EDC connectors run on Java 17 inside Docker containers
(eclipse-temurin:17-jre). As a result, the host VM does not require Java
unless the connectors are built from source outside Docker.



## 7. Disk & Filesystem

Ensure:
- Docker has sufficient disk space
- At least 10 GB free after OS install

If using the HTTP file-server:

```sudo mkdir -p /home/<user>/shared```

```sudo chmod -R a+rX /home/<user>/shared```


## 8. Time & Clock Synchronization

The VM clock must be correct.
Contract negotiation and token handling depend on it.

Check:
```timedatectl```

Enable NTP:
```sudo timedatectl set-ntp true```


## 9. Firewall Considerations

If UFW is enabled:
```sudo ufw status```

Example development setup (NOT production):

```sudo ufw allow 19193/tcp```

```sudo ufw allow 19194/tcp```

```sudo ufw allow 8080/tcp```

```sudo ufw allow 8200/tcp```


## 10. Cloud VM Specific Notes (Azure / VPS)

- VM must have a public IP or DNS name
- Disable auto-shutdown and sleep
- Ensure outbound internet access (Docker image pulls)
- Open inbound ports explicitly (cloud firewall + OS firewall)

Common pitfall:
- VM can reach laptop, but laptop cannot reach VM due to missing inbound rules


## 11. Sanity Check Before Starting the PoC

Run the following commands:

```docker ps```
```docker compose version```
```curl --version```
```jq --version```
```ip a```

If all commands work, the VM is ready for the EDC Data Space PoC.


## 12. VM Size Note – Azure Standard B1s (Provider-only Setup)

The Data Space PoC provider stack is currently running on the following Azure VM:

- Size: Standard B1s
- vCPUs: 1
- RAM: 1 GiB

This VM size is sufficient for a test provider-only setup, consisting of:
- EDC Provider Connector
- HashiCorp Vault (dev mode)
- Nginx file-server (static HTTP)


### Why this works on B1s

- Only one JVM-based service (provider)
- File-server (nginx) has negligible resource usage
- No consumer connector running on the same VM
- No MinIO / S3 / data-plane destination running locally

In this setup, memory and CPU usage remain just within limits.


### Important limitations on B1s

A Standard B1s VM leaves very little headroom.

The following will LIKELY cause instability if added to the same VM:
- Consumer connector
- MinIO or other S3-compatible storage
- Multiple JVM-based services like consumer connectors
- Heavy transfer loads
- Parallel negotiations or transfers (i.e.: testing the setup with multiple clients such as tested in KEDASH)

Typical failure modes when limits are exceeded:
- Slow startup
- Contract negotiation timing out
- Transfers failing intermittently
- Containers being killed by the kernel OOM killer (i.e.: Docker containers terminated because out-of-memory)
- Non-deterministic behaviour after restarts


### Recommended usage pattern for B1s

B1s is suitable ONLY when:
- The VM hosts the provider connector
- Vault is used only for development
- Source data is served over simple HTTP
- The VM is used for demos or hands-on labs, not load testing
- Consumer and destination storage run elsewhere

#### Examples of "consumer elsewhere":
- Consumer connector running on a local laptop
- Consumer running in a second VM



### When to upgrade the VM size

Upgrade to a larger VM if you:
- Run both provider and consumer on the same VM
- Add MinIO or other storage locally
- Use multiple data planes
- Experience unexplained timeouts or hangs
- Want predictable behavior under load

Recommended upgraded VMs:
- Standard B2s (2 vCPU, 4 GiB RAM) – minimum comfortable
- Standard B4ms (4 vCPU, 8 GiB RAM) – smooth experience


### Summary

- Standard B1s is acceptable for a provider-only PoC
- It is NOT suitable for a full end-to-end Data Space stack
- Stability depends on keeping the setup minimal
- Problems on B1s are often resource-related, not configuration-related

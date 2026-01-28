# Prerequisites – EDC Data Space PoC (Consumer – Local Workstation)

This document lists all prerequisites needed to run the EDC Consumer
locally on a laptop or workstation (Linux, macOS, or Windows).

The consumer is assumed to:
- run outside the provider VM
- not have a public IP address
- be reachable via Cloudflare Tunnel instead of port forwarding

---


## 1. Operating System

Supported / tested:
- Linux (Fedora 43, Ubuntu 22.04 / 24.04)
- macOS (Apple Silicon)
- Windows 11 (via Docker Desktop)

Notes:
- Linux is recommended. This has been tested with clouflared.
- Windows is supported via Docker Desktop. This has been tested with a Tailscale VPN.

---

## 2. Network Requirements

### Outbound connectivity (required)

The local machine must allow outbound HTTPS (TCP 443) traffic to:
- Cloudflare (for tunnels)
- Docker registries (image pulls)
- Provider VM endpoints

This is usually already the case.

### Inbound connectivity (NOT required)

No inbound ports need to be opened on the local machine.

Ports such as:
- 29194 (DSP protocol)
- 7070 (file receiver)

are exposed via Cloudflare Tunnel, not directly.

Important:
- Do NOT use `localhost` or `127.0.0.1` in cross-machine URLs
- All callback URLs must be publicly reachable HTTPS URLs
- Cloudflare hostnames are used instead of local IP addresses

---

## 3. Required Software

The following software must be installed locally.

---

### Docker Engine / Docker Desktop

Linux:
```bash
docker --version
```

macOS / Windows:
- Install Docker Desktop
- Ensure Docker is running before starting the PoC

---

### Docker Compose

Either of the following is required:
- `docker compose`
- `docker-compose`

Check:
```bash
docker compose version || docker-compose version
```

---

### Docker Permissions (Linux only)

Your user must be allowed to run Docker without `sudo` for ease of use.

```bash
sudo usermod -aG docker $USER
```

Log out and log back in (or reboot).

Verify:
```bash
docker ps
```

---

## 4. Required CLI Tools

Recommended tools for running and debugging the consumer:

- curl
- jq
- git
- netcat (nc)

Linux (Debian/Ubuntu):
```bash
sudo apt install -y curl jq git netcat
```

Linux (Fedora):
```bash
sudo dnf install -y curl jq git netcat
```

macOS (Homebrew):
```bash
brew install curl jq git
```

---

## 5. Cloudflared (Required)

Cloudflared is required to expose the consumer endpoints publicly.

Requirements:
- Free Cloudflare account
- A domain owned by the user (for stable hostnames)

Cloudflared is used for:
- DSP callback endpoint (29194)
- HttpData receiver endpoint (7070)

No router configuration is required.

See [Cloudflared Setup](consumer_ports.md) for details.

---

## 6. Java (Only if Building Connectors Locally)

Java is required if you build the EDC connectors outside Docker.

Required:
- Java 17 (Temurin)

Notes:
- The Docker images already include `eclipse-temurin:17-jre`
- Java is not required for normal PoC usage where building is handled by Docker

---

## 7. Time & Clock Synchronization

The local system clock must be correct.
Contract negotiation and token handling depend on it.

Linux:
```bash
timedatectl
sudo timedatectl set-ntp true
```

macOS / Windows:
- System time is usually synchronized automatically

---

## 8. Firewall Considerations

Local firewalls must allow:
- outbound HTTPS (TCP 443)
- outbound DNS (UDP/TCP 53)

No inbound firewall rules are required.

---

## 9. Sanity Check Before Starting the Consumer

Run the following commands:

```bash
docker ps
docker compose version
curl --version
jq --version
cloudflared --version
```

If all commands work, the local machine is ready to run the EDC Consumer.

---

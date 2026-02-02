# Prerequisites – EDC Data Space PoC (Provider –  VM)

This document lists all prerequisites needed to run the EDC Provider on a VM in for example Azure.

The consumer is assumed to:
- run outside the provider VM
- not have a public IP address
- be reachable via Cloudflare Tunnel instead of port forwarding

---


## 1. Operating System

Supported / tested:
- Linux (Ubuntu 22.04 / 24.04)
- macOS (Apple Silicon)
- Windows 11 (via Docker Desktop)

Notes:
- Linux is recommended. Easiest to set up and troubleshoot.
- Windows is supported via Docker Desktop. 
  This has been tested with a Tailscale VPN to circumvent callback URLs not being publicly reachable.

---

## 2. Network Requirements

### Inbound connectivity

In the tab `Networking`, `Network settings` of the VM in Azure Portal, ensure the following inbound rules exist:

| Name | Port               | Protocol | Source | Destination |
|------|--------------------|----------|--------|-------------|
| SSH  | 22                 | TCP      | Any    | Allow       |
| EDC  | 19191-19194, 19291 | TCP      | Any    | Allow       |


Important:
- Do NOT use `localhost` or `127.0.0.1` in cross-machine URLs
- All callback URLs must be publicly reachable


---

## 3. Required Software

The following software must be installed.

---

### Docker Engine

Linux:
```bash
docker --version
```

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

### Docker Permissions

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
- git
- netcat (nc)

Linux (Debian/Ubuntu):
```bash
sudo apt install -y curl git netcat
```

## 5. Time & Clock Synchronization

The local system clock must be correct.
Contract negotiation and token handling depend on it.

Linux:
```bash
timedatectl
sudo timedatectl set-ntp true
```

---

## 6. Sanity Check Before Starting the Consumer

Run the following commands:

```bash
docker ps
docker compose version
curl --version
```

If all commands work, the VM is ready to run the EDC Provider.

---

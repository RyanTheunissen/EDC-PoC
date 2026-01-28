# Ports for callback and file receiver

When running the consumer locally, ports 29194 and 7070 must be reachable from the provider. 
This can be done within your own router by port forwarding or by tunneling services like cloudflared.
Another option is to run the consumer in a vm just like the provider in azure and make sure the vm has a public ip 
and incoming port rules for TCP allow on the ports.

In the test setup the consumer is using cloudflared to create a tunnel for port 29194 and 7070.
To be able to get a static url for the callback endpoint a free account on cloudflare is needed with a domain owned by you.
If this is not the case, you can create tunnels with random urls, but you need to update the consumer configuration with the new callback url each time.

## Using Cloudflare Tunnels instead of port forwarding

With Cloudflare tunnels:
- no inbound ports need to be opened
- no VPN is required

To get stable hostnames for the tunnels, a free Cloudflare account and a domain are required.
If you don't have one, you can use trycloudflare.com to create a temporary tunnel. 
This is not recommended for long-term use.

## Cloudflared setup (Linux Fedora 43 Workstation)

### 1. Install cloudflared

Cloudflared is not available in the default Fedora repositories. 
Add the official Cloudflare repository:

```bash
sudo tee /etc/yum.repos.d/cloudflare-cloudflared.repo > /dev/null <<'EOF'
[cloudflare-cloudflared]
name=cloudflared
baseurl=https://pkg.cloudflare.com/cloudflared/rpm
enabled=1
gpgcheck=1
gpgkey=https://pkg.cloudflare.com/cloudflare-main.gpg
EOF
```

Install cloudflared:

```bash
sudo dnf clean all
sudo dnf install cloudflared
```

Verify installation:

```bash
cloudflared --version
```

---

### 2. Prepare your Cloudflare account and domain

1. Create a free Cloudflare account
2. Add a domain you own to Cloudflare
3. Change the domain’s nameservers at your registrar to the Cloudflare-provided ones
4. Wait until Cloudflare shows the domain as Active

This is required to get stable hostnames for the tunnel.
If this is not possible, quick tunnels using trycloudflare.com can be used, but the URL will change on every restart.

---

### 3. Authenticate cloudflared

Authenticate your local cloudflared installation with your Cloudflare account:

```bash
cloudflared tunnel login
```

This opens a browser window where you authorize cloudflared on your machine.

---

### 4. Create a named tunnel

Create a tunnel for the consumer:

```bash
cloudflared tunnel create edc-consumer
```

This will:
- Create a tunnel ID
- Generate a credentials file in ~/.cloudflared/

Confirm the tunnel exists:

```bash
cloudflared tunnel list
```

Note the tunnel ID.

---

### 5. Create the tunnel configuration

Create the configuration directory if it does not exist:

```bash
mkdir -p ~/.cloudflared
nano ~/.cloudflared/config.yml
```

Example configuration (adjust usernames, tunnel ID, and domain):

```yaml
tunnel: edc-consumer
credentials-file: /home/<your-user>/.cloudflared/<tunnel-id>.json

ingress:
  - hostname: edc-consumer.<your-domain>.com
    service: http://127.0.0.1:29194
  - hostname: edc-receiver.<your-domain>.com
    service: http://127.0.0.1:7070
  - service: http_status:404
```
Note:
- Use single-level subdomains for the tunnels. the free tier does not cover something like consumer.edc.<your-domain>.com
- Use `127.0.0.1` instead of localhost to avoid IPv6 issues.

---

### 6. Create DNS routing for the tunnel

Create a DNS record pointing your hostname to the tunnel:

```bash
cloudflared tunnel route dns edc-consumer edc-consumer.<your-domain>.com
cloudflared tunnel route dns edc-consumer edc-receiver.<your-domain>.com
```


Verify in the Cloudflare dashboard that the DNS record exists.

---

### 7. Start the tunnel

Run the tunnel:

```bash
cloudflared tunnel run edc-consumer
```

Leave this process running, or later convert it into a systemd service for persistence.

---

## Optional: run the tunnel automatically (systemd)

For more stable PoCs, the tunnel can be run as a **system service**
that starts automatically on boot.

This is optional and only needed after the tunnel works correctly
when run manually.

### Move configuration to system location

```bash
sudo mkdir -p /etc/cloudflared
sudo cp ~/.cloudflared/config.yml /etc/cloudflared/config.yml
sudo cp ~/.cloudflared/<tunnel-id>.json /etc/cloudflared/creds.json
sudo chmod 600 /etc/cloudflared/creds.json
```

Update `/etc/cloudflared/config.yml`:

```yaml
tunnel: edc-consumer
credentials-file: /etc/cloudflared/creds.json
```

### Install and start the service

```bash
sudo cloudflared service install
sudo systemctl enable --now cloudflared
systemctl status cloudflared
```

Logs:

```bash
journalctl -u cloudflared -f
```

## EDC Consumer configuration changes

Update the consumer config.properties file:

```properties
edc.dsp.callback.address=https://edc-consumer.<your-domain>.com/protocol

web.http.protocol.port=29194
web.http.protocol.path=/protocol
```

Restart the consumer containers:

```bash
docker compose down
docker compose up -d
```

---

## TransferRequest configuration (HttpData-PUSH)

When starting a transfer, the data destination must also be reachable by the provider:

```json
"dataDestination": {
  "type": "HttpData",
  "baseUrl": "https://edc-receiver.<your-domain>.com/upload"
}
```

---

## Verification checks

From any machine (including the Azure provider VM):

```bash
curl -I https://edc-consumer.<your-domain>.com/protocol
curl -I https://edc-receiver.<your-domain>.com/upload
```

Expected responses:
- `404` or `405` means the endpoint is reachable
- `502` → tunnel is running but local service is down
- A timeout indicates the tunnel is not running or misconfigured

---


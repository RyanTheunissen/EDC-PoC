# Ports for callback and file receiver

When running the consumer locally ports 29194 and 7070 must be reachable from the provider. 
This can be done within your own router by port forwarding or by tunneling services like cloudflared.
Another option is to run the consumer in a vm just like the provider in azure and make sure the vm has a public ip 
and incoming port rules for TCP allow on the needed ports.

In the test setup the consumer is using cloudflared to create a tunnel for port 29194 and 7070.
To be able to do get a static url for the callback endpoint a free account on cloudflare is needed with a domain owned by you.
If this is not the case you can create tunnels with random urls but you need to update the consumer configuration with the new callback url each time.

## Cloudflared setup (Linux Fedora 43 Workstation)

Follow the instructions from the cloudflared documentation to install the tool.
Make sure to add your domain to cloudflare and create a free account.
Next change your domain name server to the cloudflare ones. 
Then run the login command to authenticate your cloudflared tool with your cloudflare account:

    cloudflared tunnel login
After login create a tunnel:

    cloudflared tunnel create consumer-tunnel
This will create a credentials file in ~/.cloudflared/ folder.

Confirm that the tunnel was created:

    cloudflared tunnel list

Next create a config file ~/.cloudflared/config.yml with the following content (change the domain name to your own):
    tunnel: <tunnel-id-from-previous-step>
    credentials-file: /home/<your-user>/.cloudflared/<tunnel-id-from-previous-step>.json

    ingress:
      - hostname: consumer-tunnel.<your-domain>.com
        service: http://localhost:29194
      - hostname: file-receiver.<your-domain>.com
        service: http://localhost:7070
      - service: http_status:404

Finally run the tunnel:

    cloudflared tunnel run consumer-tunnel
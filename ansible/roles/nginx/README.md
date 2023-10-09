# Nginx

## About

This role install ansible and configure every website.

Nginx starts with root but the thread are started with nginx.

The certificates are generated with the [cerbot](../certbot/README.md) role.

## How to add a website

### Create a certificate
Create a certificate by adding it to the [certbot role](../certbot/README.md)

### Configuration template

#### Unique to the domain
 If the configuration should be unique to the domain, create a template for the website at the [nginx configuration directory](../../host_files/beau/nginx/http/fqdn-based)

#### Dokuwiki farmer bytle animal
If the configuration is for a dokuwiki animal domain, add it:
  * to the farm: `https:\\farmer.bytle.net`
  * and into the `nginx_dokuwiki_domains` variables

### Then run this playbook

Run this playbook
```bash
ansible-playbook playbook-root.yml -i inventories/beau.yml --vault-id passphrase.sh --tags nginx
```

The code is in the [nginx_template_conf file](./tasks/nginx_template_conf.yml)

## Quick conf

  * The template (server blocks) should be added to `/etc/nginx/conf.d`
  * Log `/var/log/nginx/`


### Agent control

```bash
systemctl start|stop|restart amplify-agent
# started
ps ax | grep -i 'amplify\-'
```

### Client Authentication

For internal tools, we enforce client authentication.
The CA certificate is [root_certificate](./files/root_certificate.pem)
and is valid for 5 years (until 10 Mar 2026)

See it on the server:

```bash
openssl x509 -in /opt/nginx/client_certificate/root_certificate.pem -noout -text
```
The client certificate are available for one year.







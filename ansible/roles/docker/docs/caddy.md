# Caddy

## Local HTTP

To develop locally the docker configuration, the TLS should be local.

`tls: internal` directive
then [CA can be imported](https://caddyserver.com/docs/automatic-https#local-https)

The CA is at `caddy_data/pki/authorities/local`

* With Docker, you need to add the `root.cer` and `intermediate.cer` as Trusted Root Certificate in Windows.
* With the caddy binary, you can use `caddy trust` to install the cert.


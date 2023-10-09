# Client Authentication


## Protection of support website

To protect support website such as netdata, we use client
authentication.

The root ca is at : `/etc/nginx/client_certs/root_certificate.pem`
The ca information is at [ca.ini](ca.ini)

## How to create a client

From [doc](https://datacadamia.com/web/server/nginx/client_authentication#creation_and_installation_of_a_client_certificate)

* Change the name in [client.ini](client.ini)
* Create the CSR

```bash
openssl req \
   -new `# Ask a certificate signing request`\
   -keyout client_private_key.pem `# The private key output (created if the key option is not given)` \
   -nodes `#don't encrypt the created private key` \
   -out client_csr.pem `# The certificate signing request (CSR) file ` \
   -config client.ini `# The client DN information`
```

* Sign it
```bash
openssl \
     x509 `# output a certificate`  \
    -req `#input is a certificate request, sign and output` \
    -days 365 `#How long till expiry of a signed certificate - def 30 days` \
    -in client_csr.pem \
    -out client_certificate.pem \
    -CA root_certificate.pem \
    -CAkey root_private_key.pem \
    -set_serial 0x"$(openssl rand -hex 16)"  `# large random unique identifier for the certificate. ` \
    -extensions client_extensions \
    -extfile client.ini
```

* Create a p12
```bash
openssl pkcs12 \
    -export `# Create the p12 file ` \
    -out client_certificate.p12 `# file name created ` \
    -inkey client_private_key.pem `# File to read private key from ` \
    -in client_certificate.pem \
    -certfile root_certificate.pem `#A filename to read additional certificates from `
```

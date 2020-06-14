# Certbot

## About

This role will install certbot of let's encrypt in order to get certificate.

Certificate are used:

  * in website ([nginx](../nginx/README.md))
  * for the server (smtp). See [Postfix](../postfix/README.md)

## Usage

  * Add the domain in the [domains](./defaults/main/domains.yml) file
  * Run this role

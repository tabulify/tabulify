# Https Dev


In dev, https is required for local development
  * for cross-site cookie with `sameSite=none`
  * by oauth provider such as Google

In prod, the nginx proxy takes care of it.


## How to use it?

They are generated with [mkcert](https://github.com/FiloSottile/mkcert)
* Install it (Windows)
```shello
winget install -e --id FiloSottile.mkcert
```
* Add the environment variable `CAROOT` to the [cert directory](../../cert/README.md) path each time
```shell
set CAROOT=C:\code\java-mono\cert
```
* Install the Root CA (DOS)
```shell
mkcert -install
```
* Create a certificate with the domain that you want. Note that we use `dev` as local TLD and not `local` to be compliant with Oauth Google that requires a valid public TLD.
```shell
cd /D D:\code\java-mono\cert
mkcert "*.combostrap.dev" "*.eraldy.dev" datacadamia.dev localhost 127.0.0.1 ::1
```
* Modify the names to [cert.pem](../../cert/cert.pem) and [key.pem](../../cert/key.pem)
* Move them to the cert directory
* The certificates and CA are used in Dev environment taken by:
  * [VerticleHttpServer](../src/main/java/net/bytle/tower/VerticleApi.java)
  * and node apps


In pure java with Bouncy Castle
* <a href="https://medium.com/@lbroudoux/generate-self-signed-certificates-in-pure-java-83d3ad94b75">...</a>
* <a href="https://www.misterpki.com/how-to-generate-a-self-signed-certificate-with-java-and-bouncycastle/">...</a>
* Generate a self-signed key pair and certificate. <a href="https://stackoverflow.com/questions/1615871/creating-an-x509-certificate-in-java-without-bouncycastle">...</a>

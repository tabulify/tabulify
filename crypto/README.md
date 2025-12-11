# Tabulify Crypto

A collection of utilities for:

* creating Digest (ie hash of files or document)
* protecting password and secrets

## List

### Protector

A cipher text of the [protector](src/main/java/com/tabulify/crypto/Protector.java) has the following structure:

* base64 of the bytes of a string (this is to delete the base64 separator)
* when we decode the base64, we get a string with two parts separated by the `>` separator
  * the first part is a byte that contains the id of the cipher transformation
  * the second part is the output of the cipher transformation
* We give the second part to the cipher transformation, and we get back the plain text

As today, we use only a `AES` transformation in
the [symmetric cipher](src/main/java/com/tabulify/crypto/CryptoSymmetricCipher.java)

* It creates as output a string composed of three `base64` strings separated by the `>` separator
* the three parts are the salt, the hmac and the ciphertext

### Others

Check the [tests](src/test/java/com/tabulify/crypto)

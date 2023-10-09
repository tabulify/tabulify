# Member App

## About
The `member` [app](app.md) is a [frontend app](app-frontend.md) that is used for identity management (login, logout and registration)

known also as:
* `customer identity and access management (CIAM)`
* `identity and access management (IAM)`

The member app is:
* An identity management system
* with user and list registration

`List registration` is generally not part of an identity management system,
but we want to be able to register user from a `list subscription`.

## How to start the app
* Start the [backend](vertx-run.md)
* Start the [frontend app](../../../js-mono/apps/member)
```shell
yarn dev
```
* Go to the app
  * http://member.nico.lan:8183 with the toplevel domain configuration
  * http://localhost:8183 without


## Definition / Component

It's defined by:
* the [combo member app](../src/main/openapi/eraldy-member-openapi.yaml)
* the [combo member App Object](../src/main/java/net/bytle/tower/eraldy/app/memberapp/EraldyMemberApp.java)
* the [Frontend Javascript app](../../../js-mono/apps/member)

## Name
We choose `member` and not `auth` or `identity`
to convey a more social aspect.

## Auth

* Oauth:
  * [Oauth doc](https://vertx.io/docs/vertx-web/java/#_oauth2authhandler_handler)
  * [vertx auth/openid](https://vertx.io/docs/vertx-auth-oauth2/java/)
* [One Time Password (Multi-Factor Authentication)](https://vertx.io/docs/vertx-web/java/#_one_time_password_multi_factor_authentication)
* [WebAuthn](https://vertx.io/docs/vertx-web/java/#_webauthn)




## Navigation
### User List registration

See [](memberListRegistration.md)

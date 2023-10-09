# Auth

## About

## Vertx

This is a short version of [Ref](https://vertx.io/docs/vertx-auth-common/java/)

### Authentication

You authenticate in Vertx with the [authenticate method](https://vertx.io/docs/apidocs/io/vertx/ext/auth/authentication/AuthenticationProvider.html#authenticate-io.vertx.ext.auth.authentication.Credentials-io.vertx.core.Handler-) that should return a [User](https://vertx.io/docs/apidocs/io/vertx/ext/auth/User.html) on successful authentication.


The authentication credentials may be:
  * a simple json with login/password
  * a JWT token
  * a OAuth bearer token

### Authorization
The authentication user object has no context or information on which authorizations the object is entitled.

Once you have a User instance you can call [authorizations](https://vertx.io/docs/apidocs/io/vertx/ext/auth/User.html#authorizations--) to get its authorizations.

Ability:
* to set individual permissions at a user level,
* to create roles (sets of permissions) for easier user management.
A user can be assigned multiple roles, and assigned permissions on top of these.

## Auth0

For each [realm (user management)](realm.md), auth0 has a [user id](https://auth0.com/docs/manage-users/user-migration/bulk-user-import-database-schema-and-examples#properties)


## Example of cookie session parameters


* Created:"Wed, 07 Jun 2023 12:24:49 GMT"
* Domain:".postman.co"
* Expires / Max-Age:"Fri, 07 Jul 2023 12:24:49 GMT"
* HostOnly:false
* HttpOnly:true
* Last Accessed:"Fri, 09 Jun 2023 07:38:44 GMT"
* Path:"/"
* SameSite:"Lax"
* Secure:true

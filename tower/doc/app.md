# Tower Apps

## About
The apps that are developed in the tower webserver are defined by:
  * an [openapi specification](openapi.md)
  * an app object that implements the [Tower App Class](../src/main/java/net/bytle/tower/TowerApp.java)


## Model and Type : Api vs Frontend

### Type
There are two kinds of App:
  * an api app or `api` that has a version
  * a frontend app or `app` that does not have any version

#### Frontend
A [frontend app](app-frontend.md) is an app that targets the browser
and produces HTML that is used by the user.

#### API
An API app is an app that:
* is not shown in the browser
* does not have the notion of session

### Top-Level Domain
Every app belongs to a [top-level domain](app-toplevel.md).
ie the app `member.combostrap.com` belongs to the domain `combostrap.com`

### Suffix
* all API app have the `api` suffix (because the word `public` is a reserved Java word, the app name is then `publicapi`)
* all GUI/HTML/Browser app have the `app` suffix

## List

### Combostrap

The `combostrap.com` domain has the following `apps`

#### Private API

`private` is an api. This is a first party API, our private api.

It's defined by:
* the [combo private api openapi](../src/main/openapi/eraldy-combo-private-openapi.yaml) -
* the [Private App Object](../src/main/java/net/bytle/tower/eraldy/app/comboprivateapi/ComboPrivateApiApp.java)

#### Public API

`public` is an API app. This is the third party developer API that requires a token that have the combostrap `realm` and `app` information. (ie the `realm` and `app` in the [model](data_model.md), not the app on this page)

It's defined by:
* the [combo public openapi file](../src/main/openapi/eraldy-combo-public-openapi.yaml)
* the [combo public App Object](../src/main/java/net/bytle/tower/eraldy/app/combopublicapi/ComboPublicApiApp.java)


#### Member App

The [member app](member.md) is a frontend app that is used for identity management (login, logout and registration)

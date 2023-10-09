# Postman


## Realm
The below realm is use for test:

* `scope`: `http://foo.combostrap.com`: the local and production test realm
* `handle`: `test`: the realm hand (ie the [cs_test schema](schema.md))

## Environment
We therefore use an environment with the following variables in Postman:
* `baseUrl`: `http://localhost:8083/combo/api/v1.0`
* `host`: `foo.combostrap.com`
* `handle`: `test`

## Api Collection Environment

The `host` is also set as http header on the API collection scope via the following
pre-request script:
```javascript
pm.request.headers.add({key: 'Host', value: pm.variables.get('host') });
```


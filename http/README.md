# HTTP module (Rest)

## About
This is the HTTP module of the Bytle Framework

It provides a FileSystem NIO.

## Third library

  * [Tail like library](https://github.com/myfreeweb/rxjava-http-tail) - allows you to follow logs over HTTP, like tail -f
  * [HttpCore](https://hc.apache.org/httpcomponents-core-5.1.x/examples.html)

## Authentication Management for users

We should implement an Oauth server to store the client token
in order to retrieve them locally or in a batch run (Vertx offers them luckily).

Why ?
  * To make it easy for user to get access to third rest api. For instance, [Linkedin is pretty difficult](https://learn.microsoft.com/en-us/linkedin/shared/authentication/client-credentials-flow)
and not easy to found.
  * Not all services offer an application token for native application.


## Model

You can make a `get` to execute a rest call
with:
- special attribute to set any headers
- data-def to define the data received

Example:
```bash
curl -X GET \
'https://${dc}.api.mailchimp.com/3.0/lists/{list_id}/members?fields=<SOME_ARRAY_VALUE>&exclude_fields=<SOME_ARRAY_VALUE>&count=10&offset=0&email_type=<SOME_STRING_VALUE>&status=<SOME_STRING_VALUE>&since_timestamp_opt=<SOME_STRING_VALUE>&before_timestamp_opt=<SOME_STRING_VALUE>&since_last_changed=<SOME_STRING_VALUE>&before_last_changed=<SOME_STRING_VALUE>&unique_email_id=<SOME_STRING_VALUE>&vip_only=<SOME_BOOLEAN_VALUE>&interest_category_id=<SOME_STRING_VALUE>&interest_ids=<SOME_STRING_VALUE>&interest_match=<SOME_STRING_VALUE>&sort_field=<SOME_STRING_VALUE>&sort_dir=<SOME_STRING_VALUE>&since_last_campaign=<SOME_BOOLEAN_VALUE>&unsubscribed_since=<SOME_STRING_VALUE>' \
--user "anystring:${apikey}"'
```
would be as:
  * a connection
```ini
[mailchimp]
uri=https://${dc}.api.mailchimp.com/3.0/
header=--user "anystring:${apikey}"'
```
  * a get request
```yaml
- name: "Get"
  operation: "define"
    args:
      data-uri: "lists/{list_id}/members?fields=<SOME_ARRAY_VALUE>&exclude_fields=<SOME_ARRAY_VALUE>&count=10&offset=0&email_type=<SOME_STRING_VALUE>&status=<SOME_STRING_VALUE>&since_timestamp_opt=<SOME_STRING_VALUE>&before_timestamp_opt=<SOME_STRING_VALUE>&since_last_changed=<SOME_STRING_VALUE>&before_last_changed=<SOME_STRING_VALUE>&unique_email_id=<SOME_STRING_VALUE>&vip_only=<SOME_BOOLEAN_VALUE>&interest_category_id=<SOME_STRING_VALUE>&interest_ids=<SOME_STRING_VALUE>&interest_match=<SOME_STRING_VALUE>&sort_field=<SOME_STRING_VALUE>&sort_dir=<SOME_STRING_VALUE>&since_last_campaign=<SOME_BOOLEAN_VALUE>&unsubscribed_since=<SOME_STRING_VALUE>@mailchimp"
      type: "json"
      data-definition:
        request:
          method: get
```
 * a put request
```yaml
- name: "Put"
  operation: "define"
    args:
      data-uri: "lists/{list_id}/members?fields=<SOME_ARRAY_VALUE>&exclude_fields=<SOME_ARRAY_VALUE>&count=10&offset=0&email_type=<SOME_STRING_VALUE>&status=<SOME_STRING_VALUE>&since_timestamp_opt=<SOME_STRING_VALUE>&before_timestamp_opt=<SOME_STRING_VALUE>&since_last_changed=<SOME_STRING_VALUE>&before_last_changed=<SOME_STRING_VALUE>&unique_email_id=<SOME_STRING_VALUE>&vip_only=<SOME_BOOLEAN_VALUE>&interest_category_id=<SOME_STRING_VALUE>&interest_ids=<SOME_STRING_VALUE>&interest_match=<SOME_STRING_VALUE>&sort_field=<SOME_STRING_VALUE>&sort_dir=<SOME_STRING_VALUE>&since_last_campaign=<SOME_BOOLEAN_VALUE>&unsubscribed_since=<SOME_STRING_VALUE>@mailchimp"
      type: "json"
      data-definition:
        request:
          method: put
          body: {"email_address":"","status_if_new":"subscribed","email_type":"","status":"subscribed","merge_fields":{},"interests":{},"language":"","vip":false,"location":{"latitude":0,"longitude":0},"marketing_permissions":[],"ip_signup":"","timestamp_signup":"","ip_opt":"","timestamp_opt":""}
```


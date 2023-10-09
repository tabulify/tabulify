# HTTP System

## In the wild

  * https://docs.pipedream.com/destinations/http/

```javascript
$send.http({
  method, // Required, HTTP method, a string, e.g. POST, GET
  url, // Required, the URL to send the HTTP request to
  data, // HTTP payload
  headers, // An object containing custom headers, e.g. { "Content-Type": "application/json" }
  params, // An object containing query string parameters as key-value pairs
  auth, // An object that contains a username and password property, for HTTP basic auth
});
```

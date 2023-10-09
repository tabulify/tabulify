# Nginx Rate Limiting


## About
The limitation is done using the [leaky bucket method](#leaky-bucket-algorithm).

## Number of request

With a html page with 4–6 resources and never more than 12 resources,
you would en up with 8 request by page


## Ref
* https://www.nginx.com/blog/rate-limiting-nginx/
* http://nginx.org/en/docs/http/ngx_http_limit_req_module.html

## Configuration

Rate limiting is configured with two main directives:
* `limit_req_zone`
* `limit_req`

### limit_req_zone

Example:
```bash
limit_req_zone key zone=name:size rate=rate [sync];
# example
# 10 Mb zone `one` with rate 10r/s
limit_req_zone $binary_remote_addr zone=one:10m rate=10r/s;
# 10 Mb zone `one` with rate 30r/m
limit_req_zone $binary_remote_addr zone=one:10m rate=30r/m;
```
The $binary_remote_addr variable’s size is:
* 4 bytes for IPv4 addresses
* 16 bytes for IPv6 addresses.
The stored state always occupies:
* 64 bytes on 32-bit platforms
* 128 bytes on 64-bit platforms.
One megabyte zone can keep about:
* 16 thousand 64-byte states
* 8 thousand 128-byte states.

### limit_req
```
limit_req zone=one burst=20 nodelay;
```

#### burst parameter
The `burst` value is the maximum number of resource by page.
The browser for each page will make request for each resource that should not be blocked.

Explanation:
Rate limiting is done in an interval of 100ms (ie one request is allowed for every 100ms).
A client cannot make a request for that URL within 100ms of its previous one

With the burst parameters, a request that arrives sooner than 100ms after the previous one is put in a queue.
The burst parameter value is the size of this queue: here 20.

If 21 requests arrive from a given IP address simultaneously, NGINX:
* forwards the first one immediately
* puts the remaining 20 in the queue.
* forwards a queued request every 100ms,
* returns 503 to the client only if an incoming request makes the number of queued requests go over 20.

#### nodelay

In the previous example of 21 request, the 20th packet
in the queue will wait 2 seconds (20*100ms) to be forwarded.

If the `nodelay` is set

Suppose that a 20‑slot queue is empty and 21 requests arrive simultaneously from a given IP address. NGINX:
* forwards all 21 requests immediately
* marks the 20 slots in the queue as taken,
* then frees 1 slot every 100ms.
(If there were 25 requests instead, NGINX would immediately forward 21 of them, mark 20 slots as taken, and reject 4 requests with status 503.)

### delay

```
limit_req zone=ip burst=12 delay=8;
```
The configuration:
* allows bursts of up to 12 requests (ie 12 resources by page)
* the first 8 are processed without delay.

#### Example

```
limit_req zone=one burst=20 nodelay;
```

```nginx
server {
    location /login/ {
        limit_req zone=one burst=5;

        proxy_pass http://my_upstream;
    }
}
```

## Module
The ngx_http_limit_req_module module (0.7.21) is used to limit
the request processing rate per a defined key,
in particular, the processing rate of requests coming from a single IP address.


## Leaky bucket algorithm
NGINX rate limiting uses the leaky bucket algorithm

https://www.nginx.com/blog/rate-limiting-nginx/#How-NGINX-Rate-Limiting-Works

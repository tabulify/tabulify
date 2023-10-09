# Monitoring endpoints


## About

All monitoring endpoints are below the [prometheus configuration](../../../host_files/beau/nginx/http/prometheus.bytle.net.conf) even if it's not always a prometheus product.



Example:

  * The [Nginx stub status](http://nginx.org/en/docs/http/ngx_http_stub_status_module.html)

See also [log files](diagnostic.md)

## Endpoints

### Netdata
[netadata](netadata.md)

### Nginx

* [Stub Status](http://nginx.org/en/docs/http/ngx_http_stub_status_module.html):
  * [https://prometheus.bytle.net/stub_status](https://prometheus.bytle.net/stub_status) - for remote access
  * `http://localhost/stub_status` - for local access by [netdata](https://learn.netdata.cloud/docs/agent/collectors/python.d.plugin/nginx)

### Others
  * [Prometheus Node Exporter](https://prometheus.bytle.net/metrics)
  * [Nginx Status](https://github.com/vozlt/nginx-module-vts)
     * [https://prometheus.bytle.net/nginx-status](https://prometheus.bytle.net/status)
  * [Php Status](https://prometheus.bytle.net/php-status?full)
  * [Nginx Agent for Amplify](https://amplify.nginx.com)






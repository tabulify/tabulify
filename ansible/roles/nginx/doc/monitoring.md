# Monitoring endpoints


## About

All monitoring are below the [prometheus configuration](../templates/prometheus-nginx-site.conf.j2) even if it's not always a prometheus product.


Example:
 
  * The [Nginx stub status](http://nginx.org/en/docs/http/ngx_http_stub_status_module.html)
  

## Endpoints

  * https://prometheus.bytle.net/metrics - Prometheus Node Exporter
  * [Stub Status](http://nginx.org/en/docs/http/ngx_http_stub_status_module.html):
     * [https://prometheus.bytle.net/stub_status](https://prometheus.bytle.net/stub_status) - for remote access
     * `http://localhost/stub_status` - for local access by [netdata](https://learn.netdata.cloud/docs/agent/collectors/python.d.plugin/nginx)
  * [Status](https://github.com/vozlt/nginx-module-vts)
     * [https://prometheus.bytle.net/status](https://prometheus.bytle.net/status)

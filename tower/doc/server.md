# Server

## About

The application is [deployed](deployment.md) on a VM.


## Server configuration

The server is configured with ansible.
See [Apps](../../ansible/roles/apps/README.md)

## Service

```bash
sudo systemctl start tower
sudo systemctl stop tower
```

## Server Debug

* The service log
```bash
journalctl -f -u backend
```
* The analytics error log
```bash
tail -f /opt/tower/logs/analytics-error.log
```
* The web request log
```bash
tail -f /opt/tower/logs/web.log
```

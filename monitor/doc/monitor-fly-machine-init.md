# Fly Init

TODO ? https://fly.io/docs/machines/guides-examples/terraform-machines/

## Before deploying action

* Create the app
```
fly apps create --machines --name eraldy-monitor
```

then minimal File toml to not give the app name and region at the command line
```toml
app = "eraldy-monitor"
primary_region = "ams"
```

Then allocate IP
```bash
# allocate ip
fly ips allocate-v4 --shared
fly ips allocate-v6
```

Then secret
* Secret
```bash
fly secrets import < .monitor.secret.env
fly secrets set monitor_mail_smtp_default_password=xxxx
```

Then create the image with the run command
```bash
fly machine run . --schedule daily --restart no --name eraldy-monitor
```
or change
```bash
fly machine update 683d920b195638 --schedule daily --restart no
```

# Top-Level App

## About
Every [app](app.md) belongs to a [top-level domain](../src/main/java/net/bytle/tower/TowerTopLevelDomain.java).

ie the app `member.combostrap.com` belongs to the domain `combostrap.com`

You can configure the toplevel domain in the [env](env.md) [file](../.tower.yml)

## Example for the combostrap domain

If the path name is `combo`, the configuration can be set with `combo.toplevel.domain`.

Example:
```yaml
combo.toplevel.domain: 'nico.lan'
```

In this case, the dev laptop is called `nico.lan`

and the local host file redirects the `apps domain` to `localhost`

```hosts
127.0.0.1 member.nico.lan # identity management
127.0.0.1 api.nico.lan # public api
127.0.0.1 tower.nico.lan # private api
```

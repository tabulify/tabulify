https://github.com/paketo-buildpacks/samples

```bash
pack build react-sample --buildpack paketo-buildpacks/nodejs --env "BP_NODE_RUN_SCRIPTS=build"
```

## Nixpack

alternative to buildpacks

App source + Nix packages + Docker = Image

https://github.com/railwayapp/nixpacks

With fly

```bash
fly deploy --nixpacks
fly machine run --build-nixpacks .
```

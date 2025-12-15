# Contrib

## Install your development environment

See [How to install your dev env](doc/Dev.md)

## Scripts

All scripts entry can be seen in [Taskfile](../Taskfile.yml)

### Ntabul - Next Tabul

[ntabul](script/ntabul) - `ntabul (Next Tabul)` is the script that run the next version of `tabul`.

It can run:

* with maven: change your code, run `ntabul` with the `--compile` option and see your changes instantly
* with the release assembly:
  * `ntabul --install` to install it
  * then `ntabul` to run next command

### Release

[release](doc/release.md) - Run all test, and creates a tag if successful

### Ryuk Stop

[ryuk-stop](script/ryuk-stop) - Stops left-over [ryuk test container](https://github.com/testcontainers/moby-ryuk)
that should have stop but have not.

### Documentation utility

* [site-start task](../Taskfile.yml) to start the documentation
* [site-broken task](../Taskfile.yml) to get the broken links
* [site-sync task](../Taskfile.yml) to sync the database
* [site-frontmatter task](../Taskfile.yml) to sync the frontmatter
* [doc-exec](script/doc-exec) - Run code in the code block of
  the [tabulify website](../docs-tabul/README.md)
  and update the console block

## Doc

The [doc directory](doc) contains general dev documentation

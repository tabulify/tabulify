# Contrib

## Install your development environment

See [How to install your dev env](doc/Dev.md)

## Scripts

All scripts entry can be seen in [Taskfile](../Taskfile.yml)

### Ntabul - Next Tabul

[ntabul](script/ntabul) - Next Tabul: The next version of tabul. Change your code, run `ntabul` and see your changes

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
* [install-cli](script/install-cli) - Create a client distribution and installs it locally
* [doc-exec](script/doc-exec) - Run code in the code block of
  the [tabulify website](../docs-tabul/README.md)
  and update the console block


## Doc

The [doc directory](doc) contains general dev documentation

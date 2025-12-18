# Release Steps

## Note/Rules

* Release is the release of the tabul cli project
* Release is done via JReleaser Maven
* Release command should be invoked from the root directory
  * Why? TemplateDirectory in [brew](https://jreleaser.org/guide/latest/reference/packagers/homebrew.html) is relative
    to the current directory and accepts no template

## How to publish when the release was created

### Docker publishing

Download
https://github.com/tabulify/tabulify/releases/download/v2.0.0/tabulify-2.0.0-jre-alpine-x64.zip
to
`cli-tabul/target/jreleaser/assemble/tabulify-jre/jlink/tabulify-2.0.0-jre-alpine-x64.zip`
then rerun docker publishing
```bash
VERSION=2.0.0
curl -L -o cli-tabul/target/jreleaser/assemble/tabulify-jre/jlink/tabulify-$VERSION-jre-alpine-x64.zip https://github.com/tabulify/tabulify/releases/download/v$VERSION/tabulify-$VERSION-jre-alpine-x64.zip
mvnw jreleaser:publish -Djreleaser.packagers=docker -Djreleaser.select.platforms=linux_musl-x86_64
```


## Test

Start the Test script

```bash
release-test
release-test -rf xxx
```

## Doc Sync and broken links

* Sync, frontmatter, and broken links

```bash
# No Duplicate
task site-sync
# Sync frontmatter
task site-frontmatter
# no Broken Link
task site-broken
```

## Doc execution

In this step, we execute all docs, and we check the diff.

Note on documentation:

* The unit of doc execution is the page.
* Doc should be deterministic (ie don't change between unit run)
* No change should be seen between run
* The documentation writer is the driver of the run environment.
  * It should start/stop services as needed
  * We don't recreate the env each time (`.tabul.yml`)
  * If you want to get a clean environment, you need to delete `.tabul.yml` in the document ie

```md
<unit>
<code lang="bash">
tabul data drop --not-strict .tabul/.tabul.yml@home
</code>
</unit>
```

Procedure:

- Install the cli in `/opt/tabulify`
  * The doc is executed from this directory for better documentation
  * The cli is the jre distribution to test the added modules in the runtime

```bash
ntabul --install
```

* First execution, purge the cache and execute all docs with our [doc-exec wrapper](../script/doc-exec)

```bash
# first run: purge the whole doc cache and run
doc-exec --purge-cache run **/*.txt
# or for a quick iteration only the getting started
doc-exec --purge-cache run howto/getting_started/*
```

* Next executions, execute the docs without purging the cache. You can do it:
  * With debugger by executing the test `DocExec` of the [TabulTmpTest class](AdhocRunTabulTmpTest.md)
  * or at the command line

```bash
doc-exec run **/*.txt
# if fail, you can also run only one page
doc-exec --no-cache howto/postgres/anonymous_code_block
```

## Distribution Test

we create a:

* nojre distribution that is tested at brew installation
* jre that is tested locally when running doc-exec

## Todo

### Set process name in start script

* https://github.com/airlift/procname - Set process name for Java

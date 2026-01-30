# Release Steps

## Note/Rules

* Release is the release of the tabul cli project
* Release is done via JReleaser Maven
* Release command should be invoked from the root directory
  * Why? TemplateDirectory in [brew](https://jreleaser.org/guide/latest/reference/packagers/homebrew.html) is relative to the current directory and accepts no template


## Steps
### Test

Tests should be successful. Start the Test script

```bash
release-test
# restart from a module after a failure
release-test -rf module-name
```

### Doc Sync and broken links

* Sync, frontmatter, and broken links

```bash
# No Duplicate
task site-sync
# Sync frontmatter
task site-frontmatter
# no Broken Link
task site-broken
```

### Note and prerequisites on documentation execution

The below sections explains how to develop code example that
can be executed against the next tabul iteration.

#### Doc Code Example and Rules

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

#### How to test a next tabul execution

To test a tabul command outside the documentation or for the next release,
you need to replace `tabul` by `ntabul`

#### Documentation execution Prerequisite

The next cli iteration should be installed in `/opt/tabulify`
  * The doc is executed from this directory for better documentation
  * The cli is the jre distribution to test the added modules in the runtime

```bash
ntabul --install
# or
task install-cli
```

#### Task util

It exists some utility task

* to install only cli parts in `/opt/tabulify`
```bash
# install only the jars
task install-cli-jars
# you can also install only the resources
task install-cli-resources
```
* to copy the sqlite database from WSL
```bash
task cp-sqlite
```

### Doc execution

In this step, we:
* execute all docs with the [doc-exec wrapper](../script/doc-exec),
* and check the diff with git or the IDE.

Procedure:

* First execution, purge the cache and execute all docs with our [doc-exec wrapper](../script/doc-exec)

```bash
# first run: purge the whole doc cache and run
doc-exec --purge-cache run **/*.txt
# or for a quick iteration only the getting started
# don't use the cache
doc-exec --no-cache run howto/getting_started/*
# purge all cache
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

## Release

Create a tag to start the GitHub Workflow with [release-tag](../script/release-tag) script

## Note on Distribution
### Distribution Test

we create a:

* nojre distribution that is tested at brew installation
* jre distribution that is tested locally when [running doc-exec](#doc-execution)

## Todo

### Set process name in start script

* https://github.com/airlift/procname - Set process name for Java

## Support

### How to publish when the release was created but not published

Due to GitHub timeout, the release may be created but not published.
This sections shows you how to publish Tabulify if it's the case.

#### Docker publishing

```bash
VERSION=early-access
JLINK_DIRECTORY="cli-tabul/target/jreleaser/assemble/tabulify-jre/jlink"
mkdir -p $JLINK_DIRECTORY
curl -L -o $JLINK_DIRECTORY/tabulify-$VERSION-jre-alpine-x64.zip https://github.com/tabulify/tabulify/releases/download/v$VERSION/tabulify-$VERSION-jre-alpine-x64.zip

# Assembly is mandatory for no reason
# When publishing with docker: Error: Missing outputs for java-archive.tabulify-nojre. Distribution tabulify-nojre has not been assembled
mvnw install -DskipTests -pl "cli-tabul" -Dorg.slf4j.simpleLogger.defaultLogLevel=$LEVEL
mvnw jreleaser:assemble -pl "cli-tabul" -Djreleaser.assemblers=javaArchive

# The work
mvnw jreleaser:publish -pl "cli-tabul" -Djreleaser.packagers=docker -Djreleaser.select.platforms=linux_musl-x86_64
```

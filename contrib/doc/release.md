# Release Steps

## Test

Start the [release-test](../script/release-test) script
```bash
release-test
```
It will start the test for each module.
Why? The test containers are going down when the JVM goes down.

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
    ie

```md
<unit>
<code lang="bash">
tabul data drop --not-strict .tabul/.tabul.yml@home
</code>
</unit>
```

Procedure:

- Install the cli (The doc is executed with the tabul distribution.)

```bash
task install-cli
```

* First execution, purge the cache and execute all docs with our [doc-exec wrapper](../script/doc-exec)

```bash
# first run: purge the whole doc cache and run
doc-exec --purge-cache run **/*.txt
```

* Next executions, execute the docs without purging the cache. You can do it:
  * With debugger by executing the test `DocExec` of the [TabulTmpTest class](AdhocRunTabulTmpTest.md)
  * or at the command line

```bash
doc-exec run **/*.txt
# if fail, you can also run only one page
doc-exec --no-cache howto/postgres/anonymous_code_block
```

## Release/Tag

No errors, release.
```bash
release-tag
```

## Todo

### Set process name in start script

* https://github.com/airlift/procname - Set process name for Java

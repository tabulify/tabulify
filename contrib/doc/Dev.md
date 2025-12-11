# Dev environment

## About

How to set up a dev environment

## Steps

### Clone

```bash
git clone --recurse-submodules https://github.com/tabulify/tabulify
```

### Sdk (Java, Maven, Jreleaser)

Open the terminal to execute [.envrc](../../.envrc)

Sdks:

* are managed via sdkman in [setup-sdk.sh](../../.envrc.d/setup-sdk.sh)
* versions are in `pom.xml`

### Idea

* Jdk: Project Structure > Project Settings > Sdk > Path of sdkman
* Maven: Path of sdkman
* Run Target (WSL)
* Default for test run: Tag. `!slow`

### Add the adhoc test runtime class

Create the [AdHoc class](AdhocRunTabulTmpTest.md)
to run Tabul or DocRun from the IDE and get a debugger

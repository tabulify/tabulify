# Idea (Intellij)



## Memory

To not get any lagging, Idea can go above 2000Mo

Minimal: 3000Mo

You can see the memory in:
Right-click the status bar and select Memory Indicator


## Gradle Dependencies

If any problems:
* Refresh with the gradle windows
* Don't work: refresh with the command line
```bash
gradle idea
```

## Shared Index

We use shared index to speed up the indexing.

Indexing diagnostic:
C:\Users\gerardnico\AppData\Local\JetBrains\IntelliJIdea2023.2\log\indexing-diagnostic

The indexing uses a diff.
It should be done once a week in a normal setup with a team.

Steps:
  * If done locally, close Idea
  * Download the [zip cli tool](https://packages.jetbrains.team/maven/p/ij/intellij-shared-indexes/com/jetbrains/intellij/indexing/shared/ij-shared-indexes-tool-cli/) and unzip it
  * Create the indexes for a project (1hr)
```dos
cd /D C:\ij-shared-indexes-tool-cli-0.9.9\
.\bin\ij-shared-indexes-tool-cli indexes --ij C:\IntelliJ-IDEA-2023.1 --project d:\code\java-mono --base-url https://idea-shared-indexes.t0w0.c15.e2-3.dev
```
  * Upload the indexes created for all projects
```cmd
REM 7 minutes with progres feedback
cd /D C:\ij-shared-indexes-tool-cli-0.9.8\ij-shared-indexes-tool-data\server
rclone copy . idea-shared-indexes:idea-shared-indexes/ --progress
```
  * Then for each project a `intellij.yaml` is created
```yml
sharedIndex:
  project:
    - url: https://idea-shared-indexes.t0w0.c15.e2-3.dev/project/project-name
  # Consent to download: https://www.jetbrains.com/help/idea/shared-indexes.html#configure-index-update
  consents:
    - kind: project
      decision: allowed
```

### How to verify, log error ?
You can also search in the Log the URL: C:\Users\gerardnico\AppData\Local\JetBrains\IntelliJIdea2023.3\log
You can see that it download the list first:
```
https://idea-shared-indexes.t0w0.c15.e2-3.dev/project/java-mono/vcs/list.json.xz
```

Example:
```
2023-11-01 14:52:43,150 [ 100716]   INFO - #c.i.i.s.download - There is no need to download shared indexes for ProjectSharedIndexSuggestion(SharedIndexId(kind=project, url=https://idea-shared-indexes.t0w0.c15.e2-3.dev/project/java-mono, indexId=2f1e24c5-7f75c59c9a7f772ec50cabbcecc006da33ec1453a2f2babdc9db62103120720f), https://idea-shared-indexes.t0w0.c15.e2-3....
```

### Where are the index?
Project indexes are downloaded to `index/shared_indexes` in the IDE system directory.

Ie on windows:
```
C:\Users\gerardnico\AppData\Local\JetBrains\IntelliJIdea2021.3\index\shared_indexes
```


### Result of the Evaluation

https://www.jetbrains.com/help/idea/shared-indexes.html#evaluate_saved_time_boost

```cmd
.\bin\ij-shared-indexes-tool-cli boost --ij C:\IntelliJ-IDEA-2023.1 --project d:\code\java-mono
```
```txt
==================== SUMMARY ====================
`No project shared indexes` vs `project shared indexes` from http://127.0.0.1:25561/project/java-mono: 14m 30s 691ms vs 6m 47s 360ms.
Boost depends on a project size and a proper configuration.
If speed up is not good enough, please submit a support request https://intellij-support.jetbrains.com/hc/en-us with logs attached

.\bin\ij-shared-indexes-tool-cli boost --ij C:\IntelliJ-IDEA-2023.1 --project d:\code\java-mono
.\bin\ij-shared-indexes-tool-cli indexes-server --ij C:\IntelliJ-IDEA-2023.1 --project d:\code\java-mono
.\bin\ij-shared-indexes-tool-cli indexes --ij C:\IntelliJ-IDEA-2023.1 --project d:\code\java-mono --base-url https://idea-shared-indexes.t0w0.c15.e2-3.dev/java-mono
```

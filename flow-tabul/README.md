# The Flow module

A direct acyclic graph (DAG).

* The vertices in the graph represent the pipeline’s processes and operators
* The edges represent the data connections (i.e. channels) between them.

# Workflow

Publisher / Subscriber model

* Every task get data
  * from a publisher
  * and output to a subscriber

Algebraic data type:

* A message that wraps one data path
* Tasks can then be composed

Why a list of data path ? Because you may have dependency between data resources. The dimension table should be created
before the fact table

Why the message contains also the target ?

* Basically every data workflow starts from a source and transfers the data into a target.
* That's the definition of a data pipeline.
* A source may have several target during a pipeline. Example for a csv file:
  * The original (table for instance)
  * The last one (a directory)

Message:

* One source
* Log
  * with args
  * wait, run, start and stop time by tasks
  * status (Open, Failed, Successful, Out-filtered)

Feedback:

* Feedback is a list of data path with the wait / run end and start time in each each step in the pipeline to be able to
  produce a gantt chart with one data path on each line and the time on the x axis
* The number of message produced should be the same than the number of message received.

Task:

* input name and output name to create link between tasks
* properties
* needs all dependent data path (example: create, drop, truncate)

Operations:

* set - set properties (variable) generally on target that may be used to do variable expansion
* split (split the content)
* fork (split the list of message - the pipeline)
* filter (message will not be processed further)
* collect (collect message in a list fashion)

## Other syntax in the wild

* [Github Action](https://docs.github.com/en/free-pro-team@latest/actions/reference/workflow-syntax-for-github-actions)

A workflow run is made up of one or more jobs. Jobs run in parallel by default.

## Runners

* [Github](https://docs.github.com/en/free-pro-team@latest/actions/reference/specifications-for-github-hosted-runners)

## Operators

* https://www.nextflow.io/docs/latest/operator.html

## Event

Workflow events:

```json
{
  "runName": "<run name>",
  "runId": "<uuid>",
  "event": "<started|process_submitted|process_started|process_completed|error|completed>",
  "utcTime": "<UTC timestamp>",
  "trace": {
    "...": "..."
  },
  "metadata": {
    "...": "..."
  }
}
```

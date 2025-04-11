# Workflow

Publisher / Subscriber model

* Every task get a list of message
  * from a publisher
  * and output to a subscriber a list of message

Algebraic data type:
  * A list of message
  * Tasks can then be composed
  * A tasks get the whole list.
  * You can filter a list

Why a list of data path ?
Because you may have dependency between data resources. The dimension table should be created before the fact table


Why the message contains also the target ?
  * Basically every data workflow starts from a source and transfers the data into a target.
  * That's the definition of a data pipeline.
  * A source may have several target during a pipeline. Example for a csv file:
     * The original (table for instance)
     * The last one (a directory)

Message:
  * List of Source
  * Flow fields:
    * Tasks List (log)
       * with args
    * Current Status (Open, Failed, Successful, Out-filtered)


The number of message produced should be the same than the number of message
received.

Task:
  * input name and output name to create link between tasks
  * properties

Operations:
  * set - set properties (variable) generally on target that may be used to do variable expansion
  * split (split the content)
  * fork (split the list of message - the pipeline)
  * filter (message will not be processed further)
  * collect (collect message in a list fashion)

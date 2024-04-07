# Job Schema


## About

The `Job schema` has state machine data (ie state) that will become immutable after execution.


## Example

Job, Order execution: once the job or order execution is finished or closed, no further data or action will be done

List:
  * mailing job
  * import job

## Data Type

The data:
* is not immediately temporary. We lost information if the data is lost but the business can go on.
* become immutable. If we lost the data, after the immutability, we lose only the history.
* can be reconstructed from analytics data (We may retrieve the history)
* may be archived
* is not invoice/finance data

## Why? Maintenance

* This schema can become pretty big (one row for each email)
* The data may be archived
* The backup plan is not the same as the main/metadata schema (`cs-realms`)

# Play (Workflow)

## About

A workflow is a DAG (Directed Acyclic Graph)

## Composition

### Parameters

  * in the pipeline
  * at the command line

## Existing Workflow

### PipeDream

Just a sequence of linear steps that starts with a trigger

  * https://docs.pipedream.com/workflows/steps/#types-of-steps
  * https://github.com/PipedreamHQ/pipedream#workflows

### Airflow
Example with Airflow:
  * [Argument (scope the data pipeline)](https://airflow.apache.org/docs/stable/tutorial.html#default-arguments)

| owner | airflow |
| depends_on_past | False |
| start_date | days_ago(2) |
| email | john@example.com ] |
| email_on_failure | False |
| email_on_retry | False |
| retries | 1 |
| retry_delay | timedelta(minutes=5) |
| queue | bash_queue |
| pool | backfill |
| priority_weight | 10 |
| end_date | datetime(2016, 1, 1) |
| wait_for_downstream | False |
| dag | dag |
| sla | timedelta(hours=2) |
| execution_timeout | timedelta(seconds=300) |
| on_failure_callback | some_function |
| on_success_callback | some_other_function |
| on_retry_callback | another_function |
| sla_miss_callback | yet_another_function |
| trigger_rule | all_success |

## Command

  * templated command (powered by [Macros](https://airflow.apache.org/docs/stable/macros.html))

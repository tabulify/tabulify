# Analytics


## How to track

See https://datacadamia.com/marketing/analytics/tracker#methods

## Type

There is 2 types of analytics:
* real time / reporting: the last 10 sign up
* aggregate: the sign-up by month, ...

As of today:
- the realtime is done via a Postgres table
- the aggregate also but could be moved to DuckDb above the JSONL file
See https://datacadamia.com/db/duckdb_jsonl_nested

## User identification

[analytics User identification](analytics-user-identification.md)

## Date Time
See [date time](analytics-datetime.md)

## Page view

Track page views as a single event type by using a constant event_name
https://docs.mixpanel.com/docs/tracking/how-tos/effective-server-side-tracking#tracking-page-views
Fire page view events only on successful responses to the client

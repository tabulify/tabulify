# Local Date Time Analytics


## About

We care about local time because we want to know when our users
are using our tool.

Local time is part of [analytics](analytics.md)

We store:
* the time in UTC because this value is available everywhere by default (Db, browser, ...)
* the timezone to show the local time

Note that the creation and modification date are UTC system date
and are not part of the analytics.

## Parts

### Database

Database stores mostly the date time at `UTC+00:00` and format
the string output based on the locale
(This is the case of our [Postgres database](postgres.md)).

Timezone in Postgres have two identifiers:
  * the name (ie `Europe/London`)
  * or the abbreviation (ie `BST`).

```sql
select name, abbrev, extract( epoch from utc_offset)/3600 as "utc offset", utc_offset, is_dst as "Day Saving Time"
from pg_timezone_names
order by "utc offset";
```

```
SELECT current_setting('TIMEZONE');
```
```
Europe/Berlin
```

You can show the date at the locale time
```sql
select dt at time zone 'BST', dt from dt;
```
```
2004-10-19 09:23:54.000000   2004-10-19 08:23:54.000000 +00:00
```

### Javascript

Javascript does that also:
  * the output of Iso string or Json string is the time at `UTC+00:00`
  * the default output of the date is locale (ie based on the browser time zone and language).

The default timezone in Javascript is the name:
```javascript
Intl.DateTimeFormat().resolvedOptions().timeZone
```
```txt
Europe/Amsterdam
```

### OS

The OS date is `UTC+00:00`
```
date +%Z
```
```
UTC
```

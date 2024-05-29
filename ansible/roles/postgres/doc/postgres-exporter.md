https://github.com/prometheus-community/postgres_exporter

http://localhost:9187/metrics

Can be configured via

* [command line flags](https://github.com/prometheus-community/postgres_exporter?tab=readme-ov-file#flags)
* [env](https://github.com/prometheus-community/postgres_exporter?tab=readme-ov-file#environment-variables)
* `postgres_exporter.yml` config file

```
postgres_exporter --help
```

Metrics

```
pg_stat_activity_count
pg_stat_activity_max_tx_duration
pg_stat_archiver_archived_count
pg_stat_archiver_failed_count
pg_stat_bgwriter_buffers_alloc
pg_stat_bgwriter_buffers_backend_fsync
pg_stat_bgwriter_buffers_backend
pg_stat_bgwriter_buffers_checkpoint
pg_stat_bgwriter_buffers_clean
pg_stat_bgwriter_checkpoint_sync_time
pg_stat_bgwriter_checkpoint_write_time
pg_stat_bgwriter_checkpoints_req
pg_stat_bgwriter_checkpoints_timed
pg_stat_bgwriter_maxwritten_clean
pg_stat_bgwriter_stats_reset
pg_stat_database_blk_read_time
pg_stat_database_blk_write_time
pg_stat_database_blks_hit
pg_stat_database_blks_read
pg_stat_database_conflicts_confl_bufferpin
pg_stat_database_conflicts_confl_deadlock
pg_stat_database_conflicts_confl_lock
pg_stat_database_conflicts_confl_snapshot
pg_stat_database_conflicts_confl_tablespace
pg_stat_database_conflicts
pg_stat_database_deadlocks
pg_stat_database_numbackends
pg_stat_database_stats_reset
pg_stat_database_tup_deleted
pg_stat_database_tup_fetched
pg_stat_database_tup_inserted
pg_stat_database_tup_returned
pg_stat_database_tup_updated
pg_stat_database_xact_commit
pg_stat_database_xact_rollback
pg_stat_replication_pg_current_wal_lsn_bytes
pg_stat_replication_pg_wal_lsn_diff
pg_stat_replication_reply_time
pg_replication_lag
pg_database_size_bytes
```

# Query

No more supported by postgres-exporter
https://git.app.uib.no/caleno/helm-charts/-/blob/master/stable/prometheus-postgres-exporter/values.yaml#L214

```yaml
pg_database:
  query: "SELECT pg_database.datname, pg_database_size(pg_database.datname) as size FROM pg_database"
  master: true
  cache_seconds: 30
  metrics:
    - datname:
        usage: "LABEL"
        description: "Name of the database"
    - size_bytes:
        usage: "GAUGE"
        description: "Disk space used by the database"
```

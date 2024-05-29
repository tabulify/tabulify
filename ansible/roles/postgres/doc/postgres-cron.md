```sql
SELECT j.*,
       jd.*
FROM cron.job j
       LEFT JOIN cron.job_run_details jd
                 ON j.jobid = jd.jobid
ORDER BY jd.start_time DESC;
```

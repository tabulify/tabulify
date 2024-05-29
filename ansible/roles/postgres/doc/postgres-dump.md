# Postgres Dump

## Dir

Example: data for data file and toc for code.

```bash
/data/pgdump/dumpfile-db-postgres/3229.dat.gz
/data/pgdump/dumpfile-db-postgres/3231.dat.gz
/data/pgdump/dumpfile-db-postgres/3389.dat.gz
/data/pgdump/dumpfile-db-postgres/3390.dat.gz
/data/pgdump/dumpfile-db-postgres/toc.dat
```

* No data change

```bash
repository 337fcbe2 opened (version 2, compression level auto)
comparing snapshot 5aecd0a8 to 8098a02a:

[0:00] 100.00%  13 / 13 index files loaded
M    /data/pgdump/dumpfile-db-postgres/toc.dat

Files:           0 new,     0 removed,     1 changed
Dirs:            0 new,     0 removed
Others:          0 new,     0 removed
Data Blobs:      1 new,     1 removed
Tree Blobs:      4 new,     4 removed
  Added:   7.653 KiB
  Removed: 7.656 KiB
```

* Added a table

```bash
repository 337fcbe2 opened (version 2, compression level auto)
comparing snapshot 8098a02a to 2d62b66e:

[0:00] 100.00%  14 / 14 index files loaded
-    /data/pgdump/dumpfile-db-postgres/3225.dat.gz
-    /data/pgdump/dumpfile-db-postgres/3227.dat.gz
+    /data/pgdump/dumpfile-db-postgres/3229.dat.gz
+    /data/pgdump/dumpfile-db-postgres/3231.dat.gz
-    /data/pgdump/dumpfile-db-postgres/3385.dat.gz
+    /data/pgdump/dumpfile-db-postgres/3389.dat.gz
+    /data/pgdump/dumpfile-db-postgres/3390.dat.gz
     M    /data/pgdump/dumpfile-db-postgres/toc.dat
-    /data/pgdump/dumpfile-sc-test.sql/

Files:           4 new,     3 removed,     1 changed
Dirs:            0 new,     1 removed
Others:          0 new,     0 removed
Data Blobs:      1 new,     1 removed
Tree Blobs:      4 new,     5 removed
Added:   7.992 KiB
Removed: 7.666 KiB
```

* Added data (a row)

```bash
repository 337fcbe2 opened (version 2, compression level auto)
comparing snapshot 2d62b66e to bcbdf9df:

[0:00] 100.00%  15 / 15 index files loaded
M    /data/pgdump/dumpfile-db-postgres/3390.dat.gz
M    /data/pgdump/dumpfile-db-postgres/toc.dat

Files:           0 new,     0 removed,     2 changed
Dirs:            0 new,     0 removed
Others:          0 new,     0 removed
Data Blobs:      2 new,     1 removed
Tree Blobs:      4 new,     4 removed
Added:   8.031 KiB
Removed: 7.992 KiB
```

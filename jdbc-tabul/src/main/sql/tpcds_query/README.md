# TPCDS

## About

The tpcds queries for each database.

More ... see [TPC-DS](https://tabulify.com/tpcds)


# Steps
## Installation

```bash
sudo apt-get install gcc make flex bison byacc git
git clone https://github.com/databricks/tpcds-kit
cd tpcds-kit/tools
make OS=LINUX
chmod +x dsqgen
chmod +x dsdgen
```

Other tpcds-kit repo for info:
* https://github.com/cloudera/impala-tpcds-kit

## Modify dialect if needed

In dialect files:
  * `sqlserver`  (tpcds-kit/query_templates/sqlserver.tpl)
  * `oracle`  (tpcds-kit/query_templates/oracle.tpl)
  * `netezza`  (tpcds-kit/query_templates/netezza.tpl)
add:
```
define _END = "";
```
to avoid:
```
ERROR: Substitution'_END' is used before being initialized at line 63 in /home/admin/code/tpcds-kit/query_templates/query1.tpl
```

## Generate

They can be generated with the `TpcdsQueryGenerator.java`
(Doc: https://datacadamia.com/data/type/relation/benchmark/tpcds/dsqgen)


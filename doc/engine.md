# Engine


## Looker
By default, Looker uses a [HyperSQL](http://hsqldb.org/) in-memory database as the application's internal database. On busy instances, this database can grow to be gigabytes in size, which can lead to performance issues. For these large deployments, customers should replace the HyperSQL database with a MySQL database backend (6).

https://help.looker.com/hc/en-us/articles/360033946033-Looker-Multistage-Development-Framework-Dev-QA-Prod-for-Customer-Hosted-Deployments
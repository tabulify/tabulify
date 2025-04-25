# Connection (formerly DataStore)


## About

The datastore object is responsible for:
  * connection
  * metadata:
     * data path creation
     * data resources selection
  * and type management

The data system is a child object of the datastore responsible for:
  * data manipulation - DML (insert)
  * data definition - DDL (create)


## Example of datastore / data system

### By type
  * jdbc
    * sql server
    * ...
  * file (csv, excel)
    * local
    * sftp,
    * http
  * browser (?)

### By product

Warehouse:
  * Redshift
  * BigQuery,
  * SnowflakeDB
Streaming targets:
  * Pub/Sub,
  * Kinesis
File System:
  * S3,
  * GCS
Database:
  * Elasticsearch

#### Source Stream

Apache Kafka
AWS Kinesis
Http
Web sockets
Network Sockets
Files
Teradata Listener

#### Output Stream & Sinks

Kafka, Kinesis or AMQP Streams
Databases (SQL, NoSQL)
Datawarehouses
Web Services
Files
New or Existing Blaze Streams

#### File

Discoverable Formats include:

Line
CSV
JSON
XML
Binary (Avro/ProtoBuf)


#### Application

Stripe: https://github.com/fishtown-analytics/stripe

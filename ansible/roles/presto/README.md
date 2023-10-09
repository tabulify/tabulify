# Presto Installation


## About

Presto works on file only with Hive Metadata and Hdfs.

This role was never fully developed because we switched to Drill
that allows a single node installation without hdfs.

## Etc / Configuration

The etc directory (inside the installation directory) hold the following configuration:
  * Node Properties: environmental configuration specific to each node
  * JVM Config: command line options for the Java Virtual Machine
  * Config Properties: configuration for the Presto server
  * Catalog Properties: configuration for Connectors (data sources)

## Log

After launching, you can find the log files in var/log:
* `launcher.log` : This log is created by the launcher and is connected to the stdout and stderr streams of the server. It will contain a few log messages that occur while the server logging is being initialized and any errors or diagnostics produced by the JVM.
* `server.log` : This is the main log file used by Presto. It will typically contain the relevant information if the server fails during initialization. It is automatically rotated and compressed.
* `http-request.log` : This is the HTTP request log which contains every HTTP request received by the server. It is automatically rotated and compressed.

## Ref
  * [Deployment](https://prestodb.io/docs/current/installation/deployment.html)
  * [Based on the Docker image build](https://prestodb.io/docs/current/installation/deployment.html#an-example-deployment-with-docker)
  * https://github.com/elisska/ansible-presto/blob/master/roles/install-presto-coordinator/tasks/main.yml

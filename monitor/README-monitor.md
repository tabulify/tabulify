# Monitor


This project runs on schedule and monitor the state of the system.

It will check:
* the DNS configuration
* the status point
* ...


## Deploy

The machine should be created first. See [](./doc/monitor-init.md)

Then deploy by updating the image.

* with gradle
```bash
..\gradlew deploy
```
* at the command line
```bash
# build the jar
..\gradlew assemble
# update the image
fly machine update 683d920b195638 --yes --schedule daily --restart no --dockerfile ./Dockerfile
```

## Start manually

You can start the container with the machine id
```bash
fly machine start 683d920b195638
```

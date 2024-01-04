# Monitor


This project runs on schedule and monitor the state of the system.

It will check:
* the DNS configuration
* the status point
* ...


## Deploy

The machine should be created first. See [](./doc/monitor-fly-machine-init.md)

Then deploy by updating the image.
* new secret? (Return an error but secret where imported)
```bash
fly secrets import < .monitor.secret.env
```
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


## Dashboard example

https://status.checklyhq.com/
https://images.prismic.io/checklyhq/03e3bce4-2910-4068-ab70-fe0ef76ec806_CleanShot+2023-12-13+at+20.55.30%402x.png?auto=compress%2Cformat&fit=max&w=1200

https://www.checklyhq.com/the-new-relic-alternative-with-no-hidden-fees/

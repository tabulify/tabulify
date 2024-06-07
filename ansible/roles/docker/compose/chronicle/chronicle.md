# Chronicle Job Scheduler

## Doc

https://github.com/jhuckaby/Cronicle

## Docker

https://github.com/soulteary/docker-cronicle

```bash
docker run \
        -v /etc/localtime:/etc/localtime:ro \
        -v /etc/timezone:/etc/timezone:ro \
        -v `pwd`/data/data:/opt/cronicle/data:rw \
        -v `pwd`/data/logs:/opt/cronicle/logs:rw \
        -v `pwd`/data/plugins:/opt/cronicle/plugins:rw \
        -p 3012:3012 \
        --hostname cronicle \
        --name cronicle \
        soulteary/cronicle
```

User/Pwd: admin/admin

## Execute Job via API

[Run API Hook via get](https://github.com/jhuckaby/Cronicle/blob/master/docs/APIReference.md#run_event):

```bash
curl https://chronicle.eraldy.dev/api/app/run_event/v1?id=elx4r160c01&title=MyTitle&api_key=8d7ec5b0ec1789eed39a8a7fd8e09a16
```

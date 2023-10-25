# Smtp Inbox

## About
`Smtp inbox` is the inbox application that receive email, process and deliver them.

To deploy `inbox.eraldy.com`,
```bash
..\gradlew deploy
```

## Fly Init

The command that we have run to init the fly environment
```
..\gradlew assemble
```

```bash
docker build -t com.eraldy/smtp-inbox -f deploy/inbox.eraldy.com/Dockerfile .
```

```dos
fly launch ^
  --dockerfile deploy/inbox.eraldy.com/Dockerfile ^
  --image-label com.eraldy/inbox ^
  --name eraldy-inbox ^
  --internal-port 25 ^
  --local-only ^
  --file-secret deploy/inbox.eraldy.com/.smtp-server-inbox-fly-secret.env ^
  --region ams
```

```bash
flyctl secrets import < deploy/inbox.eraldy.com/.smtp-server-inbox-fly-secret.env
```
```bash
fly scale count 1
```

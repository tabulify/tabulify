# Overmmind

* https://fly.io/docs/app-guides/multiple-processes/#use-a-procfile-manager
* https://github.com/DarthSim/overmind

```Dockerfile
FROM golang as overmind
RUN apt-get update && \
apt-get install -y \
bash \
tmux \
curl \
# The Overmind binary is installed to $(go env GOPATH)/bin
RUN GO111MODULE=on go get -u github.com/DarthSim/overmind/v2
```

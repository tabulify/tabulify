# Ubuntu to get the last git stable
# To be abel to add the ppa below
# as specified there
# https://git-scm.com/download/linux
FROM ubuntu:24.10


# Linux
RUN apt-get update
RUN apt-get install add-apt-repository
RUN add-apt-repository ppa:git-core/ppa
RUN apt-get install git-core



# https://git-scm.com/book/en/v2/Git-on-the-Server-Git-Daemon \
# The --reuseaddr option allows the server to restart without waiting for old connections to time out,  \
# the --base-path option allows people to clone projects without specifying the entire path,  \
# the path at the end tells the Git daemon where to look for repositories to export.  \
# If you’re running a firewall, you’ll also need to punch a hole in it at port 9418
# supervisor
# git daemon --reuseaddr --base-path=/srv/git/ /srv/git/


# Setting up Smart HTTP is basically just enabling a CGI script
# that is provided with Git called git-http-backend on the server.
# `git-http-backend` do all the negotiation
# to send and receive data over HTTP.
# It does not implement any authentication itself,
# but that can easily be controlled at the layer of the web server that invokes it.
# https://git-scm.com/docs/git-http-backend#_examples
# source: https://github.com/git/git/blob/master/http-backend.c
# ruby: https://github.com/grackorg/grack
# go: https://github.com/asim/git-http-backend
# python: https://github.com/dvdotsenko/git_http_backend.py

CMD git http-backend

## 
## This script will create a Oracle XE database docker image
## 
## Parameters:
##    1- The Oracle User Name
##    2- The Oracle Password
## to download the installation binary
##
## Most of the files comes from https://github.com/oracle/docker-images.git
## docker-images/OracleDatabase/dockerfiles/11.2.0.2
##
##  There are two ports that are exposed in this image:
##     * 1521 which is the port to connect to the Oracle Database.
##     * 8080 which is the port of Oracle Application Express (APEX). (Example: http://192.168.99.100:8080/apex/)
##
## Default Password for SYS and SYSTEM: welcome1
## SID: xe
##


# Download the file
ORACLE_LOGIN=$1
ORACLE_PWD=$2
softwareDownloader.sh -u $ORACLE_LOGIN -p $ORACLE_PWD http://download.oracle.com/otn/linux/oracle11g/xe/oracle-xe-11.2.0-1.0.x86_64.rpm.zip

# A check on the swap ask for more memory
# https://github.com/oracle/docker-images/issues/294
docker-machine stop
VBoxManage modifyvm default --cpus 2
VBoxManage modifyvm default --memory 8192

# Starting docker
docker-machine start
eval $(docker-machine env default --shell bash)

# Build the image
buildDockerImage.sh -v 11.2.0.2 -x

# Run the image

docker run --name oraclexe \
--shm-size=1g \
-p 1521:1521 -p 8080:8080 \
-e ORACLE_PWD=welcome1 \
oracle/database:11.2.0.2-xe

# TODO: The binding on the local file system doesn't work really well with mingw
# -v $(pwd)/startup:/opt/oracle/scripts/startup \ Startup script
# -v [<host mount point>:]/u01/app/oracle/oradata \ To keep the data


# Then before each test suit
docker start oraclexe

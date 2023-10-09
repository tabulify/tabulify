# Command Migration


## Backup
```bash
cd /opt/www
nohup zip -r all.zip ./* &
```
  * Check the jobs
```
jobs
```
  * Grab the PID 
```
ps -ef | grep zip
```
  * And monitor
```
pid=22018
count=0
while kill -0 $pid 2> /dev/null; do
    count=$(( $count + 1 ))
    echo "${count} - Process is running"
    sleep 10
done
echo "${count} - Process has exited"
```
## Restore

  * Send it to the target server
```bash
scp -p all.zip www-user@beau.bytle.net:/opt/www/all.zip
```
  * Unzip
```bash
nohup unzip all.zip &
```

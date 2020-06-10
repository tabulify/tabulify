# Nginx

## Conf

The server blocks should be added to `/etc/nginx/conf.d`


## Test

To test a DNS change you can use the map function of the chrome browser.

```bash
chrome.exe --host-resolver-rules="MAP gerardnico.com 212.186.33.26"
```
Example:
```dos
cd /D "C:\Program Files (x86)\Google\Chrome\Application\"
chrome.exe --host-resolver-rules="MAP bytle.net  164.132.99.202, MAP gerardnico.com 164.132.99.202, MAP datacadamia.com 164.132.99.202"
```

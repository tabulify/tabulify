# Chrome / ChromeDriver

## Ansible

```bash
ansible-playbook playbook-root.yml -i inventories/beau.yml --vault-id passphrase.sh --tags chrome
```


## Chrome Installation

### Automatic Yum Epel

See the [main.yml](./tasks/main.yml) that installs the package

In Epel, there is the following package available

```
#    chrome-remote-desktop.x86_64             99.0.4844.84-1.el7            epel
#    chromedriver.x86_64                      99.0.4844.84-1.el7            epel
#    chromium.x86_64                          99.0.4844.84-1.el7            epel
#    chromium-common.x86_64                   99.0.4844.84-1.el7            epel
#    chromium-headless.x86_64                 99.0.4844.84-1.el7            epel
```


Example of chrome driver start:
```bash
chromedriver
```
```
Starting ChromeDriver 99.0.4844.84 (81a11fc2ee8a41e17451f29195387f276d3bb379-refs/branch-heads/4844_74@{#6}) on port 9515
Only local connections are allowed.
Please see https://chromedriver.chromium.org/security-considerations for suggestions on keeping ChromeDriver safe.
ChromeDriver was started successfully.
```
### Manual / Google Chrome Repo

This is for information purpose, as we use the package manager.

See the [chrome.yml](./tasks/chrome.yml)

Installing Google Chrome add the Google repository to automatically keep Google Chrome up to date.
If you don’t want Google's repository, do “sudo touch /etc/default/google-chrome” before installing the package.
```bash
cat /etc/yum.repos.d/google-chrome.repo
```
```ini
[google-chrome]
name=google-chrome
baseurl=https://dl.google.com/linux/chrome/rpm/stable/x86_64
enabled=1
gpgcheck=1
gpgkey=https://dl.google.com/linux/linux_signing_key.pub
```

## Update

```bash
sudo yum update google-chrome-stable
```

## Ref

https://www.google.com/linuxrepositories/

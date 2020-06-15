# Ansible - Nexus Installation

## About 

An Ansible role to install and configure [Sonatype Nexus](http://www.sonatype.org/nexus/)

The extraction/installation process creates two sibling [directories](https://help.sonatype.com/repomanager3/installation/directories): 
  * an application directory 
  * and a data directory, sometimes called the "Sonatype Work" directory

## Port

8443 should be in the ovh firewall

## Prerequisites and dependency

For dependency (OS family,...) and licence, see [meta](meta/main.yml)

## Usage

```yaml
- name: Example Playbook
  roles:
    - name: Install nexus
      role: nexus
      nexus_version: 3.19.1-01
      nexus_group: nexus
      nexus_user: nexus
      nexus_root: /opt
      nexus_install_temp: /tmp/nexus
      nexus_pid_dir: /var/run/nexus
      nexus_shell: /sbin/nologin
      nexus_fqdn: nexus.example.com
      nexus_host: 0.0.0.0
      nexus_port: 8082
      nexus_context_path: nexus
      nexus_ssl: False
      nexus_ssl_port: 8443
      nexus_ssl_keypass: changeit
      nexus_ssl_storepass: changeit
      nexus_download_force: no
      nexus_link: nexus-current
      # Firewall (list of white ip or a range)
      nexus_white_ip_address:
        - 139.177.205.84
        - 0.0.0.0/0 # Public
      # Derived 
      nexus_download_base_url: http://download.sonatype.com/nexus/3/ 
      nexus_distribution_name: nexus-{{ nexus_version }}-bundle.tar.gz
      nexus_application_directory: nexus-{{ nexus_version }}
```
where:
  * `nexus_version` is the version of nexus ([Full list](https://help.sonatype.com/repomanager3/download/download-archives---repository-manager-3))
  * `nexus_fqdn` is the full qualified domain name used by nginx to proxy every request in port 80 to the nexus_port. 
  * user properties are:
    * `nexus_user` is the user name
    * `nexus_group` is the user group 
  * directories properties are:
    * `nexus_root` is the root directory (default to `/opt`) where the installation archive is extracted and create the following directory:
       * `nexus-install-dir` (install home) = `{{nexus_root}}/nexus-{{nexus-version}}` 
       * `nexus-data-dir` = `{{nexus_root}}/sonatype-work/nexus3`
    * `nexus_install_temp` is where the installation file will be downloaded
    * `nexus_pid_dir` is where the pid file will be saved (default to `/var/run/nexus`)
  * the url will be at: `http://nexus_host:nexus_port/nexus_context_path/`
    

Variables that can be passed to this role and their default values are as follows.

## Support 

### Log

If you got any error, the log is located at 
${nexus_root}/nexus-${NEXUS_VERSION}/sonatype-work/nexus3/log/
```bash
tail -f ${nexus_root}/nexus-${NEXUS_VERSION}/sonatype-work/nexus3/log/nexus.log
```

### Test

```bash
curl -I vps748761.ovh.net:8082/nexus
```
```text
HTTP/1.1 302 Found
Date: Wed, 01 Jan 2020 15:35:25 GMT
Location: http://vps748761.ovh.net:8082/nexus/
Content-Length: 0
Server: Jetty(9.4.18.v20190429)
```

## Accessing the user interface 

Interface will be available at `http://nexus_host:nexus_port/nexus_context_path/`

Example: 
  * without Nginx: [http://vps748761.ovh.net:8082/nexus](http://vps748761.ovh.net:8082/nexus)
  * with Nginx: Use just the full qualified domain name [http://nexus.example.com](http://nexus.example.come)

Admin user:
  * Login: `admin` 
  * password: `$data-dir\admin.password`

## Next steps

  * [Backup and restore](https://help.sonatype.com/repomanager3/backup-and-restore)
  * [User Interface](https://help.sonatype.com/repomanager3/user-interface)
  * [Configuration](https://help.sonatype.com/repomanager3/configuration)
  * [Format Setup (maven, npm,..)](https://help.sonatype.com/repomanager3/formats)
  * [Security](https://help.sonatype.com/repomanager3/security)

## Documentation / Reference

Inspired by:
  * [Asymmetrik nexus role](https://github.com/Asymmetrik/ansible-roles/tree/master/nexus)
  * [Nexus installation documentation](https://help.sonatype.com/repomanager3/installation)


    



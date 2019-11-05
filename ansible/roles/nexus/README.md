# Ansible - Nexus Installation

## About 

An Ansible role to install and configure [Sonatype Nexus](http://www.sonatype.org/nexus/)

The extraction/installation process creates two sibling [directories](https://help.sonatype.com/repomanager3/installation/directories): 
  * an application directory 
  * and a data directory, sometimes called the "Sonatype Work" directory


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
      nexus_host: 0.0.0.0
      nexus_port: 8081
      nexus_ssl: False
      nexus_ssl_port: 8443
      nexus_ssl_keypass: changeit
      nexus_ssl_storepass: changeit
      nexus_download_force: no
      nexus_link: nexus-current
      # Derived 
      nexus_download_base_url: http://download.sonatype.com/nexus/3/ 
      nexus_distribution_name: nexus-{{ nexus_version }}-bundle.tar.gz
      nexus_application_directory: nexus-{{ nexus_version }}
      
```
where:
  * `nexus_version` is the version of nexus ([Full list](https://help.sonatype.com/repomanager3/download/download-archives---repository-manager-3))
  * user properties are:
    * `nexus_user` is the user name
    * `nexus_group` is the user group 
  * directories properties are:
    * `nexus_root` is the root directory (default to `/opt`) where the installation archive is extracted and create the following directory:
       * `nexus-install-dir` (install home) = `{{nexus_root}}/nexus-{{nexus-version}}` 
       * `nexus-data-dir` = `{{nexus_root}}/sonatype-work/nexus3`
    * `nexus_install_temp` is where the installation file will be downloaded
    * `nexus_pid_dir` is where the pid file will be saved (default to `/var/run/nexus`)
    

Variables that can be passed to this role and their default values are as follows.

## Support / Log

If you got any error, the log is located at 
${nexus_root}/nexus-${NEXUS_VERSION}/sonatype-work/nexus3/log/
```bash
tail -f ${nexus_root}/nexus-${NEXUS_VERSION}/sonatype-work/nexus3/log/nexus.log
```

## Accessing the user interface 

Interface will be available at `http://<server_host>:<port>`

Example: [http://vps748761.ovh.net:8081/](http://vps748761.ovh.net:8081/)

Admin user:
  * Login: `admin` 
  * password: `$data-dir\admin.password`

## Next steps

  * [User Interface](https://help.sonatype.com/repomanager3/user-interface)
  * [Configuration](https://help.sonatype.com/repomanager3/configuration)
  * [Format Setup (maven, npm,..)](https://help.sonatype.com/repomanager3/formats)
  * [Security](https://help.sonatype.com/repomanager3/security)

## Documentation / Reference

Inspired by:
  * [Asymmetrik nexus role](https://github.com/Asymmetrik/ansible-roles/tree/master/nexus)
  * [Nexus installation documentation](https://help.sonatype.com/repomanager3/installation)


    



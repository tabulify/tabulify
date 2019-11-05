# Ansible - Nexus Installation

## About 

An Ansible role to install and configure [Sonatype Nexus](http://www.sonatype.org/nexus/)

The extraction process creates two sibling [directories](https://help.sonatype.com/repomanager3/installation/directories): 
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
      nexus_home: /opt/nexus
      nexus_dest: /tmp/nexus
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
      nexus_download_base_url: http://www.sonatype.org/downloads 
      nexus_distribution_name: nexus-{{ nexus_version }}-bundle.tar.gz
      nexus_application_directory: nexus-{{ nexus_version }}
      
```
where:
  * `nexus_version` is the version of nexus ([Full list](https://help.sonatype.com/repomanager3/download/download-archives---repository-manager-3))
  * user properties are:
    * `nexus_user` is the user name
    * `nexus_group` is the user group 
  * directories properties are:
    * `nexus_home` is the installation directory (default to `/opt/nexus`)
    * `nexus_dest` is where the installation file will be downloaded
    * `nexus_pid_dir` is where the pid file will be saved (default to `/var/run/nexus`)
    
    * `nexus_shell` is the default to `/sbin/nologin`

Variables that can be passed to this role and their default values are as follows.

## Documentation / Reference

Inspired by:
  * [Asymmetrik nexus role](https://github.com/Asymmetrik/ansible-roles/tree/master/nexus)
  * [Nexus installation documentation](https://help.sonatype.com/repomanager3/installation)


    



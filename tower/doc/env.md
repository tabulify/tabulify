# Environment


## Variable

We manage environment with the files:
  * [.tower.yml](../.tower.yml) for the non-secret env
  * `.tower.secret.yml` for the secret ones

### Development
The file `.tower.secret.yml` is:
* deleted of its content after being read in production (ie in a non dev environment).
* not in the subversion
  * that's why [.tower.yml](../.tower.yml) has the whole definition
  * therefore you can put real secret when developing

### Production Deployment

For [deployment](deployment.md), the file [.tower.yml](../.tower.yml) is split in two in production.

Check Ansible:
* [.tower.yml](../../ansible/roles/apps/templates/.tower.yml)
* [.tower.secret.yml](../../ansible/roles/apps/templates/.tower.secret.yml)

If you add environment, they should also come in these files and be deployed
by the machine admin.

## Priorities
The environment are managed via [the config retrieve](../src/main/java/net/bytle/tower/util/TowerConfigRetriever.java)

The priorities are in ascendant order:
  * the os environment
  * the yaml File
  * the yaml Secret File
  * the java sys properties
Meaning that the yaml Secret File has a higher priority than the yaml file.

## Dev vs Production

A dev boolean is available in the [EnvUtil class](../src/main/java/net/bytle/tower/util/Env.java).

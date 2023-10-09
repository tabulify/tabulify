REM run on VPS

ansible-playbook playbook-root.yml -i inventories/cana.yml --vault-id passphrase.sh --extra-vars "ansible_user=fedora ansible_password=RdWGZHTwTmCx" --tags os

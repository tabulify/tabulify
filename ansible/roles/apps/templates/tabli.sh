#!/usr/bin/env bash

# shellcheck disable=SC1083
java -jar {{ tabli_shadow_path }}/tabli.jar --env production "$@"
RESULT=$?
if [ ${RESULT} -ne 0 ];
then
    echo -e "\nTabli has failed."
else
    echo -e "\nTabli has succeeded."
fi

exit $RESULT;

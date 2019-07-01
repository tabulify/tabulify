#!/usr/bin/env bash

SCRIPT_PATH=$( cd $(dirname $0) ; pwd -P )

# The cygpath transformation is needed if we are on Windows
if [ -e "/usr/bin/cygpath" ]
then
  LIB_PATH="$(cygpath -w ${SCRIPT_PATH}/lib/)*"
else
  LIB_PATH="$SCRIPT_PATH/lib/*"
fi

# The java property BCLI_APP_HOME pass the app home dir

# ${1+"$@"} is mandatory if we want to preserve the quotation of the arguments
# otherwise every space will split an argument in two
# See https://stackoverflow.com/questions/743454/space-in-java-command-line-arguments
${SCRIPT_PATH}/jre/bin/java -classpath "$LIB_PATH" -DBCLI_APP_HOME=${SCRIPT_PATH} ${Main.Class} ${1+"$@"}

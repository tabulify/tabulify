#!/bin/sh

# Parse query string to get the 'name' parameter
QUERY_STRING=${QUERY_STRING:-$(echo "$REQUEST_URI" | sed -n 's/.*\?\(.*\)/\1/p')}
NAME=$(echo "$QUERY_STRING" | sed -n 's/.*name=\([^&]*\).*/\1/p')

if [ -z "$NAME" ]; then
  echo "Status: 400 Bad Request"
  echo "Content-type: text/plain"
  echo
  echo "Error: No environment variable name specified"
  echo "Usage: ?name=VARIABLE_NAME"
  exit 0
fi

# Get the value of the environment variable with the name specified
VALUE=$(eval echo \$"$NAME")

# Check if the environment variable exists
if [ -z "$VALUE" ] && [ ! -n "$(env | grep "^$NAME=")" ]; then
  # Return 404 Not Found if the environment variable doesn't exist
  echo "Status: 404 Not Found"
  echo "Content-type: text/plain"
  echo
  echo "Error: Environment variable '$NAME' not found"
  exit 0
fi

# Return the value if the environment variable exists
echo "Content-type: text/plain"
echo
echo "$VALUE"

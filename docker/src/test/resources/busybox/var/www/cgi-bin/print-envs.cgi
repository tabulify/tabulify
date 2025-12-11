#!/bin/sh

echo "Content-type: text/html"
echo ""

echo "<html>"
echo "<head>"
echo "<title>Apache Environment Variables</title>"
echo "<style>"
echo "body { font-family: Arial, sans-serif; margin: 20px; }"
echo "h1 { color: #333; }"
echo "table { border-collapse: collapse; max-width: 600px; width: 100%; table-layout: fixed; margin: 0 auto; }"
echo "th, td { border: 1px solid #ddd; padding: 8px; text-align: left; word-wrap: break-word; overflow-wrap: break-word; max-width: 300px; white-space: normal; }"
echo "tr:nth-child(even) { background-color: #f2f2f2; }"
echo "th { background-color: #4CAF50; color: white; }"
echo "</style>"
echo "</head>"
echo "<body>"
echo "<h1>Apache Environment Variables</h1>"
echo "<table>"
echo "<tr><th>Variable</th><th>Value</th></tr>"

# Sort the environment variables for better readability
env | sort | while read line; do
  var=$(echo "$line" | cut -d= -f1)
  val=$(echo "$line" | cut -d= -f2-)
  if [ "$var" = "HTTP_COOKIE" ]; then
    continue
  fi
  echo "<tr><td>$var</td><td>$val</td></tr>"
done
# or env

echo "</table>"
echo "</body>"
echo "</html>"

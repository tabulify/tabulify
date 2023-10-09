SELECT count(*)
from Log
WHERE logtype = 'nginx'
  and response != '200' and response NOT LIKE '3%' FACET request, response, hostname, agent  SINCE 1 day ago

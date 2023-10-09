SELECT
  CAST((xpath('/api/query/pages/page/@title', xml))[1] AS text) AS title,
  CAST((xpath('/api/query/pages/page/@description', xml))[1] AS text) AS description
FROM wikipedia;

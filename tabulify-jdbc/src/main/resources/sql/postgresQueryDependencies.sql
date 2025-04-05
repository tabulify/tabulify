-- retrieve the dependencies of query
SELECT
distinct
dependency.relname as dependency_table,
dependency_ns.nspname as dependency_schema
FROM pg_depend
JOIN pg_rewrite ON pg_depend.objid = pg_rewrite.oid
JOIN pg_class as relation ON pg_rewrite.ev_class = relation.oid
JOIN pg_class as dependency ON pg_depend.refobjid = dependency.oid
JOIN pg_namespace relation_ns ON relation_ns.oid = relation.relnamespace
JOIN pg_namespace dependency_ns ON dependency_ns.oid = dependency.relnamespace
where
deptype = 'n'
and refobjsubid <> 0 -- not on object level otherwise you get the query itself
and relation.relname = ?
and relation_ns.nspname = ?


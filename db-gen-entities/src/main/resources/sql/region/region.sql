
select
	distinct
	region,
	"region-code" as code
from
	country
where
	"region-code" is not null
order by region

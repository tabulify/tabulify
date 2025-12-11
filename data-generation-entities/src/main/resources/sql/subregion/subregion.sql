
select
	distinct
	"sub-region" as subregion,
	"sub-region-code" as code
from
	country
where
	"sub-region-code" is not null
order by
  "sub-region"

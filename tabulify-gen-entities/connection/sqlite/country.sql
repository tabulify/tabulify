select
	name as country,
	"alpha-2" as code2,
	"alpha-3" as code3,
	"country-code" as coden,
	"region-code" as region_code,
	"sub-region-code" as subregion_code
from
	country
order by name

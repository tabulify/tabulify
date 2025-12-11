select firstname,
gender,
(total*1.0 / (sum(total) over ()))* 100 as probabilty
from(
select
	Boy_name as "firstname",
	'M' as Gender,
	Boy_number as total
from
	(
	select
		*
	from
		baby_uk_scotland
	where
		boy_name <> '' and boy_name <> 'A') boy
union all
select
	Girl_name,
	'F' as gender,
	Girl_number as total
from
	(
	select
		*
	from
		baby_uk_scotland
	where
		girl_name <> ''
		) girl
) babies
order by firstname

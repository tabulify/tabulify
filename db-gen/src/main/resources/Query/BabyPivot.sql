select
	'Male' as "Gender",
	Boy_name as "Name",
	(Boy_number*1.0 / (sum(Boy_number) over ()))* 100 as Probability
from
	(
	select
		*
	from
		baby
	where
		boy_name <> "" )
union all
select
	'Female' as gender,
	Girl_name,
	(Girl_number*1.0 / (sum(Girl_number) over ()))* 100 as probabilty
from
	(
	select
		*
	from
		baby
	where
		girl_name <> "" )

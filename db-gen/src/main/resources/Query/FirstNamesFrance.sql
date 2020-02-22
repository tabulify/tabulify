select
	sexe as gender,
	prenom as firstname,
	frequence*1.0 / sum(frequence) over () as probability
FROM
	(
	select
		CASE sexe WHEN 1 THEN "Male"
		ELSE "Female"
END as sexe,
	substr(premier_prenom,
	1,
	1) || lower(substr(premier_prenom, 2)) as prenom,
	sum(frequence) as frequence
from
	baby_fr bf
where
	premier_prenom not like '_prenoms_rares%'
group by
	1,
	2
order by
	3 desc
)

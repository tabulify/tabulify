select
	prenom as firstname,
	sexe as gender,
	frequence*1.0 / sum(frequence) over () as probability
FROM
	(
	select
		CASE sexe WHEN 1 THEN 'm'
		ELSE 'f'
END as sexe,
	substr(premier_prenom,
	1,
	1) || lower(substr(premier_prenom, 2)) as prenom,
	sum(frequence) as frequence
from
	baby_fr bf
where
	premier_prenom not like '_prenoms_rares%'
	and premier_prenom != 'A'
group by
	1,
	2
order by
	2 asc
) sub

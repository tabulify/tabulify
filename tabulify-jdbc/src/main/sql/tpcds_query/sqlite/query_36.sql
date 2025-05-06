SELECT
	SUM( ss_net_profit )/ SUM( ss_ext_sales_price ) AS gross_margin ,
	i_category ,
	i_class
	--,grouping(i_category)+grouping(i_class) as lochierarchy
,
	i_category + i_class AS lochierarchy ,
	RANK() OVER (
		--partition by grouping(i_category)+grouping(i_class),
 PARTITION BY i_category + i_class,
		--case when grouping(i_class) = 0 then i_category end 
 CASE
			WHEN i_class = 0 THEN i_category
		END
	ORDER BY
		SUM( ss_net_profit )/ SUM( ss_ext_sales_price ) ASC
	) AS rank_within_parent
FROM
	store_sales ,
	date_dim d1 ,
	item ,
	store
WHERE
	d1.d_year = 2000
	AND d1.d_date_sk = ss_sold_date_sk
	AND i_item_sk = ss_item_sk
	AND s_store_sk = ss_store_sk
	AND s_state IN (
		'TN',
		'TN',
		'TN',
		'TN',
		'TN',
		'TN',
		'TN',
		'TN'
	)
GROUP BY
	--ROLLUP(
		i_category,
		i_class
	--)
ORDER BY
	lochierarchy DESC ,
	CASE
		WHEN lochierarchy = 0 THEN i_category
	END ,
	rank_within_parent
LIMIT 100;

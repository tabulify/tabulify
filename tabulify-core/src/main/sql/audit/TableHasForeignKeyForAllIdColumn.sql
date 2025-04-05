-- Om te testen als alle ID kolom met uitzondering van de primary key een foreign Key hebben

SELECT
  CASE
    WHEN column_name_with_id.column_name =
      column_name_with_foreign_key.column_name
    THEN 'Goed'
    ELSE 'Niet Goed'
  END status,
  column_name_with_id.column_name "Kolom met Id",
  column_name_with_foreign_key.column_name "Kolom met Foreign Key"
FROM
  (SELECT
    column_name
  FROM
    User_Tab_Columns
  WHERE
    table_name = upper('&1')
  AND column_name LIKE '%ID%'
  AND column_name NOT LIKE 'META%'
  AND column_name NOT IN
    (SELECT
      column_name
    FROM
      user_cons_columns cons_column,
      user_constraints const
    WHERE
      cons_column.table_name  = upper( '&1' )
    AND const.constraint_name = cons_column.constraint_name
    AND const.constraint_type = 'P' -- Primary Key)
    )
  ) column_name_with_id
FULL OUTER JOIN
  (SELECT
    column_name
  FROM
    user_cons_columns cons_column,
    user_constraints const
  WHERE
    cons_column.table_name  = upper( '&1' )
  AND const.constraint_name = cons_column.constraint_name
  AND const.constraint_type = 'R' -- Foreign Key
  ) column_name_with_foreign_key
ON
  column_name_with_id.column_name = column_name_with_foreign_key.column_name;

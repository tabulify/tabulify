REM TO CHECK IF a column IS UNIQUE
REM Syntax:
REM @TableHasAnUniqueColumn Table Column
REM Example:
REM @TableHasAnUniqueColumn Mp_Fact_Verkoop_Orders corrd_id

Select
case when Aantal_Record_With_Double <> 0 then
  'Error: &2 is niet uniek ('
  || Aantal_Record_With_Double
  || ') in the fact table &1 ' 
  else
  'Goed: &2 is unique in the table &1'
  end AS MESSAGE
FROM
  (
    SELECT
      count(*) AS aantal_record_with_double
    FROM
      (
        SELECT
          COUNT(*) AS aantal
        FROM
          &1
        WHERE
          &2 NOT IN (-1, -2)
        HAVING
          COUNT(*) > 1
        GROUP BY
          &2
      )
  );

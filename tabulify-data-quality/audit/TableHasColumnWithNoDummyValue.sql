REM To check if a column has no dummy value

REM Syntax:
REM @TableHasNoDummyValue Table Column

REM Example:
REM @TableHasNoDummyValue Mp_Fact_Verkoop_Orders corrd_id

SELECT
  CASE
    WHEN aantal <> 0
    THEN 'Error: De feit tabel &1 heeft ' ||
      aantal                              ||
      ' dummy record ('                   ||
      dummy_record                        ||
      ') in de kolom &2'
    ELSE 'Goed: De feit tabel &1 heeft geen ' || dummy_record || ' dummy record in de kolom &2'
  END AS MESSAGE
FROM
  (
    SELECT
      dummy_view.dummy_record AS dummy_record
    ,
      aantal_view.aantal AS aantal
    FROM
      (
        SELECT
          -1 AS dummy_record
        FROM
          DUAL
        UNION
        SELECT
          -2 AS dummy_record
        FROM
          DUAL
      )
      dummy_view
    ,
      (
        SELECT
          &2 AS dummy_record
        ,
          COUNT(*) AS aantal
        FROM
          &1
        WHERE
          &2 IN (-1, -2)
        GROUP BY
          &2
      )
      aantal_view
    WHERE
      dummy_view.dummy_record = aantal_view.dummy_record (+)
  ); 

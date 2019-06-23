REM To check if a table is not empty

REM Syntax:
REM @TableIsNotEmpty Table

REM Example:
REM @TableIsNotEmpty Mp_Fact_Verkoop_Orders 

Select
case when aantal <> 0 then
  'Goed: The table &1 is niet leeg' 
  else
  'Fout: The table &1 is empty' 
  end AS MESSAGE
FROM
  (
    SELECT
      COUNT(*) AS Aantal
    FROM
      (
        SELECT * FROM &1
      )
  )
;
  

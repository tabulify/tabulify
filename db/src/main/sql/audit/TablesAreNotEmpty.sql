-- Author: Nicolas GERARD
-- Email:  gerard@hotitem.nl
-- Verify that table are not empty for the current user
-- Syntax:
-- @TablesAreNotEmpty

DECLARE
  Vc_TableList sys_refcursor;
  Vs_SqlStatement  VARCHAR2(600) := NULL;
  Vs_TableName     VARCHAR2(30) := NULL;
  Vn_RowCountTable NUMBER       := NULL;
BEGIN
  -- To avoid: ORA-20000: ORU-10027: buffer overflow, limit of 20000 bytes
  Dbms_Output.Enable(100000);
  Vs_Sqlstatement :=
  'select table_name from user_tables order by table_name asc';
  OPEN Vc_TableList FOR vs_sqlstatement;
  LOOP
    
    FETCH
      Vc_TableList
    INTO
      Vs_TableName;
    EXIT
  WHEN Vc_TableList%notfound;
    vs_sqlstatement := 'select count(*) from ' || Vs_TableName;
    EXECUTE Immediate Vs_Sqlstatement INTO Vn_RowCountTable;
    
    IF (Vn_RowCountTable = 0) THEN
      Dbms_Output.Put_Line('!Error!: The Table ' || Rpad(Vs_TableName,30, ' ') ||
      ' has no rows !');
    
    ELSE
      Dbms_Output.Put_Line('Goed   : The Table ' || Rpad(Vs_TableName,30, ' ') ||
      ' has '                                 || Vn_RowCountTable || ' rows.')
      ;
    
    END IF;
  
  END LOOP;
  CLOSE Vc_TableList;

END;
/

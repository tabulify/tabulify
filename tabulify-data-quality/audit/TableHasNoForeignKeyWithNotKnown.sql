-- Author: Nicolas GERARD
-- Email:  gerard@hotitem.nl
-- Verify that the foreign key column of a table are no Not Know record
-- Syntax:
-- @TableHasNoForeignKeyFullOfNotKnown Table
-- Example:
-- @TableHasNoForeignKeyFullOfNotKnown mp_Fact_Verkoop_Orders
DECLARE
  vc_columnlist sys_refcursor;
  vc_resultlist sys_refcursor;
  Vs_SqlStatement  VARCHAR2(600);
  vs_columnname    VARCHAR2(30) := NULL;
  vs_dummyvalue    VARCHAR2(30) := NULL;
  vn_rowcounttable NUMBER       := NULL;
  Vn_Countdummy    NUMBER       := NULL;
  Vs_TableName     VARCHAR2(30) := '&1';
BEGIN
  -- To avoid: ORA-20000: ORU-10027: buffer overflow, limit of 20000 bytes
  Dbms_Output.Enable(100000);
  vs_sqlstatement := 'select count(*) from ' || Vs_TableName;
  EXECUTE Immediate Vs_Sqlstatement INTO Vn_Rowcounttable;
  Vs_Sqlstatement := 'select column_name '                     || ' from ' ||
  ' user_cons_columns cons_column, '                           || ' user_constraints const ' ||
  ' where cons_column.table_name  = upper('''                  || Vs_TableName || ''') ' ||
  ' and const.constraint_name =  cons_column.constraint_name ' ||
  ' and const.constraint_type = ''R'' -- Foreign Key';
  OPEN vc_columnlist FOR vs_sqlstatement;
  LOOP
    FETCH
      Vc_Columnlist
    INTO
      Vs_Columnname;
    EXIT
  WHEN vc_columnlist%notfound;
    IF (vs_columnname LIKE 'DATMD_ID%') THEN
      vs_dummyvalue := '-2';
    ELSE
      vs_dummyvalue := '-1';
    END IF;
    -- SQL made to return a value even if the query return NULL for the count
    vs_sqlstatement := 'select '''        || vs_dummyvalue ||
    ''' as dummy, (select count(*) from ' || Vs_TableName || ' where ' ||
    vs_columnname                         || '  in (' || vs_dummyvalue ||
    ') group by '                         || vs_columnname ||
    ') as count_dummy from dual';
    OPEN vc_resultlist FOR vs_sqlstatement;
    LOOP
      FETCH
        vc_resultlist
      INTO
        vs_dummyvalue
      ,
        vn_countdummy;
      EXIT
    WHEN Vc_Resultlist%Notfound;
      IF (Vn_Countdummy != 0) THEN
        Dbms_Output.Put_Line('Error: The column '               || Vs_TableName || '.' || Rpad
        (Vs_Columnname,20, ' ')                                 || ' has ' || lpad( TO_CHAR(
        Vn_Countdummy),9,' ')                                   || ' of ' || Vs_Dummyvalue ||
        ' for a total of '                                      || Vn_Rowcounttable || ' rows'
                                                                || ' (A Percentage of ' ||
        lpad( ROUND( Vn_Countdummy/Vn_Rowcounttable*100),2,' ') || ').' );
      ELSE
        Dbms_Output.Put_Line('Goed: The column ' || Vs_TableName || '.' || Rpad
        (Vs_Columnname,20,' ')                   || ' has no ' || Vs_Dummyvalue
                                                 || ' for a total of ' ||
        Vn_Rowcounttable                         || ' rows.');
      END IF;
    END LOOP;
    CLOSE vc_resultlist;
  END LOOP;
  CLOSE vc_columnlist;
END ;
/

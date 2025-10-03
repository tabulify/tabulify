-- Verify that the columns with ID of all fact table from the current user have
-- a foreign key
-- Syntax:
-- @TablesHaveForeignKeyForAllIdColumn

DECLARE
  Vc_TableList sys_refcursor;
  Vs_TableName user_tables.table_name%TYPE := NULL;
  Vc_ColumnList sys_refcursor;
  Vs_ColumnName User_Tab_Columns.column_name%TYPE := NULL;
  Vs_SqlStatement VARCHAR2(600)                   := NULL;
  Vn_RowCount     NUMBER                          := NULL;
BEGIN
  -- To avoid: ORA-20000: ORU-10027: buffer overflow, limit of 20000 bytes
  Dbms_Output.Enable(100000);
  Vs_Sqlstatement :=
  'select table_name from user_tables where table_name like ''%FACT%'' order by table_name asc'
  ;
  OPEN Vc_TableList FOR vs_sqlstatement;
  LOOP

FETCH
      Vc_TableList
    INTO
      Vs_TableName;
    EXIT
  WHEN Vc_TableList%notfound;
    -- Get the ID Columns, not metadata and not a primary key
    Vs_Sqlstatement :=
    'SELECT column_name FROM User_Tab_Columns WHERE table_name = ''' ||
    Vs_TableName                                                     ||
    ''' AND column_name LIKE ''%ID%'''                               ||
    ' AND column_name NOT LIKE ''META%'' AND column_name NOT IN '    ||
    ' (SELECT column_name FROM user_cons_columns cons_column, user_constraints const WHERE cons_column.table_name  = '''
    || Vs_TableName ||
    ''' AND const.constraint_name = cons_column.constraint_name AND const.constraint_type = ''P'' ) order by column_name asc'
    ; -- Filter of P Primary Key)
    OPEN Vc_ColumnList FOR vs_sqlstatement;
    LOOP

FETCH
        Vc_ColumnList
      INTO
        Vs_ColumnName;
      EXIT
    WHEN Vc_ColumnList%notfound;
      -- Has the column a foreign Key ?
      Vs_Sqlstatement :=
      'SELECT count(*) FROM user_cons_columns cons_column, user_constraints const'
                                                                || ' WHERE cons_column.table_name  = ''' || Vs_TableName ||
      ''' AND cons_column.column_name = '''                     || Vs_ColumnName ||
      ''' AND const.constraint_name = cons_column.constraint_name ' ||
      ' AND const.constraint_type = ''R'' -- Foreign Key ';
      EXECUTE Immediate Vs_Sqlstatement INTO Vn_RowCount;

IF (Vn_RowCount IS NULL) THEN
        Dbms_Output.Put_Line('!Error!: The column ' || Rpad(Vs_TableName || '.'
                                                    || Vs_ColumnName,60, ' ')
                                                    || ' has no foreign key !')
        ;

ELSE
        Dbms_Output.Put_Line('Goed   : The column ' || Rpad(Vs_TableName || '.'
                                                    || Vs_ColumnName,60, ' ')
                                                    || ' has ' || Vn_RowCount
                                                    || ' foreign key');

END IF;

END LOOP;

END LOOP;

END;
/

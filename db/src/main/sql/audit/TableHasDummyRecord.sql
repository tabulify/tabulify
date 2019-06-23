REM om te controleren als een dimensie tabel heeft
REM -1 en -2 en dat hun omschrijving correct is
--
REM Syntax:
REM @TableHasDummyRecord Table Id_Column Key_Column
--
REM Example:
REM @TableNoDummyValue Mp_Dim_Tabel
--
-- Author: Nicolas GERARD
-- Email:  gerardnico@gmail.com
--
-- Verify that the foreign key column of a table are not full of dummy record (
-- more than 90%)
--
-- Syntax:
--  @TableHasNotForeignKeyFullOfDummy Table
--
-- Example:
-- @TableHasNotForeignKeyFullOfDummy mp_Fact_Verkoop_Orders
--
DECLARE
  vc_columnlist sys_refcursor;
  vc_resultlist sys_refcursor;
  Vs_SqlStatement    VARCHAR2(4000);
  vs_columnname      VARCHAR2(30)  := NULL;
  vs_table           VARCHAR2(30)  := NULL;
  vs_owner           VARCHAR2(30)  := NULL;
  vs_constraint_type CHAR(1)       := NULL;
  vs_primary_key     VARCHAR2(30)  := NULL;
  vs_message         VARCHAR2(300) := NULL;
type UniqueKeyList
IS
  TABLE OF VARCHAR2(30);
  vl_unique_key uniquekeylist;
  vs_dummyvalue    VARCHAR2(30) := NULL;
  vn_rowcounttable NUMBER       := NULL;
  Vn_Countdummy    NUMBER       := NULL;
  i                INTEGER      := NULL;
  vn_Thresold_full NUMBER       := 90; -- Above this thresold an error message
  -- is given.
BEGIN
  -- To avoid: ORA-20000: ORU-10027: buffer overflow, limit of 20000 bytes
  Dbms_Output.Enable(100000);
  -- Initialization values
  vs_table := '&1';
  vs_owner := '&2';
  -- Get the Primary and unique constraint
  vs_sqlstatement :=
  'select cons_column.column_name, const.constraint_type FROM ' ||
  'all_cons_columns cons_column, all_constraints const where '  ||
  'cons_column.table_name = upper('''                           || vs_table ||
  ''') '                                                        ||
  'and cons_column.owner = upper('''                            || vs_owner ||
  ''') '                                                        ||
  'and const.constraint_name = cons_column.constraint_name '    ||
  'and const.constraint_type in (''P'',''U'') -- Primary and Unique';
  -- Initialize the primary key in its variable and the unique key in the
  -- UniqueKey List
  vl_unique_key := UniqueKeyList();
  i             := 1;
  OPEN vc_columnlist FOR vs_sqlstatement;
  LOOP
    FETCH
      Vc_Columnlist
    INTO
      vs_columnname
    ,
      vs_constraint_type;
    EXIT
  WHEN vc_columnlist%notfound;
    CASE vs_constraint_type
    WHEN 'P' THEN
      vs_primary_key := vs_columnname;
    WHEN 'U' THEN
      BEGIN
        vl_unique_key.extend(1);
        vl_unique_key(i):= vs_columnname;
        i               := i+1;
      END;
    END CASE;
  END LOOP;
  dbms_output.put_line('Primary Key: ' || vs_primary_key);
  IF vl_unique_key.EXISTS(1) = TRUE THEN
    FOR i                   IN vl_unique_key.FIRST..vl_unique_key.LAST
    LOOP
      dbms_output.put_line('Uniek Key(' || i || '): ' || vl_unique_key(i));
    END LOOP;
  ELSE
    dbms_output.put_line('Error: Geen Uniek Key voor de tabel ' || vs_table);
  END IF;
  -- Creation of the test SQL. The work !
  vs_sqlstatement := 'SELECT ' || 'CASE ' || ' WHEN dummy_id_dim is null ' ||
  ' THEN ''Error: De tabel '   || vs_table ||
  ' heeft geen '' || dummy_id || '' dummy record'' ';
  IF vl_unique_key.EXISTS(1) = TRUE THEN
    FOR i                   IN vl_unique_key.FIRST..vl_unique_key.LAST
    LOOP
      vs_sqlstatement := vs_sqlstatement               ||
      ' WHEN dummy_id_dim = -1 and dummy_key_'         || substr(vl_unique_key(i),0,20) ||
      '  not in (''Onbekend'', ''-1'', ''9999/12/31'')  '                              ||
      ' THEN ''Error: De dummy record -1 in de tabel ' || vs_table ||
      ' heeft niet voor de key '                       || vl_unique_key(i) ||
      ' de waarde "Onbekend" maar '' || dummy_key_'    || substr(vl_unique_key(i),0,20) ||
      ' WHEN dummy_id_dim = -2 and dummy_key_'         || substr(vl_unique_key(i),0,20) ||
      ' not in (''Leeg'', ''-2'', ''9999/12/31'') '                                  ||
      ' THEN ''Error: De dummy record -2 in de tabel ' || vs_table ||
      ' heeft niet voor de key '                       || vl_unique_key(i) ||
      ' de waarde "Leeg" maar '' || dummy_key_'        || substr(vl_unique_key(i),0,20);
    END LOOP;
  END IF;
  vs_sqlstatement := vs_sqlstatement                                      || ' ELSE ''Goed: De tabel ' || vs_table
                                                                          ||
  '  heeft geen problem met de dummy record '' || dummy_id_dim || ''.'' ' ||
  ' END AS "CONTROLE VAN DE DUMMY RECORD"'                                ||
  ' FROM '                                                                ||
  ' ( '                                                                   ||
  '  SELECT '                                                             ||
  '    dummy_view.dummy_id AS  dummy_id, '                                ||
  '    dummy_view.dummy_key AS dummy_key, '                               ||
  '    dim_view.dummy_id AS dummy_id_dim ';
  IF vl_unique_key.EXISTS(1) = TRUE THEN
  vs_sqlstatement := vs_sqlstatement || ',';
    FOR i                   IN vl_unique_key.FIRST..vl_unique_key.LAST
    LOOP
      vs_sqlstatement := vs_sqlstatement || 'to_char(dim_view.dummy_key_' ||
      substr(vl_unique_key(i),0,20)      || ') AS dummy_key_' || substr(vl_unique_key(i),0,20);
      IF (i             != vl_unique_key.LAST) THEN
        vs_sqlstatement := vs_sqlstatement || ',';
      END IF;
    END LOOP;
  END IF;
  vs_sqlstatement := vs_sqlstatement                 || ' FROM ' || ' ( ' || 'SELECT ' ||
  '      -1 AS dummy_id, ''Onbekend'' AS dummy_key ' || '    FROM ' ||
  '      DUAL '                                      || '    UNION  ' ||
  '    SELECT '                                      ||
  '      -2 AS dummy_id, ''Leeg'' as dummy_key '     || '    FROM ' ||
  '      DUAL '                                      || '  ) ' ||
  '  dummy_view , '                                  || ' ( ' || '    SELECT '
                                                     || vs_primary_key ||
  ' AS dummy_id ';
  IF vl_unique_key.EXISTS(1) = TRUE THEN
        vs_sqlstatement := vs_sqlstatement || ',';
    FOR i                   IN vl_unique_key.FIRST..vl_unique_key.LAST
    LOOP
      vs_sqlstatement := vs_sqlstatement || vl_unique_key(i) ||
      ' AS dummy_key_'                   || substr(vl_unique_key(i),0,20);
      IF (i             != vl_unique_key.LAST) THEN
        vs_sqlstatement := vs_sqlstatement || ',';
      END IF;
    END LOOP;
  END IF;
  
  vs_sqlstatement := vs_sqlstatement               || '    FROM ' || vs_owner || '.' ||
  vs_table                                         || '    WHERE ' || vs_primary_key ||
  ' IN (-1, -2) '                                  || ') dim_view ' || ' WHERE  ' ||
  '  dummy_view.dummy_id = dim_view.dummy_id (+) ' || ')';
  
  --dbms_output.put_line(vs_sqlstatement);
  
  OPEN vc_resultlist FOR vs_sqlstatement;
  LOOP
    FETCH
      vc_resultlist
    INTO
      vs_message;
    EXIT
  WHEN vc_resultlist%notfound;
    dbms_output.put_line(vs_message);
  END LOOP;
  
END ;
/

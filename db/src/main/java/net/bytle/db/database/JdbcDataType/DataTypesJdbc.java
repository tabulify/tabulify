package net.bytle.db.database.JdbcDataType;

import net.bytle.db.database.DataTypeJdbc;

import java.math.BigDecimal;
import java.net.URL;
import java.sql.*;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by gerard on 07-01-2016.
 */
public class DataTypesJdbc {

    static final Map<String, DataTypeJdbc> dataTypeJdbcByName = new HashMap<>();
    static final Map<Integer,DataTypeJdbc> dataTypeJdbcById = new HashMap<Integer,DataTypeJdbc>();
    static final Map<Class,DataTypeJdbc> dataTypeJdbcByClass = new HashMap<Class,DataTypeJdbc>();

    static {
        dataTypeJdbcById.put(JdbcArray.TYPE_CODE, new JdbcArray() );
        dataTypeJdbcById.put(JdbcBigInt.TYPE_CODE, new JdbcBigInt() );
        dataTypeJdbcById.put(JdbcBinary.TYPE_CODE, new JdbcBinary() );
        dataTypeJdbcById.put(JdbcBit.TYPE_CODE, new JdbcBit() );
        dataTypeJdbcById.put(JdbcBlob.TYPE_CODE, new JdbcBlob() );
        dataTypeJdbcById.put(JdbcBoolean.TYPE_CODE, new JdbcBoolean() );
        dataTypeJdbcById.put(JdbcChar.TYPE_CODE, new JdbcChar() );
        dataTypeJdbcById.put(JdbcClob.TYPE_CODE, new JdbcClob() );
        dataTypeJdbcById.put(JdbcDataLinkJdbc.TYPE_CODE, new JdbcDataLinkJdbc() );
        dataTypeJdbcById.put(JdbcDate.TYPE_CODE, new JdbcDate() );
        dataTypeJdbcById.put(JdbcDecimal.TYPE_CODE, new JdbcDecimal() );
        dataTypeJdbcById.put(JdbcDistinct.TYPE_CODE, new JdbcDistinct() );
        dataTypeJdbcById.put(JdbcDouble.TYPE_CODE, new JdbcDouble() );
        dataTypeJdbcById.put(JdbcFloat.TYPE_CODE, new JdbcFloat() );
        dataTypeJdbcById.put(JdbcInteger.TYPE_CODE, new JdbcInteger() );
        dataTypeJdbcById.put(JdbcJavaObject.TYPE_CODE, new JdbcJavaObject() );
        dataTypeJdbcById.put(JdbcLongNVarchar.TYPE_CODE, new JdbcLongNVarchar() );
        dataTypeJdbcById.put(JdbcLongVarBinary.TYPE_CODE, new JdbcLongVarBinary() );
        dataTypeJdbcById.put(JdbcLongVarchar.TYPE_CODE, new JdbcLongVarchar() );
        dataTypeJdbcById.put(JdbcNChar.TYPE_CODE, new JdbcNChar() );
        dataTypeJdbcById.put(JdbcNClob.TYPE_CODE, new JdbcNClob() );
        dataTypeJdbcById.put(JdbcNull.TYPE_CODE, new JdbcNull() );
        dataTypeJdbcById.put(JdbcNumeric.TYPE_CODE, new JdbcNumeric() );
        dataTypeJdbcById.put(JdbcNVarchar.TYPE_CODE, new JdbcNVarchar() );
        dataTypeJdbcById.put(JdbcOther.TYPE_CODE, new JdbcOther() );
        dataTypeJdbcById.put(JdbcReal.TYPE_CODE, new JdbcReal() );
        dataTypeJdbcById.put(JdbcRef.TYPE_CODE, new JdbcRef() );
        dataTypeJdbcById.put(JdbcRowid.TYPE_CODE, new JdbcRowid() );
        dataTypeJdbcById.put(JdbcSmallInt.TYPE_CODE, new JdbcSmallInt() );
        dataTypeJdbcById.put(JdbcSqlXml.TYPE_CODE, new JdbcSqlXml() );
        dataTypeJdbcById.put(JdbcStruct.TYPE_CODE, new JdbcStruct() );
        dataTypeJdbcById.put(JdbcTime.TYPE_CODE, new JdbcTime() );
        dataTypeJdbcById.put(JdbcTimestamp.TYPE_CODE, new JdbcTimestamp() );
        dataTypeJdbcById.put(JdbcTinyInt.TYPE_CODE, new JdbcTinyInt() );
        dataTypeJdbcById.put(JdbcVarBinary.TYPE_CODE, new JdbcVarBinary() );
        dataTypeJdbcById.put(JdbcVarchar.TYPE_CODE, new JdbcVarchar() );

        // Mapping from Java Type to Sql Type
        // From https://docs.oracle.com/javase/6/docs/technotes/guides/jdbc/getstart/mapping.html

        // From Pure Jdbc Type to Sql Type
        dataTypeJdbcByClass.put(long.class, new JdbcBigInt() );
        dataTypeJdbcByClass.put(Boolean.class, new JdbcBit() ); // No Boolean
        dataTypeJdbcByClass.put(URL.class, new JdbcDataLinkJdbc() );
        dataTypeJdbcByClass.put(BigDecimal.class, new JdbcNumeric() ); // Of Decimal ?
        dataTypeJdbcByClass.put(Double.class, new JdbcDouble() ); // And no Float
        dataTypeJdbcByClass.put(Integer.class, new JdbcInteger() ); // And not small int, tinyInt
        dataTypeJdbcByClass.put(Object.class, new JdbcJavaObject() );
        dataTypeJdbcByClass.put(byte[].class, new JdbcLongVarBinary() ); // No Binary of varbinary
        dataTypeJdbcByClass.put(byte.class, new JdbcTinyInt() );
        dataTypeJdbcByClass.put(String.class, new JdbcLongVarchar() ); // No char of varchar, LongNVarchar, nchar, nvarchar
        dataTypeJdbcByClass.put(Float.class, new JdbcReal() );
        dataTypeJdbcByClass.put(Short.class, new JdbcSmallInt() );
        dataTypeJdbcByClass.put(Long.class, new JdbcBigInt() );

        // From Java Sql Data Type to Sql Type
        dataTypeJdbcByClass.put(Array.class, new JdbcArray() );
        dataTypeJdbcByClass.put(Ref.class, new JdbcRef() );
        dataTypeJdbcByClass.put(RowId.class, new JdbcRowid() );
        dataTypeJdbcByClass.put(SQLXML.class, new JdbcSqlXml() );
        dataTypeJdbcByClass.put(Date.class, new JdbcDate() );
        dataTypeJdbcByClass.put(Blob.class, new JdbcBlob() );
        dataTypeJdbcByClass.put(NClob.class, new JdbcNClob() );
        dataTypeJdbcByClass.put(Clob.class, new JdbcClob() );
        dataTypeJdbcByClass.put(Struct.class, new JdbcStruct() );
        dataTypeJdbcByClass.put(Time.class, new JdbcTime() );
        dataTypeJdbcByClass.put(Timestamp.class, new JdbcTimestamp() );

        dataTypeJdbcByName.put("ARRAY", new JdbcArray());
        dataTypeJdbcByName.put("BIGINT", new JdbcBigInt());
        dataTypeJdbcByName.put("BINARY", new JdbcBinary());
        dataTypeJdbcByName.put("BIT", new JdbcBit());
        dataTypeJdbcByName.put("BLOB", new JdbcBlob());
        dataTypeJdbcByName.put("BOOLEAN", new JdbcBoolean());
        dataTypeJdbcByName.put("CHAR", new JdbcChar());
        dataTypeJdbcByName.put("CLOB", new JdbcClob());
        dataTypeJdbcByName.put("DATALINK", new JdbcDataLinkJdbc());
        dataTypeJdbcByName.put("DATE", new JdbcDate());
        dataTypeJdbcByName.put("DECIMAL", new JdbcDecimal());
        dataTypeJdbcByName.put("DISTINCT", new JdbcDistinct());
        dataTypeJdbcByName.put("DOUBLE", new JdbcDouble());
        dataTypeJdbcByName.put("FLOAT", new JdbcFloat());
        dataTypeJdbcByName.put("INTEGER", new JdbcInteger());
        dataTypeJdbcByName.put("JAVA_OBJECT", new JdbcJavaObject());
        dataTypeJdbcByName.put("LONGNVARCHAR", new JdbcLongNVarchar());
        dataTypeJdbcByName.put("LONGVARBINARY", new JdbcLongVarBinary());
        dataTypeJdbcByName.put("LONGVARCHAR", new JdbcLongVarchar());
        dataTypeJdbcByName.put("NCHAR", new JdbcNChar());
        dataTypeJdbcByName.put("NCLOB", new JdbcNClob());
        dataTypeJdbcByName.put("NULL", new JdbcNull());
        dataTypeJdbcByName.put("NUMERIC", new JdbcNumeric());
        dataTypeJdbcByName.put("NVARCHAR", new JdbcNVarchar());
        dataTypeJdbcByName.put("OTHER", new JdbcOther());
        dataTypeJdbcByName.put("REAL", new JdbcReal());
        dataTypeJdbcByName.put("REF", new JdbcRef());
        dataTypeJdbcByName.put("ROWID", new JdbcRowid());
        dataTypeJdbcByName.put("SMALLINT", new JdbcSmallInt());
        dataTypeJdbcByName.put("SQLXML", new JdbcSqlXml());
        dataTypeJdbcByName.put("STRUCT", new JdbcStruct());
        dataTypeJdbcByName.put("TIME", new JdbcTime());
        dataTypeJdbcByName.put("TIMESTAMP", new JdbcTimestamp());
        dataTypeJdbcByName.put("TINYINT", new JdbcTinyInt());
        dataTypeJdbcByName.put("VARBINARY", new JdbcVarBinary());
        dataTypeJdbcByName.put("VARCHAR", new JdbcVarchar());

    }

    static public DataTypeJdbc of(Integer typeCode){
        return dataTypeJdbcById.get(typeCode);
    }

    static public DataTypeJdbc of(String typeName) {

        return dataTypeJdbcByName.get(typeName.toUpperCase());
    }

    public static DataTypeJdbc ofClass(Class clazz) {
        return dataTypeJdbcByClass.get(clazz);
    }


    /**
     * Print JDBC data type given by the database wrapper
     *
     */
    public static void printJdbcDataType()  {


        // Headers
        System.out.println("Data Type\t" +
                "Type Name\t"
        );

        // Data Type Info
        Collection<DataTypeJdbc> dataTypeJdbc = dataTypeJdbcById.values();

        for (DataTypeJdbc typeInfo : dataTypeJdbc) {
            System.out.println(
                    typeInfo.getTypeCode() + "\t" +
                            typeInfo.getTypeName() + "\t"

            );

        }


    }

}

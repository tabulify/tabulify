/**
 * Created by gerard on 31-10-2016.
 *
 * GrabConfig is needed in order to load the JDBC driver
 */
@GrabConfig(systemClassLoader=true)
@Grab(group = 'com.oracle', module = 'ojdbc', version = '6')
@Grab(group = 'net.bytle', module = 'bytle-tabular', version = '1.0.0-SNAPSHOT')

import net.bytle.table.TableLoader.QueryDataLoader
import net.bytle.table.TableLoader.TableLoaderOptions
import net.bytle.db.database.Database

println "Source Database: "+args[0];
println "Target Database: "+args[1];

Database sourceDatabase = new Database.Builder()
        .url(args[0])
        .driver("oracle.jdbc.OracleDriver")
        .build();

Database targetDatabase = new Database.Builder()
        .url(args[1])
        .driver("oracle.jdbc.OracleDriver")
        .build();

try {

    String sql = "SELECT\n" +
            "        *\n" +
            "    FROM\n" +
            "        OWBSYS.ALL_RT_AUDIT_MAP_RUNS map_run\n" +
            "    WHERE\n" +
            "        start_time > sysdate - 60";
    QueryDataLoader queryTableLoader = new QueryDataLoader(
            sourceDatabase,
            sql,
            targetDatabase
    ).tableName("THPOWB_AUDIT_MAP_RUNS")
    .loadOptions(TableLoaderOptions.DROP_TABLE);

    queryTableLoader.load();

} finally {

    targetDatabase.close();
    sourceDatabase.close();

}
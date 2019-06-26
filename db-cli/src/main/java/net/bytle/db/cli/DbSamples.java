package net.bytle.db.cli;

import net.bytle.db.DbLoggers;
import net.bytle.db.model.TableDef;
import net.bytle.db.sample.BytleSchema;
import net.bytle.db.tpc.TpcdsModel;

import java.util.Arrays;
import java.util.List;
import java.util.logging.Logger;

import static net.bytle.db.tpc.TpcdsModel.TPCDS_SCHEMA_DWH;
import static net.bytle.db.tpc.TpcdsModel.TPCDS_SCHEMA_STG;
import static net.bytle.db.tpc.TpcdsModel.TPCDS_SCHEMA_STORE_SALES;

public class DbSamples {

    public static final Logger LOGGER = DbLoggers.LOGGER_DB_SAMPLE;


    public static List<TableDef> getTables(String sample) {

        switch (sample) {
            case TpcdsModel.TPCDS_SCHEMA:
                return TpcdsModel.get().getSchemaTables(TpcdsModel.TPCDS_SCHEMA);

            case TPCDS_SCHEMA_DWH:
                return TpcdsModel.get().getSchemaTables(TPCDS_SCHEMA_DWH);

            case TPCDS_SCHEMA_STG:
                return TpcdsModel.get().getSchemaTables(TPCDS_SCHEMA_STG);

            case TPCDS_SCHEMA_STORE_SALES:
                return TpcdsModel.get().getSchemaTables(TPCDS_SCHEMA_STORE_SALES);

            case BytleSchema.SCHEMA_NAME:
                return BytleSchema.get().getTables();

            default:
                throw new RuntimeException("The sample schema (" + sample + ") is unknown");
        }

    }

    static public List<String> getNames() {
        return Arrays.asList(
                TpcdsModel.TPCDS_SCHEMA,
                TPCDS_SCHEMA_DWH,
                TPCDS_SCHEMA_STG,
                TPCDS_SCHEMA_STORE_SALES,
                BytleSchema.SCHEMA_NAME
        );
    }
}

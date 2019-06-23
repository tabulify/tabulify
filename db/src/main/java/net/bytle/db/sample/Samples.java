package net.bytle.db.sample;

import net.bytle.db.DbLoggers;

import java.util.Arrays;
import java.util.List;
import java.util.logging.Logger;

public class Samples {

    public static final Logger LOGGER = DbLoggers.LOGGER_DB_SAMPLE;


    public static SchemaSample get(String sample) {

        switch (sample) {
            case TpcdsModel.TPCDS_SCHEMA:
                return TpcdsModel.get(TpcdsModel.TPCDS_SCHEMA);

            case TpcdsModel.TPCDS_SCHEMA_DWH:
                return TpcdsModel.get(TpcdsModel.TPCDS_SCHEMA_DWH);

            case TpcdsModel.TPCDS_SCHEMA_STG:
                return TpcdsModel.get(TpcdsModel.TPCDS_SCHEMA_STG);

            case TpcdsModel.TPCDS_SCHEMA_STORE_SALES:
                return TpcdsModel.get(TpcdsModel.TPCDS_SCHEMA_STORE_SALES);

            case BytleSchema.SCHEMA_NAME:
                return BytleSchema.get();

            default:
                throw new RuntimeException("The sample schema (" + sample + ") is unknown");
        }

    }

    static public List<String> getNames() {
        return Arrays.asList(
                TpcdsModel.TPCDS_SCHEMA,
                TpcdsModel.TPCDS_SCHEMA_DWH,
                TpcdsModel.TPCDS_SCHEMA_STG,
                TpcdsModel.TPCDS_SCHEMA_STORE_SALES,
                BytleSchema.SCHEMA_NAME
        );
    }
}

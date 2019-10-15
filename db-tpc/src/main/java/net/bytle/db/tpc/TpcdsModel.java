package net.bytle.db.tpc;

import net.bytle.db.database.Database;
import net.bytle.db.database.Databases;
import net.bytle.db.engine.Tables;
import net.bytle.db.model.SchemaDef;
import net.bytle.db.model.TableDef;
import net.bytle.db.sample.SchemaSample;
import net.bytle.db.spi.Tabulars;

import java.sql.Types;
import java.util.*;
import java.util.stream.Collectors;


public class TpcdsModel implements SchemaSample {

    public static final String TPCDS_SCHEMA = "tpcds";
    public static final String TPCDS_SCHEMA_DWH = "tpcds-dwh";
    public static final String TPCDS_SCHEMA_STG = "tpcds-stg";
    public static final String TPCDS_SCHEMA_STORE_SALES = "tpcds-store-sales";

    /**
     * Dwh Table Name
     */
    public static final String DBGEN_VERSION = "dbgen_version";
    public static final String CUSTOMER_ADDRESS = "customer_address";
    public static final String CUSTOMER_DEMOGRAPHICS = "customer_demographics";
    public static final String DATE_DIM = "date_dim";
    public static final String TIME_DIM = "time_dim";
    public static final String WAREHOUSE = "warehouse";
    public static final String SHIP_MODE = "ship_mode";
    public static final String REASON = "reason";
    public static final String INCOME_BAND = "income_band";
    public static final String ITEM = "item";
    public static final String STORE = "store";
    public static final String CALL_CENTER = "call_center";
    public static final String CUSTOMER = "customer";
    public static final String WEB_SITE = "web_site";
    public static final String STORE_RETURNS = "store_returns";
    public static final String HOUSEHOLD_DEMOGRAPHICS = "household_demographics";
    public static final String WEB_PAGE = "web_page";
    public static final String PROMOTION = "promotion";
    public static final String CATALOG_PAGE = "catalog_page";
    public static final String INVENTORY = "inventory";
    public static final String CATALOG_RETURNS = "catalog_returns";
    public static final String WEB_RETURNS = "web_returns";
    public static final String WEB_SALES = "web_sales";
    public static final String CATALOG_SALES = "catalog_sales";
    public static final String STORE_SALES = "store_sales";

    /**
     * Staging tables
     */
    public static final String S_CATALOG_PAGE = "s_catalog_page";
    public static final String S_ZIP_TO_GMT = "s_zip_to_gmt";
    public static final String S_PURCHASE_LINEITEM = "s_purchase_lineitem";
    public static final String S_CUSTOMER = "s_customer";
    public static final String S_CUSTOMER_ADDRESS = "s_customer_address";
    public static final String S_PURCHASE = "s_purchase";
    public static final String S_CATALOG_ORDER = "s_catalog_order";
    public static final String S_WEB_ORDER = "s_web_order";
    public static final String S_ITEM = "s_item";
    public static final String S_CATALOG_ORDER_LINEITEM = "s_catalog_order_lineitem";
    public static final String S_WEB_ORDER_LINEITEM = "s_web_order_lineitem";
    public static final String S_STORE = "s_store";
    public static final String S_CALL_CENTER = "s_call_center";
    public static final String S_WEB_SITE = "s_web_site";
    public static final String S_WAREHOUSE = "s_warehouse";
    public static final String S_WEB_PAGE = "s_web_page";
    public static final String S_PROMOTION = "s_promotion";
    public static final String S_STORE_RETURNS = "s_store_returns";
    public static final String S_CATALOG_RETURNS = "s_catalog_returns";
    public static final String S_WEB_RETURNS = "s_web_returns";
    public static final String S_INVENTORY = "s_inventory";

    static final List<String> dwhTables = Arrays.asList(
            DBGEN_VERSION,
            CUSTOMER_ADDRESS,
            CUSTOMER_DEMOGRAPHICS,
            DATE_DIM,
            TIME_DIM,
            WAREHOUSE,
            SHIP_MODE,
            REASON,
            INCOME_BAND,
            ITEM,
            STORE,
            CALL_CENTER,
            CUSTOMER,
            WEB_SITE,
            STORE_RETURNS,
            HOUSEHOLD_DEMOGRAPHICS,
            WEB_PAGE,
            PROMOTION,
            CATALOG_PAGE,
            INVENTORY,
            CATALOG_RETURNS,
            WEB_RETURNS,
            WEB_SALES,
            CATALOG_SALES,
            STORE_SALES
    );

    static final List<String> stagingTables = Arrays.asList(
            S_CATALOG_PAGE
            , S_ZIP_TO_GMT
            , S_PURCHASE_LINEITEM
            , S_CUSTOMER
            , S_CUSTOMER_ADDRESS
            , S_PURCHASE
            , S_CATALOG_ORDER
            , S_WEB_ORDER
            , S_ITEM
            , S_CATALOG_ORDER_LINEITEM
            , S_WEB_ORDER_LINEITEM
            , S_STORE
            , S_CALL_CENTER
            , S_WEB_SITE
            , S_WAREHOUSE
            , S_WEB_PAGE
            , S_PROMOTION
            , S_STORE_RETURNS
            , S_CATALOG_RETURNS
            , S_WEB_RETURNS
            , S_INVENTORY
    );

    /**
     * The store sales snowflake schema
     */
    static final List<String> storeSalesTables = Arrays.asList(
            DBGEN_VERSION,
            CUSTOMER,
            CUSTOMER_ADDRESS,
            CUSTOMER_DEMOGRAPHICS,
            HOUSEHOLD_DEMOGRAPHICS,
            INCOME_BAND,
            PROMOTION,
            ITEM,
            TIME_DIM,
            DATE_DIM,
            STORE,
            STORE_SALES
    );

    private final SchemaDef schema;

    // A map containing all TPCDS tables
    private Map<String, TableDef> tables = new HashMap<>();

    /**
     * Use {@link #get(Database)} functions
     * to of a Tpcds object
     */
    private TpcdsModel(SchemaDef schemaDef) {


        this.schema = schemaDef;

        // case TPCDS_SCHEMA_DWH:
        buildDataWarehouseTables(schema);
        //  TPCDS_SCHEMA_STG:
        buildStagingTables(schema);

    }


    /**
     * @return a tpcds model object
     * in the default namespace
     */
    public static TpcdsModel get() {

        return new TpcdsModel(Databases.of().getCurrentSchema());

    }


    public static TpcdsModel get(Database database) {
        return new TpcdsModel(database.getCurrentSchema());
    }


    /**
     * Add the datawarehouse table to the schema
     * in memory (The tables are not created against the database)
     * <p>
     * This is equivalent to the file (tpcds.sql)
     *
     * @param schema - The schema where to build the datawarehous table
     */
    void buildDataWarehouseTables(SchemaDef schema) {


        final TableDef dbGenTable = Tables.getTable(DBGEN_VERSION)
                .addColumn("dv_version", Types.VARCHAR, 16)
                .addColumn("dv_create_date", Types.DATE)
                .addColumn("dv_create_time", Types.TIME)
                .addColumn("dv_cmdline_args", Types.VARCHAR, 200)
                .setSchema(schema);
        tables.put(dbGenTable.getName(), dbGenTable);


        final TableDef customerAddress = Tables.getTable(CUSTOMER_ADDRESS)
                .addColumn("ca_address_sk", Types.INTEGER, false)
                .addColumn("ca_address_id", Types.CHAR, 16, false)
                .addColumn("ca_street_number", Types.CHAR, 10)
                .addColumn("ca_street_name", Types.VARCHAR, 60)
                .addColumn("ca_street_type", Types.CHAR, 15)
                .addColumn("ca_suite_number", Types.CHAR, 10)
                .addColumn("ca_city", Types.VARCHAR, 60)
                .addColumn("ca_county", Types.VARCHAR, 30)
                .addColumn("ca_state", Types.CHAR, 2)
                .addColumn("ca_zip", Types.CHAR, 10)
                .addColumn("ca_country", Types.VARCHAR, 20)
                .addColumn("ca_gmt_offset", Types.DECIMAL, 5, 2)
                .addColumn("ca_location_type", Types.CHAR, 20)
                .setPrimaryKey("ca_address_sk")
                .setSchema(schema);
        tables.put(customerAddress.getName(), customerAddress);


        final TableDef customerDemographics = Tables.getTable(CUSTOMER_DEMOGRAPHICS)
                .addColumn("cd_demo_sk", Types.INTEGER, false)
                .addColumn("cd_gender", Types.CHAR, 1)
                .addColumn("cd_marital_status", Types.CHAR, 1)
                .addColumn("cd_education_status", Types.CHAR, 20)
                .addColumn("cd_purchase_estimate", Types.INTEGER)
                .addColumn("cd_credit_rating", Types.CHAR, 10)
                .addColumn("cd_dep_count", Types.INTEGER)
                .addColumn("cd_dep_employed_count", Types.INTEGER)
                .addColumn("cd_dep_college_count", Types.INTEGER)
                .setPrimaryKey("cd_demo_sk")
                .setSchema(schema);
        tables.put(customerDemographics.getName(), customerDemographics);


        final TableDef dateDim = Tables.get(DATE_DIM)
                .addColumn("d_date_sk", Types.INTEGER, false)
                .addColumn("d_date_id", Types.CHAR, 16, false)
                .addColumn("d_date", Types.DATE)
                .addColumn("d_month_seq", Types.INTEGER)
                .addColumn("d_week_seq", Types.INTEGER)
                .addColumn("d_quarter_seq", Types.INTEGER)
                .addColumn("d_year", Types.INTEGER)
                .addColumn("d_dow", Types.INTEGER)
                .addColumn("d_moy", Types.INTEGER)
                .addColumn("d_dom", Types.INTEGER)
                .addColumn("d_qoy", Types.INTEGER)
                .addColumn("d_fy_year", Types.INTEGER)
                .addColumn("d_fy_quarter_seq", Types.INTEGER)
                .addColumn("d_fy_week_seq", Types.INTEGER)
                .addColumn("d_day_name", Types.CHAR, 9)
                .addColumn("d_quarter_name", Types.CHAR, 6)
                .addColumn("d_holiday", Types.CHAR, 1)
                .addColumn("d_weekend", Types.CHAR, 1)
                .addColumn("d_following_holiday", Types.CHAR, 1)
                .addColumn("d_first_dom", Types.INTEGER)
                .addColumn("d_last_dom", Types.INTEGER)
                .addColumn("d_same_day_ly", Types.INTEGER)
                .addColumn("d_same_day_lq", Types.INTEGER)
                .addColumn("d_current_day", Types.CHAR, 1)
                .addColumn("d_current_week", Types.CHAR, 1)
                .addColumn("d_current_month", Types.CHAR, 1)
                .addColumn("d_current_quarter", Types.CHAR, 1)
                .addColumn("d_current_year", Types.CHAR, 1)
                .setPrimaryKey("d_date_sk")
                .setSchema(schema);
        tables.put(dateDim.getName(), dateDim);

        final TableDef warehouse = Tables.get(WAREHOUSE)
                .addColumn("w_warehouse_sk", Types.INTEGER, false)
                .addColumn("w_warehouse_id", Types.CHAR, 16, false)
                .addColumn("w_warehouse_name", Types.VARCHAR, 20)
                .addColumn("w_warehouse_sq_ft", Types.INTEGER)
                .addColumn("w_street_number", Types.CHAR, 10)
                .addColumn("w_street_name", Types.VARCHAR, 60)
                .addColumn("w_street_type", Types.CHAR, 15)
                .addColumn("w_suite_number", Types.CHAR, 10)
                .addColumn("w_city", Types.VARCHAR, 60)
                .addColumn("w_county", Types.VARCHAR, 30)
                .addColumn("w_state", Types.CHAR, 2)
                .addColumn("w_zip", Types.CHAR, 10)
                .addColumn("w_country", Types.VARCHAR, 20)
                .addColumn("w_gmt_offset", Types.DECIMAL, 5, 2)
                .setPrimaryKey("w_warehouse_sk")
                .setSchema(schema);
        tables.put(warehouse.getName(), warehouse);


        final TableDef shipMode = Tables.get(SHIP_MODE)
                .addColumn("sm_ship_mode_sk", Types.INTEGER, false)
                .addColumn("sm_ship_mode_id", Types.CHAR, 16, false)
                .addColumn("sm_type", Types.CHAR, 30)
                .addColumn("sm_code", Types.CHAR, 10)
                .addColumn("sm_carrier", Types.CHAR, 20)
                .addColumn("sm_contract", Types.CHAR, 20)
                .setPrimaryKey("sm_ship_mode_sk")
                .setSchema(schema);
        tables.put(shipMode.getName(), shipMode);

        final TableDef timeDim = Tables.get(TIME_DIM)
                .addColumn("t_time_sk", Types.INTEGER, false)
                .addColumn("t_time_id", Types.CHAR, 16, false)
                .addColumn("t_time", Types.INTEGER)
                .addColumn("t_hour", Types.INTEGER)
                .addColumn("t_minute", Types.INTEGER)
                .addColumn("t_second", Types.INTEGER)
                .addColumn("t_am_pm", Types.CHAR, 2)
                .addColumn("t_shift", Types.CHAR, 20)
                .addColumn("t_sub_shift", Types.CHAR, 20)
                .addColumn("t_meal_time", Types.CHAR, 20)
                .setPrimaryKey("t_time_sk")
                .setSchema(schema);
        tables.put(timeDim.getName(), timeDim);

        final TableDef reason = Tables.get(REASON)
                .addColumn("r_reason_sk", Types.INTEGER, false)
                .addColumn("r_reason_id", Types.CHAR, 16, false)
                .addColumn("r_reason_desc", Types.CHAR, 100)
                .setPrimaryKey("r_reason_sk")
                .setSchema(schema);
        tables.put(reason.getName(), reason);

        final TableDef incomeBand = Tables.get(INCOME_BAND)
                .addColumn("ib_income_band_sk", Types.INTEGER, false)
                .addColumn("ib_lower_bound", Types.INTEGER)
                .addColumn("ib_upper_bound", Types.INTEGER)
                .setPrimaryKey("ib_income_band_sk")
                .setSchema(schema);
        tables.put(incomeBand.getName(), incomeBand);

        final TableDef item = Tables.get(ITEM)
                .addColumn("i_item_sk", Types.INTEGER, false)
                .addColumn("i_item_id", Types.CHAR, 16, false)
                .addColumn("i_rec_start_date", Types.DATE)
                .addColumn("i_rec_end_date", Types.DATE)
                .addColumn("i_item_desc", Types.VARCHAR, 200)
                .addColumn("i_current_price", Types.DECIMAL, 7, 2)
                .addColumn("i_wholesale_cost", Types.DECIMAL, 7, 2)
                .addColumn("i_brand_id", Types.INTEGER)
                .addColumn("i_brand", Types.CHAR, 50)
                .addColumn("i_class_id", Types.INTEGER)
                .addColumn("i_class", Types.CHAR, 50)
                .addColumn("i_category_id", Types.INTEGER)
                .addColumn("i_category", Types.CHAR, 50)
                .addColumn("i_manufact_id", Types.INTEGER)
                .addColumn("i_manufact", Types.CHAR, 50)
                .addColumn("i_size", Types.CHAR, 20)
                .addColumn("i_formulation", Types.CHAR, 20)
                .addColumn("i_color", Types.CHAR, 20)
                .addColumn("i_units", Types.CHAR, 10)
                .addColumn("i_container", Types.CHAR, 10)
                .addColumn("i_manager_id", Types.INTEGER)
                .addColumn("i_product_name", Types.CHAR, 50)
                .setPrimaryKey("i_item_sk")
                .setSchema(schema);
        tables.put(item.getName(), item);

        final TableDef store = Tables.get(STORE)
                .addColumn("s_store_sk", Types.INTEGER, false)
                .addColumn("s_store_id", Types.CHAR, 16, false)
                .addColumn("s_rec_start_date", Types.DATE)
                .addColumn("s_rec_end_date", Types.DATE)
                .addColumn("s_closed_date_sk", Types.INTEGER)
                .addColumn("s_store_name", Types.VARCHAR, 50)
                .addColumn("s_number_employees", Types.INTEGER)
                .addColumn("s_floor_space", Types.INTEGER)
                .addColumn("s_hours", Types.CHAR, 20)
                .addColumn("s_manager", Types.VARCHAR, 40)
                .addColumn("s_market_id", Types.INTEGER)
                .addColumn("s_geography_class", Types.VARCHAR, 100)
                .addColumn("s_market_desc", Types.VARCHAR, 100)
                .addColumn("s_market_manager", Types.VARCHAR, 40)
                .addColumn("s_division_id", Types.INTEGER)
                .addColumn("s_division_name", Types.VARCHAR, 50)
                .addColumn("s_company_id", Types.INTEGER)
                .addColumn("s_company_name", Types.VARCHAR, 50)
                .addColumn("s_street_number", Types.VARCHAR, 10)
                .addColumn("s_street_name", Types.VARCHAR, 60)
                .addColumn("s_street_type", Types.CHAR, 15)
                .addColumn("s_suite_number", Types.CHAR, 10)
                .addColumn("s_city", Types.VARCHAR, 60)
                .addColumn("s_county", Types.VARCHAR, 30)
                .addColumn("s_state", Types.CHAR, 2)
                .addColumn("s_zip", Types.CHAR, 10)
                .addColumn("s_country", Types.VARCHAR, 20)
                .addColumn("s_gmt_offset", Types.DECIMAL, 5, 2)
                .addColumn("s_tax_precentage", Types.DECIMAL, 5, 2)
                .setPrimaryKey("s_store_sk")
                .addForeignKey(dateDim, "s_closed_date_sk")
                .setSchema(schema);
        tables.put(store.getName(), store);

        final TableDef callCenter = Tables.get(CALL_CENTER)
                .addColumn("cc_call_center_sk", Types.INTEGER, false)
                .addColumn("cc_call_center_id", Types.CHAR, 16, false)
                .addColumn("cc_rec_start_date", Types.DATE)
                .addColumn("cc_rec_end_date", Types.DATE)
                .addColumn("cc_closed_date_sk", Types.INTEGER)
                .addColumn("cc_open_date_sk", Types.INTEGER)
                .addColumn("cc_name", Types.VARCHAR, 50)
                .addColumn("cc_class", Types.VARCHAR, 50)
                .addColumn("cc_employees", Types.INTEGER)
                .addColumn("cc_sq_ft", Types.INTEGER)
                .addColumn("cc_hours", Types.CHAR, 20)
                .addColumn("cc_manager", Types.VARCHAR, 40)
                .addColumn("cc_mkt_id", Types.INTEGER)
                .addColumn("cc_mkt_class", Types.CHAR, 50)
                .addColumn("cc_mkt_desc", Types.VARCHAR, 100)
                .addColumn("cc_market_manager", Types.VARCHAR, 40)
                .addColumn("cc_division", Types.INTEGER)
                .addColumn("cc_division_name", Types.VARCHAR, 50)
                .addColumn("cc_company", Types.INTEGER)
                .addColumn("cc_company_name", Types.CHAR, 50)
                .addColumn("cc_street_number", Types.CHAR, 10)
                .addColumn("cc_street_name", Types.VARCHAR, 60)
                .addColumn("cc_street_type", Types.CHAR, 15)
                .addColumn("cc_suite_number", Types.CHAR, 10)
                .addColumn("cc_city", Types.VARCHAR, 60)
                .addColumn("cc_county", Types.VARCHAR, 30)
                .addColumn("cc_state", Types.CHAR, 2)
                .addColumn("cc_zip", Types.CHAR, 10)
                .addColumn("cc_country", Types.VARCHAR, 20)
                .addColumn("cc_gmt_offset", Types.DECIMAL, 5, 2)
                .addColumn("cc_tax_percentage", Types.DECIMAL, 5, 2)
                .setPrimaryKey("cc_call_center_sk")
                .addForeignKey(dateDim, "cc_closed_date_sk")
                .addForeignKey(dateDim, "cc_open_date_sk")
                .setSchema(schema);
        tables.put(callCenter.getName(), callCenter);

        TableDef householdDemographics = Tables.get(HOUSEHOLD_DEMOGRAPHICS)
                .addColumn("hd_demo_sk", Types.INTEGER, false)
                .addColumn("hd_income_band_sk", Types.INTEGER)
                .addColumn("hd_buy_potential", Types.CHAR, 15)
                .addColumn("hd_dep_count", Types.INTEGER)
                .addColumn("hd_vehicle_count", Types.INTEGER)
                .setPrimaryKey("hd_demo_sk")
                .addForeignKey(incomeBand, "hd_income_band_sk")
                .setSchema(schema);
        tables.put(householdDemographics.getName(), householdDemographics);

        final TableDef customer = Tables.get(CUSTOMER)
                .addColumn("c_customer_sk", Types.INTEGER, false)
                .addColumn("c_customer_id", Types.CHAR, 16, false)
                .addColumn("c_current_cdemo_sk", Types.INTEGER)
                .addColumn("c_current_hdemo_sk", Types.INTEGER)
                .addColumn("c_current_addr_sk", Types.INTEGER)
                .addColumn("c_first_shipto_date_sk", Types.INTEGER)
                .addColumn("c_first_sales_date_sk", Types.INTEGER)
                .addColumn("c_salutation", Types.CHAR, 10)
                .addColumn("c_first_name", Types.CHAR, 20)
                .addColumn("c_last_name", Types.CHAR, 30)
                .addColumn("c_preferred_cust_flag", Types.CHAR, 1)
                .addColumn("c_birth_day", Types.INTEGER)
                .addColumn("c_birth_month", Types.INTEGER)
                .addColumn("c_birth_year", Types.INTEGER)
                .addColumn("c_birth_country", Types.VARCHAR, 20)
                .addColumn("c_login", Types.CHAR, 13)
                .addColumn("c_email_address", Types.CHAR, 50)
                .addColumn("c_last_review_date_sk", Types.INTEGER, 10)
                .setPrimaryKey("c_customer_sk")
                .addForeignKey(customerDemographics, "c_current_cdemo_sk")
                .addForeignKey(householdDemographics, "c_current_hdemo_sk")
                .addForeignKey(customerAddress, "c_current_addr_sk")
                .addForeignKey(dateDim, "c_first_shipto_date_sk")
                .addForeignKey(dateDim, "c_first_sales_date_sk")
                .addForeignKey(dateDim, "c_last_review_date_sk")
                .setSchema(schema);
        tables.put(customer.getName(), customer);

        final TableDef webSite = Tables.get(WEB_SITE)
                .addColumn("web_site_sk", Types.INTEGER, false)
                .addColumn("web_site_id", Types.CHAR, 16)
                .addColumn("web_rec_start_date", Types.DATE)
                .addColumn("web_rec_end_date", Types.DATE)
                .addColumn("web_name", Types.VARCHAR, 50)
                .addColumn("web_open_date_sk", Types.INTEGER)
                .addColumn("web_close_date_sk", Types.INTEGER)
                .addColumn("web_class", Types.VARCHAR, 50)
                .addColumn("web_manager", Types.VARCHAR, 40)
                .addColumn("web_mkt_id", Types.INTEGER)
                .addColumn("web_mkt_class", Types.VARCHAR, 50)
                .addColumn("web_mkt_desc", Types.VARCHAR, 100)
                .addColumn("web_market_manager", Types.VARCHAR, 40)
                .addColumn("web_company_id", Types.INTEGER)
                .addColumn("web_company_name", Types.CHAR, 50)
                .addColumn("web_street_number", Types.CHAR)
                .addColumn("web_street_name", Types.VARCHAR, 60)
                .addColumn("web_street_type", Types.CHAR, 15)
                .addColumn("web_suite_number", Types.CHAR, 10)
                .addColumn("web_city", Types.VARCHAR, 60)
                .addColumn("web_county", Types.VARCHAR, 30)
                .addColumn("web_state", Types.CHAR, 2)
                .addColumn("web_zip", Types.CHAR, 10)
                .addColumn("web_country", Types.VARCHAR, 20)
                .addColumn("web_gmt_offset", Types.DECIMAL, 5, 2)
                .addColumn("web_tax_percentage", Types.DECIMAL, 5, 2)
                .setPrimaryKey("web_site_sk")
                .addForeignKey(dateDim, "web_open_date_sk")
                .addForeignKey(dateDim, "web_close_date_sk")
                .setSchema(schema);
        tables.put(webSite.getName(), webSite);

        TableDef promotion = Tables.get(PROMOTION)
                .addColumn("p_promo_sk", Types.INTEGER, false)
                .addColumn("p_promo_id", Types.CHAR, 16, false)
                .addColumn("p_start_date_sk", Types.INTEGER)
                .addColumn("p_end_date_sk", Types.INTEGER)
                .addColumn("p_item_sk", Types.INTEGER)
                .addColumn("p_cost", Types.DECIMAL, 15, 2)
                .addColumn("p_response_target", Types.INTEGER)
                .addColumn("p_promo_name", Types.CHAR, 50)
                .addColumn("p_channel_dmail", Types.CHAR, 1)
                .addColumn("p_channel_email", Types.CHAR, 1)
                .addColumn("p_channel_catalog", Types.CHAR, 1)
                .addColumn("p_channel_tv", Types.CHAR, 1)
                .addColumn("p_channel_radio", Types.CHAR, 1)
                .addColumn("p_channel_press", Types.CHAR, 1)
                .addColumn("p_channel_event", Types.CHAR, 1)
                .addColumn("p_channel_demo", Types.CHAR, 1)
                .addColumn("p_channel_details", Types.VARCHAR, 100)
                .addColumn("p_purpose", Types.CHAR, 15)
                .addColumn("p_discount_active", Types.CHAR, 1)
                .setPrimaryKey("p_promo_sk")
                .addForeignKey(dateDim, "p_start_date_sk")
                .addForeignKey(dateDim, "p_end_date_sk")
                .addForeignKey(item, "p_item_sk")
                .setSchema(schema);
        tables.put(promotion.getName(), promotion);


        TableDef storeSales = Tables.get(STORE_SALES)
                .addColumn("ss_sold_date_sk", Types.INTEGER)
                .addColumn("ss_sold_time_sk", Types.INTEGER)
                .addColumn("ss_item_sk", Types.INTEGER, false)
                .addColumn("ss_customer_sk", Types.INTEGER)
                .addColumn("ss_cdemo_sk", Types.INTEGER)
                .addColumn("ss_hdemo_sk", Types.INTEGER)
                .addColumn("ss_addr_sk", Types.INTEGER)
                .addColumn("ss_store_sk", Types.INTEGER)
                .addColumn("ss_promo_sk", Types.INTEGER)
                .addColumn("ss_ticket_number", Types.INTEGER, false)
                .addColumn("ss_quantity", Types.INTEGER)
                .addColumn("ss_wholesale_cost", Types.DECIMAL, 7, 2)
                .addColumn("ss_list_price", Types.DECIMAL, 7, 2)
                .addColumn("ss_sales_price", Types.DECIMAL, 7, 2)
                .addColumn("ss_ext_discount_amt", Types.DECIMAL, 7, 2)
                .addColumn("ss_ext_sales_price", Types.DECIMAL, 7, 2)
                .addColumn("ss_ext_wholesale_cost", Types.DECIMAL, 7, 2)
                .addColumn("ss_ext_list_price", Types.DECIMAL, 7, 2)
                .addColumn("ss_ext_tax", Types.DECIMAL, 7, 2)
                .addColumn("ss_coupon_amt", Types.DECIMAL, 7, 2)
                .addColumn("ss_net_paid", Types.DECIMAL, 7, 2)
                .addColumn("ss_net_paid_inc_tax", Types.DECIMAL, 7, 2)
                .addColumn("ss_net_profit", Types.DECIMAL, 7, 2)
                .setPrimaryKey("ss_item_sk", "ss_ticket_number")
                .addForeignKey(dateDim, "ss_sold_date_sk")
                .addForeignKey(timeDim, "ss_sold_time_sk")
                .addForeignKey(item, "ss_item_sk")
                .addForeignKey(customer, "ss_customer_sk")
                .addForeignKey(customerDemographics, "ss_cdemo_sk")
                .addForeignKey(householdDemographics, "ss_hdemo_sk")
                .addForeignKey(customerAddress, "ss_addr_sk")
                .addForeignKey(store, "ss_store_sk")
                .addForeignKey(promotion, "ss_promo_sk")
                .setSchema(schema);
        tables.put(storeSales.getName(), storeSales);

        tables.put(STORE_RETURNS, Tables.get(STORE_RETURNS)
                .addColumn("sr_returned_date_sk", Types.INTEGER)
                .addColumn("sr_return_time_sk", Types.INTEGER)
                .addColumn("sr_item_sk", Types.INTEGER, false)
                .addColumn("sr_customer_sk", Types.INTEGER)
                .addColumn("sr_cdemo_sk", Types.INTEGER)
                .addColumn("sr_hdemo_sk", Types.INTEGER)
                .addColumn("sr_addr_sk", Types.INTEGER)
                .addColumn("sr_store_sk", Types.INTEGER)
                .addColumn("sr_reason_sk", Types.INTEGER)
                .addColumn("sr_ticket_number", Types.INTEGER, false)
                .addColumn("sr_return_quantity", Types.INTEGER)
                .addColumn("sr_return_amt", Types.DECIMAL, 7, 2)
                .addColumn("sr_return_tax", Types.DECIMAL, 7, 2)
                .addColumn("sr_return_amt_inc_tax", Types.DECIMAL, 7, 2)
                .addColumn("sr_fee", Types.DECIMAL, 7, 2)
                .addColumn("sr_return_ship_cost", Types.DECIMAL, 7, 2)
                .addColumn("sr_refunded_cash", Types.DECIMAL, 7, 2)
                .addColumn("sr_reversed_charge", Types.DECIMAL, 7, 2)
                .addColumn("sr_store_credit", Types.DECIMAL, 7, 2)
                .addColumn("sr_net_loss", Types.DECIMAL, 7, 2)
                .setPrimaryKey("sr_item_sk", "sr_ticket_number")
                .addForeignKey(dateDim, "sr_returned_date_sk")
                .addForeignKey(timeDim, "sr_return_time_sk")
                .addForeignKey(item, "sr_item_sk")
                .addForeignKey(storeSales, "sr_item_sk", "sr_ticket_number")
                .addForeignKey(customer, "sr_customer_sk")
                .addForeignKey(customerDemographics, "sr_cdemo_sk")
                .addForeignKey(householdDemographics, "sr_hdemo_sk")
                .addForeignKey(customerAddress, "sr_addr_sk")
                .addForeignKey(store, "sr_store_sk")
                .addForeignKey(reason, "sr_reason_sk")
                .setSchema(schema));


        final TableDef webPage = Tables.get(WEB_PAGE)
                .addColumn("wp_web_page_sk", Types.INTEGER, false)
                .addColumn("wp_web_page_id", Types.CHAR, 16, false)
                .addColumn("wp_rec_start_date", Types.DATE)
                .addColumn("wp_rec_end_date", Types.DATE)
                .addColumn("wp_creation_date_sk", Types.INTEGER)
                .addColumn("wp_access_date_sk", Types.INTEGER)
                .addColumn("wp_autogen_flag", Types.CHAR, 1)
                .addColumn("wp_customer_sk", Types.INTEGER)
                .addColumn("wp_url", Types.VARCHAR, 100)
                .addColumn("wp_type", Types.CHAR, 50)
                .addColumn("wp_char_count", Types.INTEGER)
                .addColumn("wp_link_count", Types.INTEGER)
                .addColumn("wp_image_count", Types.INTEGER)
                .addColumn("wp_max_ad_count", Types.INTEGER)
                .setPrimaryKey("wp_web_page_sk")
                .addForeignKey(dateDim, "wp_creation_date_sk")
                .addForeignKey(dateDim, "wp_access_date_sk")
                .addForeignKey(customer, "wp_customer_sk")
                .setSchema(schema);
        tables.put(webPage.getName(), webPage);

        final TableDef catalogPage = Tables.get(CATALOG_PAGE)
                .addColumn("cp_catalog_page_sk", Types.INTEGER, false)
                .addColumn("cp_catalog_page_id", Types.CHAR, 16, false)
                .addColumn("cp_start_date_sk", Types.INTEGER)
                .addColumn("cp_end_date_sk", Types.INTEGER)
                .addColumn("cp_department", Types.VARCHAR, 50)
                .addColumn("cp_catalog_number", Types.INTEGER)
                .addColumn("cp_catalog_page_number", Types.INTEGER)
                .addColumn("cp_description", Types.VARCHAR, 100)
                .addColumn("cp_type", Types.VARCHAR, 100)
                .setPrimaryKey("cp_catalog_page_sk")
                .addForeignKey(dateDim, "cp_start_date_sk")
                .addForeignKey(dateDim, "cp_end_date_sk")
                .setSchema(schema);
        tables.put(CATALOG_PAGE, catalogPage);

        tables.put(INVENTORY, Tables.get(INVENTORY)
                .addColumn("inv_date_sk", Types.INTEGER, false)
                .addColumn("inv_item_sk", Types.INTEGER, false)
                .addColumn("inv_warehouse_sk", Types.INTEGER, false)
                .addColumn("inv_quantity_on_hand", Types.INTEGER)
                .setPrimaryKey("inv_date_sk", "inv_item_sk", "inv_warehouse_sk")
                .addForeignKey(dateDim, "inv_date_sk")
                .addForeignKey(item, "inv_item_sk")
                .addForeignKey(warehouse, "inv_warehouse_sk")
                .setSchema(schema));

        TableDef catalogSales = Tables.get(CATALOG_SALES)
                .addColumn("cs_sold_date_sk", Types.INTEGER)
                .addColumn("cs_sold_time_sk", Types.INTEGER)
                .addColumn("cs_ship_date_sk", Types.INTEGER)
                .addColumn("cs_bill_customer_sk", Types.INTEGER)
                .addColumn("cs_bill_cdemo_sk", Types.INTEGER)
                .addColumn("cs_bill_hdemo_sk", Types.INTEGER)
                .addColumn("cs_bill_addr_sk", Types.INTEGER)
                .addColumn("cs_ship_customer_sk", Types.INTEGER)
                .addColumn("cs_ship_cdemo_sk", Types.INTEGER)
                .addColumn("cs_ship_hdemo_sk", Types.INTEGER)
                .addColumn("cs_ship_addr_sk", Types.INTEGER)
                .addColumn("cs_call_center_sk", Types.INTEGER)
                .addColumn("cs_catalog_page_sk", Types.INTEGER)
                .addColumn("cs_ship_mode_sk", Types.INTEGER)
                .addColumn("cs_warehouse_sk", Types.INTEGER)
                .addColumn("cs_item_sk", Types.INTEGER, false)
                .addColumn("cs_promo_sk", Types.INTEGER)
                .addColumn("cs_order_number", Types.INTEGER, false)
                .addColumn("cs_quantity", Types.INTEGER)
                .addColumn("cs_wholesale_cost", Types.DECIMAL, 7, 2)
                .addColumn("cs_list_price", Types.DECIMAL, 7, 2)
                .addColumn("cs_sales_price", Types.DECIMAL, 7, 2)
                .addColumn("cs_ext_discount_amt", Types.DECIMAL, 7, 2)
                .addColumn("cs_ext_sales_price", Types.DECIMAL, 7, 2)
                .addColumn("cs_ext_wholesale_cost", Types.DECIMAL, 7, 2)
                .addColumn("cs_ext_list_price", Types.DECIMAL, 7, 2)
                .addColumn("cs_ext_tax", Types.DECIMAL, 7, 2)
                .addColumn("cs_coupon_amt", Types.DECIMAL, 7, 2)
                .addColumn("cs_ext_ship_cost", Types.DECIMAL, 7, 2)
                .addColumn("cs_net_paid", Types.DECIMAL, 7, 2)
                .addColumn("cs_net_paid_inc_tax", Types.DECIMAL, 7, 2)
                .addColumn("cs_net_paid_inc_ship", Types.DECIMAL, 7, 2)
                .addColumn("cs_net_paid_inc_ship_tax", Types.DECIMAL, 7, 2)
                .addColumn("cs_net_profit", Types.DECIMAL, 7, 2)
                .setPrimaryKey("cs_item_sk", "cs_order_number")
                .addForeignKey(dateDim, "cs_sold_date_sk")
                .addForeignKey(timeDim, "cs_sold_time_sk")
                .addForeignKey(dateDim, "cs_ship_date_sk")
                .addForeignKey(customer, "cs_bill_customer_sk")
                .addForeignKey(customerDemographics, "cs_bill_cdemo_sk")
                .addForeignKey(householdDemographics, "cs_bill_hdemo_sk")
                .addForeignKey(customerAddress, "cs_bill_addr_sk")
                .addForeignKey(customer, "cs_ship_customer_sk")
                .addForeignKey(customerDemographics, "cs_ship_cdemo_sk")
                .addForeignKey(householdDemographics, "cs_ship_hdemo_sk")
                .addForeignKey(customerAddress, "cs_ship_addr_sk")
                .addForeignKey(callCenter, "cs_call_center_sk")
                .addForeignKey(catalogPage, "cs_catalog_page_sk")
                .addForeignKey(shipMode, "cs_ship_mode_sk")
                .addForeignKey(warehouse, "cs_warehouse_sk")
                .addForeignKey(item, "cs_item_sk")
                .addForeignKey(promotion, "cs_promo_sk")
                .setSchema(schema);
        tables.put(catalogSales.getName(), catalogSales);

        tables.put(CATALOG_RETURNS, Tables.get(CATALOG_RETURNS)
                .addColumn("cr_returned_date_sk", Types.INTEGER)
                .addColumn("cr_returned_time_sk", Types.INTEGER)
                .addColumn("cr_item_sk", Types.INTEGER, false)
                .addColumn("cr_refunded_customer_sk", Types.INTEGER)
                .addColumn("cr_refunded_cdemo_sk", Types.INTEGER)
                .addColumn("cr_refunded_hdemo_sk", Types.INTEGER)
                .addColumn("cr_refunded_addr_sk", Types.INTEGER)
                .addColumn("cr_returning_customer_sk", Types.INTEGER)
                .addColumn("cr_returning_cdemo_sk", Types.INTEGER)
                .addColumn("cr_returning_hdemo_sk", Types.INTEGER)
                .addColumn("cr_returning_addr_sk", Types.INTEGER)
                .addColumn("cr_call_center_sk", Types.INTEGER)
                .addColumn("cr_catalog_page_sk", Types.INTEGER)
                .addColumn("cr_ship_mode_sk", Types.INTEGER)
                .addColumn("cr_warehouse_sk", Types.INTEGER)
                .addColumn("cr_reason_sk", Types.INTEGER)
                .addColumn("cr_order_number", Types.INTEGER, false)
                .addColumn("cr_return_quantity", Types.INTEGER)
                .addColumn("cr_return_amount", Types.DECIMAL, 7, 2)
                .addColumn("cr_return_tax", Types.DECIMAL, 7, 2)
                .addColumn("cr_return_amt_inc_tax", Types.DECIMAL, 7, 2)
                .addColumn("cr_fee", Types.DECIMAL, 7, 2)
                .addColumn("cr_return_ship_cost", Types.DECIMAL, 7, 2)
                .addColumn("cr_refunded_cash", Types.DECIMAL, 7, 2)
                .addColumn("cr_reversed_charge", Types.DECIMAL, 7, 2)
                .addColumn("cr_store_credit", Types.DECIMAL, 7, 2)
                .addColumn("cr_net_loss", Types.DECIMAL, 7, 2)
                .setPrimaryKey("cr_item_sk", "cr_order_number")
                .addForeignKey(dateDim, "cr_returned_date_sk")
                .addForeignKey(timeDim, "cr_returned_time_sk")
                .addForeignKey(item, "cr_item_sk")
                .addForeignKey(catalogSales, "cr_item_sk", "cr_order_number")
                .addForeignKey(customer, "cr_refunded_customer_sk")
                .addForeignKey(customerDemographics, "cr_refunded_cdemo_sk")
                .addForeignKey(householdDemographics, "cr_refunded_hdemo_sk")
                .addForeignKey(customerAddress, "cr_refunded_addr_sk")
                .addForeignKey(customer, "cr_returning_customer_sk")
                .addForeignKey(customerDemographics, "cr_returning_cdemo_sk")
                .addForeignKey(householdDemographics, "cr_returning_hdemo_sk")
                .addForeignKey(customerAddress, "cr_returning_addr_sk")
                .addForeignKey(callCenter, "cr_call_center_sk")
                .addForeignKey(catalogPage, "cr_catalog_page_sk")
                .addForeignKey(shipMode, "cr_ship_mode_sk")
                .addForeignKey(warehouse, "cr_warehouse_sk")
                .addForeignKey(reason, "cr_reason_sk")
                .setSchema(schema));

        TableDef webSales = Tables.get(WEB_SALES)
                .addColumn("ws_sold_date_sk", Types.INTEGER)
                .addColumn("ws_sold_time_sk", Types.INTEGER)
                .addColumn("ws_ship_date_sk", Types.INTEGER)
                .addColumn("ws_item_sk", Types.INTEGER, false)
                .addColumn("ws_bill_customer_sk", Types.INTEGER)
                .addColumn("ws_bill_cdemo_sk", Types.INTEGER)
                .addColumn("ws_bill_hdemo_sk", Types.INTEGER)
                .addColumn("ws_bill_addr_sk", Types.INTEGER)
                .addColumn("ws_ship_customer_sk", Types.INTEGER)
                .addColumn("ws_ship_cdemo_sk", Types.INTEGER)
                .addColumn("ws_ship_hdemo_sk", Types.INTEGER)
                .addColumn("ws_ship_addr_sk", Types.INTEGER)
                .addColumn("ws_web_page_sk", Types.INTEGER)
                .addColumn("ws_web_site_sk", Types.INTEGER)
                .addColumn("ws_ship_mode_sk", Types.INTEGER)
                .addColumn("ws_warehouse_sk", Types.INTEGER)
                .addColumn("ws_promo_sk", Types.INTEGER)
                .addColumn("ws_order_number", Types.INTEGER, false)
                .addColumn("ws_quantity", Types.INTEGER)
                .addColumn("ws_wholesale_cost", Types.DECIMAL, 7, 2)
                .addColumn("ws_list_price", Types.DECIMAL, 7, 2)
                .addColumn("ws_sales_price", Types.DECIMAL, 7, 2)
                .addColumn("ws_ext_discount_amt", Types.DECIMAL, 7, 2)
                .addColumn("ws_ext_sales_price", Types.DECIMAL, 7, 2)
                .addColumn("ws_ext_wholesale_cost", Types.DECIMAL, 7, 2)
                .addColumn("ws_ext_list_price", Types.DECIMAL, 7, 2)
                .addColumn("ws_ext_tax", Types.DECIMAL, 7, 2)
                .addColumn("ws_coupon_amt", Types.DECIMAL, 7, 2)
                .addColumn("ws_ext_ship_cost", Types.DECIMAL, 7, 2)
                .addColumn("ws_net_paid", Types.DECIMAL, 7, 2)
                .addColumn("ws_net_paid_inc_tax", Types.DECIMAL, 7, 2)
                .addColumn("ws_net_paid_inc_ship", Types.DECIMAL, 7, 2)
                .addColumn("ws_net_paid_inc_ship_tax", Types.DECIMAL, 7, 2)
                .addColumn("ws_net_profit", Types.DECIMAL, 7, 2)
                .setPrimaryKey("ws_item_sk", "ws_order_number")
                .addForeignKey(dateDim, "ws_sold_date_sk")
                .addForeignKey(timeDim, "ws_sold_time_sk")
                .addForeignKey(dateDim, "ws_ship_date_sk")
                .addForeignKey(item, "ws_item_sk")
                .addForeignKey(customer, "ws_bill_customer_sk")
                .addForeignKey(customerDemographics, "ws_bill_cdemo_sk")
                .addForeignKey(householdDemographics, "ws_bill_hdemo_sk")
                .addForeignKey(customerAddress, "ws_bill_addr_sk")
                .addForeignKey(customer, "ws_ship_customer_sk")
                .addForeignKey(customerDemographics, "ws_ship_cdemo_sk")
                .addForeignKey(householdDemographics, "ws_ship_hdemo_sk")
                .addForeignKey(customerAddress, "ws_ship_addr_sk")
                .addForeignKey(webPage, "ws_web_page_sk")
                .addForeignKey(webSite, "ws_web_site_sk")
                .addForeignKey(shipMode, "ws_ship_mode_sk")
                .addForeignKey(warehouse, "ws_warehouse_sk")
                .addForeignKey(promotion, "ws_promo_sk")
                .setSchema(schema);
        tables.put(webSales.getName(), webSales);

        tables.put(WEB_RETURNS, Tables.get(WEB_RETURNS)
                .addColumn("wr_returned_date_sk", Types.INTEGER)
                .addColumn("wr_returned_time_sk", Types.INTEGER)
                .addColumn("wr_item_sk", Types.INTEGER, false)
                .addColumn("wr_refunded_customer_sk", Types.INTEGER)
                .addColumn("wr_refunded_cdemo_sk", Types.INTEGER)
                .addColumn("wr_refunded_hdemo_sk", Types.INTEGER)
                .addColumn("wr_refunded_addr_sk", Types.INTEGER)
                .addColumn("wr_returning_customer_sk", Types.INTEGER)
                .addColumn("wr_returning_cdemo_sk", Types.INTEGER)
                .addColumn("wr_returning_hdemo_sk", Types.INTEGER)
                .addColumn("wr_returning_addr_sk", Types.INTEGER)
                .addColumn("wr_web_page_sk", Types.INTEGER)
                .addColumn("wr_reason_sk", Types.INTEGER)
                .addColumn("wr_order_number", Types.INTEGER, false)
                .addColumn("wr_return_quantity", Types.INTEGER)
                .addColumn("wr_return_amt", Types.DECIMAL, 7, 2)
                .addColumn("wr_return_tax", Types.DECIMAL, 7, 2)
                .addColumn("wr_return_amt_inc_tax", Types.DECIMAL, 7, 2)
                .addColumn("wr_fee", Types.DECIMAL, 7, 2)
                .addColumn("wr_return_ship_cost", Types.DECIMAL, 7, 2)
                .addColumn("wr_refunded_cash", Types.DECIMAL, 7, 2)
                .addColumn("wr_reversed_charge", Types.DECIMAL, 7, 2)
                .addColumn("wr_account_credit", Types.DECIMAL, 7, 2)
                .addColumn("wr_net_loss", Types.DECIMAL, 7, 2)
                .setPrimaryKey("wr_item_sk", "wr_order_number")
                .addForeignKey(dateDim, "wr_returned_date_sk")
                .addForeignKey(timeDim, "wr_returned_time_sk")
                .addForeignKey(item, "wr_item_sk")
                .addForeignKey(webSales, "wr_item_sk", "wr_order_number")
                .addForeignKey(customer, "wr_refunded_customer_sk")
                .addForeignKey(customerDemographics, "wr_refunded_cdemo_sk")
                .addForeignKey(householdDemographics, "wr_refunded_hdemo_sk")
                .addForeignKey(customerAddress, "wr_refunded_addr_sk")
                .addForeignKey(customer, "wr_returning_customer_sk")
                .addForeignKey(customerDemographics, "wr_returning_cdemo_sk")
                .addForeignKey(householdDemographics, "wr_returning_hdemo_sk")
                .addForeignKey(customerAddress, "wr_returning_addr_sk")
                .addForeignKey(webPage, "wr_web_page_sk")
                .addForeignKey(reason, "wr_reason_sk")
                .setSchema(schema));

    }


    /**
     * Build the source table/staging tables.
     * The table are build in the given schema. If the schema is null,
     * it will be in the schema chosen during initialization of the tpcds application.
     * <p>
     * This are the tables of the file tpcds_source.sql
     *
     * @param schema -  The schema where to build the source table
     */
    void buildStagingTables(SchemaDef schema) {

        tables.put(S_CATALOG_PAGE, Tables.get(S_CATALOG_PAGE)
                .addColumn("cpag_catalog_number", Types.INTEGER, false)
                .addColumn("cpag_catalog_page_number", Types.INTEGER, false)
                .addColumn("cpag_department", Types.CHAR, 20)
                .addColumn("cpag_id", Types.CHAR, 16)
                .addColumn("cpag_start_date", Types.CHAR, 10)
                .addColumn("cpag_end_date", Types.CHAR, 10)
                .addColumn("cpag_description", Types.VARCHAR, 100)
                .addColumn("cpag_type", Types.VARCHAR, 100)
                .setSchema(schema));

        tables.put(S_ZIP_TO_GMT, Tables.get(S_ZIP_TO_GMT)
                .addColumn("zipg_zip", Types.CHAR, 5, false)
                .addColumn("zipg_gmt_offset", Types.INTEGER, false)
                .setSchema(schema));

        tables.put(S_PURCHASE_LINEITEM, Tables.get(S_PURCHASE_LINEITEM)
                .addColumn("plin_purchase_id", Types.INTEGER, false)
                .addColumn("plin_line_number", Types.INTEGER, false)
                .addColumn("plin_item_id", Types.CHAR, 16)
                .addColumn("plin_promotion_id", Types.CHAR, 16)
                .addColumn("plin_quantity", Types.INTEGER)
                .addColumn("plin_sale_price", Types.NUMERIC, 7, 2)
                .addColumn("plin_coupon_amt", Types.NUMERIC, 7, 2)
                .addColumn("plin_comment", Types.VARCHAR, 100)
                .setSchema(schema));

        tables.put(S_CUSTOMER, Tables.get(S_CUSTOMER)
                .addColumn("cust_customer_id", Types.CHAR, 16, false)
                .addColumn("cust_salutation", Types.CHAR, 10)
                .addColumn("cust_last_name", Types.CHAR, 20)
                .addColumn("cust_first_name", Types.CHAR, 20)
                .addColumn("cust_preffered_flag", Types.CHAR, 1)
                .addColumn("cust_birth_date", Types.CHAR, 10)
                .addColumn("cust_birth_country", Types.CHAR, 20)
                .addColumn("cust_login_id", Types.CHAR, 13)
                .addColumn("cust_email_address", Types.CHAR, 50)
                .addColumn("cust_last_login_chg_date", Types.CHAR, 10)
                .addColumn("cust_first_shipto_date", Types.CHAR, 10)
                .addColumn("cust_first_purchase_date", Types.CHAR, 10)
                .addColumn("cust_last_review_date", Types.CHAR, 10)
                .addColumn("cust_primary_machine_id", Types.CHAR, 15)
                .addColumn("cust_secondary_machine_id", Types.CHAR, 15)
                .addColumn("cust_street_number", Types.SMALLINT)
                .addColumn("cust_suite_number", Types.CHAR, 10)
                .addColumn("cust_street_name1", Types.CHAR, 30)
                .addColumn("cust_street_name2", Types.CHAR, 30)
                .addColumn("cust_street_type", Types.CHAR, 15)
                .addColumn("cust_city", Types.CHAR, 60)
                .addColumn("cust_zip", Types.CHAR, 10)
                .addColumn("cust_county", Types.CHAR, 30)
                .addColumn("cust_state", Types.CHAR, 2)
                .addColumn("cust_country", Types.CHAR, 20)
                .addColumn("cust_loc_type", Types.CHAR, 20)
                .addColumn("cust_gender", Types.CHAR, 1)
                .addColumn("cust_marital_status", Types.CHAR, 1)
                .addColumn("cust_educ_status", Types.CHAR, 20)
                .addColumn("cust_credit_rating", Types.CHAR, 10)
                .addColumn("cust_purch_est", Types.NUMERIC, 7, 2)
                .addColumn("cust_buy_potential", Types.CHAR, 15)
                .addColumn("cust_depend_cnt", Types.SMALLINT)
                .addColumn("cust_depend_emp_cnt", Types.SMALLINT)
                .addColumn("cust_depend_college_cnt", Types.SMALLINT)
                .addColumn("cust_vehicle_cnt", Types.SMALLINT)
                .addColumn("cust_annual_income", Types.NUMERIC, 9, 2)
                .setSchema(schema));


        tables.put(S_CUSTOMER_ADDRESS, Tables.get(S_CUSTOMER_ADDRESS)
                .addColumn("cadr_address_id", Types.CHAR, 16, false)
                .addColumn("cadr_street_number", Types.INTEGER)
                .addColumn("cadr_street_name1", Types.CHAR, 25)
                .addColumn("cadr_street_name2", Types.CHAR, 25)
                .addColumn("cadr_street_type", Types.CHAR, 15)
                .addColumn("cadr_suitnumber", Types.CHAR, 10)
                .addColumn("cadr_city", Types.CHAR, 60)
                .addColumn("cadr_county", Types.CHAR, 30)
                .addColumn("cadr_state", Types.CHAR, 2)
                .addColumn("cadr_zip", Types.CHAR, 10)
                .addColumn("cadr_country", Types.CHAR, 20)
                .setSchema(schema));

        tables.put(S_PURCHASE, Tables.get(S_PURCHASE)
                .addColumn("purc_purchase_id", Types.INTEGER, false)
                .addColumn("purc_store_id", Types.CHAR, 16)
                .addColumn("purc_customer_id", Types.CHAR, 16)
                .addColumn("purc_purchase_date", Types.CHAR, 10)
                .addColumn("purc_purchase_time", Types.INTEGER)
                .addColumn("purc_register_id", Types.INTEGER)
                .addColumn("purc_clerk_id", Types.INTEGER)
                .addColumn("purc_comment", Types.CHAR, 100)
                .setSchema(schema));

        tables.put(S_CATALOG_ORDER, Tables.get(S_CATALOG_ORDER)
                .addColumn("cord_order_id", Types.INTEGER, false)
                .addColumn("cord_bill_customer_id", Types.CHAR, 16)
                .addColumn("cord_ship_customer_id", Types.CHAR, 16)
                .addColumn("cord_order_date", Types.CHAR, 10)
                .addColumn("cord_order_time", Types.INTEGER)
                .addColumn("cord_ship_mode_id", Types.CHAR, 16)
                .addColumn("cord_call_center_id", Types.CHAR, 16)
                .addColumn("cord_order_comments", Types.VARCHAR, 100)
                .setSchema(schema));

        tables.put(S_WEB_ORDER, Tables.get(S_WEB_ORDER)
                .addColumn("word_order_id", Types.INTEGER, false)
                .addColumn("word_bill_customer_id", Types.CHAR, 16)
                .addColumn("word_ship_customer_id", Types.CHAR, 16)
                .addColumn("word_order_date", Types.CHAR, 10)
                .addColumn("word_order_time", Types.INTEGER)
                .addColumn("word_ship_mode_id", Types.CHAR, 16)
                .addColumn("word_web_site_id", Types.CHAR, 16)
                .addColumn("word_order_comments", Types.CHAR, 100)
                .setSchema(schema));

        tables.put(S_ITEM, Tables.get(S_ITEM)
                .addColumn("item_item_id", Types.CHAR, 16, false)
                .addColumn("item_item_description", Types.CHAR, 200)
                .addColumn("item_list_price", Types.NUMERIC, 7, 2)
                .addColumn("item_wholesale_cost", Types.NUMERIC, 7, 2)
                .addColumn("item_size", Types.CHAR, 20)
                .addColumn("item_formulation", Types.CHAR, 20)
                .addColumn("item_color", Types.CHAR, 20)
                .addColumn("item_units", Types.CHAR, 10)
                .addColumn("item_container", Types.CHAR, 10)
                .addColumn("item_manager_id", Types.INTEGER)
                .setSchema(schema));

        tables.put(S_CATALOG_ORDER_LINEITEM, Tables.get(S_CATALOG_ORDER_LINEITEM)
                .addColumn("clin_order_id", Types.INTEGER, false)
                .addColumn("clin_line_number", Types.INTEGER, false)
                .addColumn("clin_item_id", Types.CHAR, 16)
                .addColumn("clin_promotion_id", Types.CHAR, 16)
                .addColumn("clin_quantity", Types.INTEGER)
                .addColumn("clin_sales_price", Types.NUMERIC, 7, 2)
                .addColumn("clin_coupon_amt", Types.NUMERIC, 7, 2)
                .addColumn("clin_warehouse_id", Types.CHAR, 16)
                .addColumn("clin_ship_date", Types.CHAR, 10)
                .addColumn("clin_catalog_number", Types.INTEGER)
                .addColumn("clin_catalog_page_number", Types.INTEGER)
                .addColumn("clin_ship_cost", Types.NUMERIC, 7, 2)
                .setSchema(schema));

        tables.put(S_WEB_ORDER_LINEITEM, Tables.get(S_WEB_ORDER_LINEITEM)
                .addColumn("wlin_order_id", Types.INTEGER, false)
                .addColumn("wlin_line_number", Types.INTEGER, false)
                .addColumn("wlin_item_id", Types.CHAR, 16)
                .addColumn("wlin_promotion_id", Types.CHAR, 16)
                .addColumn("wlin_quantity", Types.INTEGER)
                .addColumn("wlin_sales_price", Types.NUMERIC, 7, 2)
                .addColumn("wlin_coupon_amt", Types.NUMERIC, 7, 2)
                .addColumn("wlin_warehouse_id", Types.CHAR, 16)
                .addColumn("wlin_ship_date", Types.CHAR, 10)
                .addColumn("wlin_ship_cost", Types.NUMERIC, 7, 2)
                .addColumn("wlin_web_page_id", Types.CHAR, 16)
                .setSchema(schema));

        tables.put(S_STORE, Tables.get(S_STORE)
                .addColumn("stor_store_id", Types.CHAR, 1, false)
                .addColumn("stor_closed_date", Types.CHAR, 10)
                .addColumn("stor_name", Types.CHAR, 50)
                .addColumn("stor_employees", Types.INTEGER)
                .addColumn("stor_floor_space", Types.INTEGER)
                .addColumn("stor_hours", Types.CHAR, 20)
                .addColumn("stor_store_manager", Types.CHAR, 40)
                .addColumn("stor_market_id", Types.INTEGER)
                .addColumn("stor_geography_class", Types.CHAR, 100)
                .addColumn("stor_market_manager", Types.CHAR, 40)
                .addColumn("stor_tax_percentage", Types.NUMERIC, 5, 2)
                .setSchema(schema));

        tables.put(S_CALL_CENTER, Tables.get(S_CALL_CENTER)
                .addColumn("call_center_id", Types.CHAR, 16, false)
                .addColumn("call_open_date", Types.CHAR, 10)
                .addColumn("call_closed_date", Types.CHAR, 10)
                .addColumn("call_center_name", Types.CHAR, 50)
                .addColumn("call_center_class", Types.CHAR, 50)
                .addColumn("call_center_employees", Types.INTEGER)
                .addColumn("call_center_sq_ft", Types.INTEGER)
                .addColumn("call_center_hours", Types.CHAR, 20)
                .addColumn("call_center_manager", Types.CHAR, 40)
                .addColumn("call_center_tax_percentage", Types.NUMERIC, 7, 2)
                .setSchema(schema));


        tables.put(S_WEB_SITE, Tables.get(S_WEB_SITE)
                .addColumn("wsit_web_site_id", Types.CHAR, 16, false)
                .addColumn("wsit_open_date", Types.CHAR, 10)
                .addColumn("wsit_closed_date", Types.CHAR, 10)
                .addColumn("wsit_site_name", Types.CHAR, 50)
                .addColumn("wsit_site_class", Types.CHAR, 50)
                .addColumn("wsit_site_manager", Types.CHAR, 40)
                .addColumn("wsit_tax_percentage", Types.DECIMAL, 5, 2)
                .setSchema(schema));

        tables.put(S_WAREHOUSE, Tables.get(S_WAREHOUSE)
                .addColumn("wrhs_warehouse_id", Types.CHAR, 16, false)
                .addColumn("wrhs_warehouse_desc", Types.CHAR, 200)
                .addColumn("wrhs_warehouse_sq_ft", Types.INTEGER)
                .setSchema(schema));

        tables.put(S_WEB_PAGE, Tables.get(S_WEB_PAGE)
                .addColumn("wpag_web_page_id", Types.CHAR, 16, false)
                .addColumn("wpag_create_date", Types.CHAR, 10)
                .addColumn("wpag_access_date", Types.CHAR, 10)
                .addColumn("wpag_autogen_flag", Types.CHAR, 1)
                .addColumn("wpag_url", Types.CHAR, 100)
                .addColumn("wpag_type", Types.CHAR, 50)
                .addColumn("wpag_char_cnt", Types.INTEGER)
                .addColumn("wpag_link_cnt", Types.INTEGER)
                .addColumn("wpag_image_cnt", Types.INTEGER)
                .addColumn("wpag_max_ad_cnt", Types.INTEGER)
                .setSchema(schema));

        tables.put(S_PROMOTION, Tables.get(S_PROMOTION)
                .addColumn("prom_promotion_id", Types.CHAR, 16, false)
                .addColumn("prom_promotion_name", Types.CHAR, 30)
                .addColumn("prom_start_date", Types.CHAR, 10)
                .addColumn("prom_end_date", Types.CHAR, 10)
                .addColumn("prom_cost", Types.NUMERIC, 7, 2)
                .addColumn("prom_response_target", Types.CHAR, 1)
                .addColumn("prom_channel_dmail", Types.CHAR, 1)
                .addColumn("prom_channel_email", Types.CHAR, 1)
                .addColumn("prom_channel_catalog", Types.CHAR, 1)
                .addColumn("prom_channel_tv", Types.CHAR, 1)
                .addColumn("prom_channel_radio", Types.CHAR, 1)
                .addColumn("prom_channel_press", Types.CHAR, 1)
                .addColumn("prom_channel_event", Types.CHAR, 1)
                .addColumn("prom_channel_demo", Types.CHAR, 1)
                .addColumn("prom_channel_details", Types.CHAR, 100)
                .addColumn("prom_purpose", Types.CHAR, 15)
                .addColumn("prom_discount_active", Types.CHAR, 1)
                .addColumn("prom_discount_pct", Types.NUMERIC, 5, 2)
                .setSchema(schema));

        tables.put(S_STORE_RETURNS, Tables.get(S_STORE_RETURNS)
                .addColumn("sret_store_id", Types.CHAR, 16)
                .addColumn("sret_purchase_id", Types.CHAR, 16, false)
                .addColumn("sret_line_number", Types.INTEGER, false)
                .addColumn("sret_item_id", Types.CHAR, 16, false)
                .addColumn("sret_customer_id", Types.CHAR, 16)
                .addColumn("sret_return_date", Types.CHAR, 10)
                .addColumn("sret_return_time", Types.CHAR, 10)
                .addColumn("sret_ticket_number", Types.CHAR, 20)
                .addColumn("sret_return_qty", Types.INTEGER)
                .addColumn("sret_return_amt", Types.NUMERIC, 7, 2)
                .addColumn("sret_return_tax", Types.NUMERIC, 7, 2)
                .addColumn("sret_return_fee", Types.NUMERIC, 7, 2)
                .addColumn("sret_return_ship_cost", Types.NUMERIC, 7, 2)
                .addColumn("sret_refunded_cash", Types.NUMERIC, 7, 2)
                .addColumn("sret_reversed_charge", Types.NUMERIC, 7, 2)
                .addColumn("sret_store_credit", Types.NUMERIC, 7, 2)
                .addColumn("sret_reason_id", Types.CHAR, 16)
                .setSchema(schema));

        tables.put(S_CATALOG_RETURNS, Tables.get(S_CATALOG_RETURNS)
                .addColumn("cret_call_center_id", Types.CHAR, 16)
                .addColumn("cret_order_id", Types.INTEGER, false)
                .addColumn("cret_line_number", Types.INTEGER, false)
                .addColumn("cret_item_id", Types.CHAR, 16, false)
                .addColumn("cret_return_customer_id", Types.CHAR, 16)
                .addColumn("cret_refund_customer_id", Types.CHAR, 16)
                .addColumn("cret_return_date", Types.CHAR, 10)
                .addColumn("cret_return_time", Types.CHAR, 10)
                .addColumn("cret_return_qty", Types.INTEGER)
                .addColumn("cret_return_amt", Types.NUMERIC, 7, 2)
                .addColumn("cret_return_tax", Types.NUMERIC, 7, 2)
                .addColumn("cret_return_fee", Types.NUMERIC, 7, 2)
                .addColumn("cret_return_ship_cost", Types.NUMERIC, 7, 2)
                .addColumn("cret_refunded_cash", Types.NUMERIC, 7, 2)
                .addColumn("cret_reversed_charge", Types.NUMERIC, 7, 2)
                .addColumn("cret_merchant_credit", Types.NUMERIC, 7, 2)
                .addColumn("cret_reason_id", Types.CHAR, 16)
                .addColumn("cret_shipmode_id", Types.CHAR, 16)
                .addColumn("cret_catalog_page_id", Types.CHAR, 16)
                .addColumn("cret_warehouse_id", Types.CHAR, 16)
                .setSchema(schema));

        tables.put(S_WEB_RETURNS, Tables.get(S_WEB_RETURNS)
                .addColumn("wret_web_site_id", Types.CHAR, 16)
                .addColumn("wret_order_id", Types.INTEGER, false)
                .addColumn("wret_line_number", Types.INTEGER, false)
                .addColumn("wret_item_id", Types.CHAR, 16, false)
                .addColumn("wret_return_customer_id", Types.CHAR, 16)
                .addColumn("wret_refund_customer_id", Types.CHAR, 16)
                .addColumn("wret_return_date", Types.CHAR, 10)
                .addColumn("wret_return_time", Types.CHAR, 10)
                .addColumn("wret_return_qty", Types.INTEGER)
                .addColumn("wret_return_amt", Types.NUMERIC, 7, 2)
                .addColumn("wret_return_tax", Types.NUMERIC, 7, 2)
                .addColumn("wret_return_fee", Types.NUMERIC, 7, 2)
                .addColumn("wret_return_ship_cost", Types.NUMERIC, 7, 2)
                .addColumn("wret_refunded_cash", Types.NUMERIC, 7, 2)
                .addColumn("wret_reversed_charge", Types.NUMERIC, 7, 2)
                .addColumn("wret_account_credit", Types.NUMERIC, 7, 2)
                .addColumn("wret_reason_id", Types.CHAR, 16)
                .setSchema(schema));

        tables.put(S_INVENTORY, Tables.get(S_INVENTORY)
                .addColumn("invn_warehouse_id", Types.CHAR, 16, false)
                .addColumn("invn_item_id", Types.CHAR, 16, false)
                .addColumn("invn_date", Types.CHAR, 10, false)
                .addColumn("invn_qty_on_hand", Types.INTEGER)
                .setSchema(schema));


    }

    /**
     * @return all tables for a schema
     */
    public List<TableDef> getSchemaTables(String schemaName) {

        switch (schemaName) {
            case TPCDS_SCHEMA:
                return getTables();
            case TPCDS_SCHEMA_DWH:
                return getTables().stream()
                        .filter(s -> dwhTables.contains(s.getName()))
                        .collect(Collectors.toList());
            case TPCDS_SCHEMA_STG:
                return getTables().stream()
                        .filter(s -> stagingTables.contains(s.getName()))
                        .collect(Collectors.toList());
            case TPCDS_SCHEMA_STORE_SALES:
                return Tabulars.atomic(getTables().stream()
                        .filter(s -> storeSalesTables.contains(s.getName()))
                        .collect(Collectors.toList())
                );
            default:
                throw new RuntimeException("TPC-DS Schema Name (" + schemaName + ") is unknown");
        }


    }

    /**
     * @return all tables
     */
    public List<TableDef> getTables() {

        return new ArrayList<>(tables.values());


    }

    /**
     * @param tableName - one of the static constant field that represents a table name
     * @return - the definition of this table
     */
    public TableDef getDataPath(String tableName) {
        return tables.get(tableName);
    }

    @Override
    public List<TableDef> getTables(String... tableNames) {

        return Arrays.stream(tableNames).map(name -> tables.get(name)).collect(Collectors.toList());

    }


}

package com.tabulify.tpc;

import com.tabulify.connection.Connection;
import com.tabulify.model.SqlDataTypeAnsi;
import com.tabulify.sample.SchemaSample;
import com.tabulify.spi.DataPath;
import com.tabulify.spi.Tabulars;

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


  public static final List<String> DWH_TABLES = Arrays.asList(
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
  public static final List<String> storeSalesTables = Arrays.asList(
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
  /**
   * A utility field to old the number of tables
   */
  public static final int TOTAL_NUMBERS_OF_TABLES = 46;
  public static final int TOTAL_NUMBERS_OF_DWH_TABLES = 25;

  private final Connection connection;

  // A map containing all TPCDS tables
  private final Map<String, DataPath> tables = new HashMap<>();


  private TpcdsModel(Connection tpcConnection) {


    this.connection = tpcConnection;

    // case TPCDS_SCHEMA_DWH:
    buildDataWarehouseTables();
    assert tables.size() == TOTAL_NUMBERS_OF_DWH_TABLES : "Dwh Table size was not " + TOTAL_NUMBERS_OF_DWH_TABLES + " but " + tables.size();
    // TPCDS_SCHEMA_STG:
    buildStagingTables();
    assert tables.size() == TOTAL_NUMBERS_OF_TABLES;

  }


  /**
   * @param connection - the connection where to create the tables, it can be a {@link TpcConnection} or any {@link Connection}
   *                   <p>
   *                   Example:
   *                   <code>
   *                   List<DataPath> dataPaths = TpcdsModel.of(sqlConnection).getAndCreateDataPaths();
   *                   Tabulars.create(dataPaths)
   *                   </code>
   */
  public static TpcdsModel of(Connection connection) {
    return new TpcdsModel(connection);
  }


  /**
   * Add the datawarehouse table to the schema
   * in memory (The tables are not created against the database)
   * <p>
   * This is equivalent to the file (tpcds.sql)
   */
  void buildDataWarehouseTables() {


    createAndAddDataPath(DBGEN_VERSION)
      .getOrCreateRelationDef()
      .addColumn("dv_version", SqlDataTypeAnsi.CHARACTER_VARYING, 16)
      .addColumn("dv_create_date", SqlDataTypeAnsi.DATE)
      .addColumn("dv_create_time", SqlDataTypeAnsi.TIME)
      .addColumn("dv_cmdline_args", SqlDataTypeAnsi.CHARACTER_VARYING, 200);

    final DataPath customerAddress = createAndAddDataPath(CUSTOMER_ADDRESS)
      .getOrCreateRelationDef()
      .addColumn("ca_address_sk", SqlDataTypeAnsi.INTEGER, false)
      .addColumn("ca_address_id", SqlDataTypeAnsi.CHARACTER, 16, false)
      .addColumn("ca_street_number", SqlDataTypeAnsi.CHARACTER, 10)
      .addColumn("ca_street_name", SqlDataTypeAnsi.CHARACTER_VARYING, 60)
      .addColumn("ca_street_type", SqlDataTypeAnsi.CHARACTER, 15)
      .addColumn("ca_suite_number", SqlDataTypeAnsi.CHARACTER, 10)
      .addColumn("ca_city", SqlDataTypeAnsi.CHARACTER_VARYING, 60)
      .addColumn("ca_county", SqlDataTypeAnsi.CHARACTER_VARYING, 30)
      .addColumn("ca_state", SqlDataTypeAnsi.CHARACTER, 2)
      .addColumn("ca_zip", SqlDataTypeAnsi.CHARACTER, 10)
      .addColumn("ca_country", SqlDataTypeAnsi.CHARACTER_VARYING, 20)
      .addColumn("ca_gmt_offset", SqlDataTypeAnsi.DECIMAL, 5, 2)
      .addColumn("ca_location_type", SqlDataTypeAnsi.CHARACTER, 20)
      .setPrimaryKey("ca_address_sk")
      .addUniqueKey("ca_address_id")
      .getDataPath();

    final DataPath customerDemographics = createAndAddDataPath(CUSTOMER_DEMOGRAPHICS)
      .getOrCreateRelationDef()
      .addColumn("cd_demo_sk", SqlDataTypeAnsi.INTEGER, false)
      .addColumn("cd_gender", SqlDataTypeAnsi.CHARACTER, 1)
      .addColumn("cd_marital_status", SqlDataTypeAnsi.CHARACTER, 1)
      .addColumn("cd_education_status", SqlDataTypeAnsi.CHARACTER, 20)
      .addColumn("cd_purchase_estimate", SqlDataTypeAnsi.INTEGER)
      .addColumn("cd_credit_rating", SqlDataTypeAnsi.CHARACTER, 10)
      .addColumn("cd_dep_count", SqlDataTypeAnsi.INTEGER)
      .addColumn("cd_dep_employed_count", SqlDataTypeAnsi.INTEGER)
      .addColumn("cd_dep_college_count", SqlDataTypeAnsi.INTEGER)
      .setPrimaryKey("cd_demo_sk")
      .getDataPath();

    final DataPath dateDim = createAndAddDataPath(DATE_DIM)
      .getOrCreateRelationDef()
      .addColumn("d_date_sk", SqlDataTypeAnsi.INTEGER, false)
      .addColumn("d_date_id", SqlDataTypeAnsi.CHARACTER, 16, false)
      .addColumn("d_date", SqlDataTypeAnsi.DATE)
      .addColumn("d_month_seq", SqlDataTypeAnsi.INTEGER)
      .addColumn("d_week_seq", SqlDataTypeAnsi.INTEGER)
      .addColumn("d_quarter_seq", SqlDataTypeAnsi.INTEGER)
      .addColumn("d_year", SqlDataTypeAnsi.INTEGER)
      .addColumn("d_dow", SqlDataTypeAnsi.INTEGER)
      .addColumn("d_moy", SqlDataTypeAnsi.INTEGER)
      .addColumn("d_dom", SqlDataTypeAnsi.INTEGER)
      .addColumn("d_qoy", SqlDataTypeAnsi.INTEGER)
      .addColumn("d_fy_year", SqlDataTypeAnsi.INTEGER)
      .addColumn("d_fy_quarter_seq", SqlDataTypeAnsi.INTEGER)
      .addColumn("d_fy_week_seq", SqlDataTypeAnsi.INTEGER)
      .addColumn("d_day_name", SqlDataTypeAnsi.CHARACTER, 9)
      .addColumn("d_quarter_name", SqlDataTypeAnsi.CHARACTER, 6)
      .addColumn("d_holiday", SqlDataTypeAnsi.CHARACTER, 1)
      .addColumn("d_weekend", SqlDataTypeAnsi.CHARACTER, 1)
      .addColumn("d_following_holiday", SqlDataTypeAnsi.CHARACTER, 1)
      .addColumn("d_first_dom", SqlDataTypeAnsi.INTEGER)
      .addColumn("d_last_dom", SqlDataTypeAnsi.INTEGER)
      .addColumn("d_same_day_ly", SqlDataTypeAnsi.INTEGER)
      .addColumn("d_same_day_lq", SqlDataTypeAnsi.INTEGER)
      .addColumn("d_current_day", SqlDataTypeAnsi.CHARACTER, 1)
      .addColumn("d_current_week", SqlDataTypeAnsi.CHARACTER, 1)
      .addColumn("d_current_month", SqlDataTypeAnsi.CHARACTER, 1)
      .addColumn("d_current_quarter", SqlDataTypeAnsi.CHARACTER, 1)
      .addColumn("d_current_year", SqlDataTypeAnsi.CHARACTER, 1)
      .setPrimaryKey("d_date_sk")
      .addUniqueKey("d_date_id")
      .getDataPath();

    final DataPath warehouse = createAndAddDataPath(WAREHOUSE);
    warehouse.getOrCreateRelationDef()
      .addColumn("w_warehouse_sk", SqlDataTypeAnsi.INTEGER, false)
      .addColumn("w_warehouse_id", SqlDataTypeAnsi.CHARACTER, 16, false)
      .addColumn("w_warehouse_name", SqlDataTypeAnsi.CHARACTER_VARYING, 20)
      .addColumn("w_warehouse_sq_ft", SqlDataTypeAnsi.INTEGER)
      .addColumn("w_street_number", SqlDataTypeAnsi.CHARACTER, 10)
      .addColumn("w_street_name", SqlDataTypeAnsi.CHARACTER_VARYING, 60)
      .addColumn("w_street_type", SqlDataTypeAnsi.CHARACTER, 15)
      .addColumn("w_suite_number", SqlDataTypeAnsi.CHARACTER, 10)
      .addColumn("w_city", SqlDataTypeAnsi.CHARACTER_VARYING, 60)
      .addColumn("w_county", SqlDataTypeAnsi.CHARACTER_VARYING, 30)
      .addColumn("w_state", SqlDataTypeAnsi.CHARACTER, 2)
      .addColumn("w_zip", SqlDataTypeAnsi.CHARACTER, 10)
      .addColumn("w_country", SqlDataTypeAnsi.CHARACTER_VARYING, 20)
      .addColumn("w_gmt_offset", SqlDataTypeAnsi.DECIMAL, 5, 2)
      .setPrimaryKey("w_warehouse_sk")
      .addUniqueKey("w_warehouse_id")
      .getDataPath();


    final DataPath shipMode = createAndAddDataPath(SHIP_MODE)
      .getOrCreateRelationDef()
      .addColumn("sm_ship_mode_sk", SqlDataTypeAnsi.INTEGER, false)
      .addColumn("sm_ship_mode_id", SqlDataTypeAnsi.CHARACTER, 16, false)
      .addColumn("sm_type", SqlDataTypeAnsi.CHARACTER, 30)
      .addColumn("sm_code", SqlDataTypeAnsi.CHARACTER, 10)
      .addColumn("sm_carrier", SqlDataTypeAnsi.CHARACTER, 20)
      .addColumn("sm_contract", SqlDataTypeAnsi.CHARACTER, 20)
      .setPrimaryKey("sm_ship_mode_sk")
      .addUniqueKey("sm_ship_mode_id")
      .getDataPath();


    final DataPath timeDim = createAndAddDataPath(TIME_DIM)
      .getOrCreateRelationDef()
      .addColumn("t_time_sk", SqlDataTypeAnsi.INTEGER, false)
      .addColumn("t_time_id", SqlDataTypeAnsi.CHARACTER, 16, false)
      .addColumn("t_time", SqlDataTypeAnsi.INTEGER)
      .addColumn("t_hour", SqlDataTypeAnsi.INTEGER)
      .addColumn("t_minute", SqlDataTypeAnsi.INTEGER)
      .addColumn("t_second", SqlDataTypeAnsi.INTEGER)
      .addColumn("t_am_pm", SqlDataTypeAnsi.CHARACTER, 2)
      .addColumn("t_shift", SqlDataTypeAnsi.CHARACTER, 20)
      .addColumn("t_sub_shift", SqlDataTypeAnsi.CHARACTER, 20)
      .addColumn("t_meal_time", SqlDataTypeAnsi.CHARACTER, 20)
      .setPrimaryKey("t_time_sk")
      .addUniqueKey("t_time_id")
      .getDataPath();

    final DataPath reason = createAndAddDataPath(REASON)
      .getOrCreateRelationDef()
      .addColumn("r_reason_sk", SqlDataTypeAnsi.INTEGER, false)
      .addColumn("r_reason_id", SqlDataTypeAnsi.CHARACTER, 16, false)
      .addColumn("r_reason_desc", SqlDataTypeAnsi.CHARACTER, 100)
      .setPrimaryKey("r_reason_sk")
      .addUniqueKey("r_reason_id")
      .getDataPath();

    final DataPath incomeBand = createAndAddDataPath(INCOME_BAND)
      .getOrCreateRelationDef()
      .addColumn("ib_income_band_sk", SqlDataTypeAnsi.INTEGER, false)
      .addColumn("ib_lower_bound", SqlDataTypeAnsi.INTEGER)
      .addColumn("ib_upper_bound", SqlDataTypeAnsi.INTEGER)
      .setPrimaryKey("ib_income_band_sk")
      .getDataPath();

    final DataPath item = createAndAddDataPath(ITEM)
      .getOrCreateRelationDef()
      .addColumn("i_item_sk", SqlDataTypeAnsi.INTEGER, false)
      // business key that cannot be unique due to the history structure start/end date
      .addColumn("i_item_id", SqlDataTypeAnsi.CHARACTER, 16, false)
      .addColumn("i_rec_start_date", SqlDataTypeAnsi.DATE)
      .addColumn("i_rec_end_date", SqlDataTypeAnsi.DATE)
      .addColumn("i_item_desc", SqlDataTypeAnsi.CHARACTER_VARYING, 200)
      .addColumn("i_current_price", SqlDataTypeAnsi.DECIMAL, 7, 2)
      .addColumn("i_wholesale_cost", SqlDataTypeAnsi.DECIMAL, 7, 2)
      .addColumn("i_brand_id", SqlDataTypeAnsi.INTEGER)
      .addColumn("i_brand", SqlDataTypeAnsi.CHARACTER, 50)
      .addColumn("i_class_id", SqlDataTypeAnsi.INTEGER)
      .addColumn("i_class", SqlDataTypeAnsi.CHARACTER, 50)
      .addColumn("i_category_id", SqlDataTypeAnsi.INTEGER)
      .addColumn("i_category", SqlDataTypeAnsi.CHARACTER, 50)
      .addColumn("i_manufact_id", SqlDataTypeAnsi.INTEGER)
      .addColumn("i_manufact", SqlDataTypeAnsi.CHARACTER, 50)
      .addColumn("i_size", SqlDataTypeAnsi.CHARACTER, 20)
      .addColumn("i_formulation", SqlDataTypeAnsi.CHARACTER, 20)
      .addColumn("i_color", SqlDataTypeAnsi.CHARACTER, 20)
      .addColumn("i_units", SqlDataTypeAnsi.CHARACTER, 10)
      .addColumn("i_container", SqlDataTypeAnsi.CHARACTER, 10)
      .addColumn("i_manager_id", SqlDataTypeAnsi.INTEGER)
      .addColumn("i_product_name", SqlDataTypeAnsi.CHARACTER, 50)
      .setPrimaryKey("i_item_sk")
      .getDataPath();

    final DataPath store = createAndAddDataPath(STORE)
      .getOrCreateRelationDef()
      .addColumn("s_store_sk", SqlDataTypeAnsi.INTEGER, false)
      .addColumn("s_store_id", SqlDataTypeAnsi.CHARACTER, 16, false)
      .addColumn("s_rec_start_date", SqlDataTypeAnsi.DATE)
      .addColumn("s_rec_end_date", SqlDataTypeAnsi.DATE)
      .addColumn("s_closed_date_sk", SqlDataTypeAnsi.INTEGER)
      .addColumn("s_store_name", SqlDataTypeAnsi.CHARACTER_VARYING, 50)
      .addColumn("s_number_employees", SqlDataTypeAnsi.INTEGER)
      .addColumn("s_floor_space", SqlDataTypeAnsi.INTEGER)
      .addColumn("s_hours", SqlDataTypeAnsi.CHARACTER, 20)
      .addColumn("s_manager", SqlDataTypeAnsi.CHARACTER_VARYING, 40)
      .addColumn("s_market_id", SqlDataTypeAnsi.INTEGER)
      .addColumn("s_geography_class", SqlDataTypeAnsi.CHARACTER_VARYING, 100)
      .addColumn("s_market_desc", SqlDataTypeAnsi.CHARACTER_VARYING, 100)
      .addColumn("s_market_manager", SqlDataTypeAnsi.CHARACTER_VARYING, 40)
      .addColumn("s_division_id", SqlDataTypeAnsi.INTEGER)
      .addColumn("s_division_name", SqlDataTypeAnsi.CHARACTER_VARYING, 50)
      .addColumn("s_company_id", SqlDataTypeAnsi.INTEGER)
      .addColumn("s_company_name", SqlDataTypeAnsi.CHARACTER_VARYING, 50)
      .addColumn("s_street_number", SqlDataTypeAnsi.CHARACTER_VARYING, 10)
      .addColumn("s_street_name", SqlDataTypeAnsi.CHARACTER_VARYING, 60)
      .addColumn("s_street_type", SqlDataTypeAnsi.CHARACTER, 15)
      .addColumn("s_suite_number", SqlDataTypeAnsi.CHARACTER, 10)
      .addColumn("s_city", SqlDataTypeAnsi.CHARACTER_VARYING, 60)
      .addColumn("s_county", SqlDataTypeAnsi.CHARACTER_VARYING, 30)
      .addColumn("s_state", SqlDataTypeAnsi.CHARACTER, 2)
      .addColumn("s_zip", SqlDataTypeAnsi.CHARACTER, 10)
      .addColumn("s_country", SqlDataTypeAnsi.CHARACTER_VARYING, 20)
      .addColumn("s_gmt_offset", SqlDataTypeAnsi.DECIMAL, 5, 2)
      .addColumn("s_tax_precentage", SqlDataTypeAnsi.DECIMAL, 5, 2)
      .setPrimaryKey("s_store_sk")
      .addUniqueKey("s_store_id") // B (for business key)
      .addForeignKey(dateDim, "s_closed_date_sk")
      .getDataPath();

    final DataPath callCenter = createAndAddDataPath(CALL_CENTER)
      .getOrCreateRelationDef()
      .addColumn("cc_call_center_sk", SqlDataTypeAnsi.INTEGER, false)
      .addColumn("cc_call_center_id", SqlDataTypeAnsi.CHARACTER, 16, false)
      .addColumn("cc_rec_start_date", SqlDataTypeAnsi.DATE)
      .addColumn("cc_rec_end_date", SqlDataTypeAnsi.DATE)
      .addColumn("cc_closed_date_sk", SqlDataTypeAnsi.INTEGER)
      .addColumn("cc_open_date_sk", SqlDataTypeAnsi.INTEGER)
      .addColumn("cc_name", SqlDataTypeAnsi.CHARACTER_VARYING, 50)
      .addColumn("cc_class", SqlDataTypeAnsi.CHARACTER_VARYING, 50)
      .addColumn("cc_employees", SqlDataTypeAnsi.INTEGER)
      .addColumn("cc_sq_ft", SqlDataTypeAnsi.INTEGER)
      .addColumn("cc_hours", SqlDataTypeAnsi.CHARACTER, 20)
      .addColumn("cc_manager", SqlDataTypeAnsi.CHARACTER_VARYING, 40)
      .addColumn("cc_mkt_id", SqlDataTypeAnsi.INTEGER)
      .addColumn("cc_mkt_class", SqlDataTypeAnsi.CHARACTER, 50)
      .addColumn("cc_mkt_desc", SqlDataTypeAnsi.CHARACTER_VARYING, 100)
      .addColumn("cc_market_manager", SqlDataTypeAnsi.CHARACTER_VARYING, 40)
      .addColumn("cc_division", SqlDataTypeAnsi.INTEGER)
      .addColumn("cc_division_name", SqlDataTypeAnsi.CHARACTER_VARYING, 50)
      .addColumn("cc_company", SqlDataTypeAnsi.INTEGER)
      .addColumn("cc_company_name", SqlDataTypeAnsi.CHARACTER, 50)
      .addColumn("cc_street_number", SqlDataTypeAnsi.CHARACTER, 10)
      .addColumn("cc_street_name", SqlDataTypeAnsi.CHARACTER_VARYING, 60)
      .addColumn("cc_street_type", SqlDataTypeAnsi.CHARACTER, 15)
      .addColumn("cc_suite_number", SqlDataTypeAnsi.CHARACTER, 10)
      .addColumn("cc_city", SqlDataTypeAnsi.CHARACTER_VARYING, 60)
      .addColumn("cc_county", SqlDataTypeAnsi.CHARACTER_VARYING, 30)
      .addColumn("cc_state", SqlDataTypeAnsi.CHARACTER, 2)
      .addColumn("cc_zip", SqlDataTypeAnsi.CHARACTER, 10)
      .addColumn("cc_country", SqlDataTypeAnsi.CHARACTER_VARYING, 20)
      .addColumn("cc_gmt_offset", SqlDataTypeAnsi.DECIMAL, 5, 2)
      .addColumn("cc_tax_percentage", SqlDataTypeAnsi.DECIMAL, 5, 2)
      .setPrimaryKey("cc_call_center_sk")
      .addUniqueKey("cc_call_center_id") // B for business column
      .addForeignKey(dateDim, "cc_closed_date_sk")
      .addForeignKey(dateDim, "cc_open_date_sk")
      .getDataPath();

    final DataPath householdDemographics = createAndAddDataPath(HOUSEHOLD_DEMOGRAPHICS)
      .getOrCreateRelationDef()
      .addColumn("hd_demo_sk", SqlDataTypeAnsi.INTEGER, false)
      .addColumn("hd_income_band_sk", SqlDataTypeAnsi.INTEGER)
      .addColumn("hd_buy_potential", SqlDataTypeAnsi.CHARACTER, 15)
      .addColumn("hd_dep_count", SqlDataTypeAnsi.INTEGER)
      .addColumn("hd_vehicle_count", SqlDataTypeAnsi.INTEGER)
      .setPrimaryKey("hd_demo_sk")
      .addForeignKey(incomeBand, "hd_income_band_sk")
      .getDataPath();

    final DataPath customer = createAndAddDataPath(CUSTOMER)
      .getOrCreateRelationDef()
      .addColumn("c_customer_sk", SqlDataTypeAnsi.INTEGER, false)
      .addColumn("c_customer_id", SqlDataTypeAnsi.CHARACTER, 16, false)
      .addColumn("c_current_cdemo_sk", SqlDataTypeAnsi.INTEGER)
      .addColumn("c_current_hdemo_sk", SqlDataTypeAnsi.INTEGER)
      .addColumn("c_current_addr_sk", SqlDataTypeAnsi.INTEGER)
      .addColumn("c_first_shipto_date_sk", SqlDataTypeAnsi.INTEGER)
      .addColumn("c_first_sales_date_sk", SqlDataTypeAnsi.INTEGER)
      .addColumn("c_salutation", SqlDataTypeAnsi.CHARACTER, 10)
      .addColumn("c_first_name", SqlDataTypeAnsi.CHARACTER, 20)
      .addColumn("c_last_name", SqlDataTypeAnsi.CHARACTER, 30)
      .addColumn("c_preferred_cust_flag", SqlDataTypeAnsi.CHARACTER, 1)
      .addColumn("c_birth_day", SqlDataTypeAnsi.INTEGER)
      .addColumn("c_birth_month", SqlDataTypeAnsi.INTEGER)
      .addColumn("c_birth_year", SqlDataTypeAnsi.INTEGER)
      .addColumn("c_birth_country", SqlDataTypeAnsi.CHARACTER_VARYING, 20)
      .addColumn("c_login", SqlDataTypeAnsi.CHARACTER, 13)
      .addColumn("c_email_address", SqlDataTypeAnsi.CHARACTER, 50)
      .addColumn("c_last_review_date_sk", SqlDataTypeAnsi.INTEGER, 10)
      .setPrimaryKey("c_customer_sk")
      .addUniqueKey("c_customer_id") // B column (for business column)
      .addForeignKey(customerDemographics, "c_current_cdemo_sk")
      .addForeignKey(householdDemographics, "c_current_hdemo_sk")
      .addForeignKey(customerAddress, "c_current_addr_sk")
      .addForeignKey(dateDim, "c_first_shipto_date_sk")
      .addForeignKey(dateDim, "c_first_sales_date_sk")
      .addForeignKey(dateDim, "c_last_review_date_sk")
      .getDataPath();

    final DataPath webSite = createAndAddDataPath(WEB_SITE)
      .getOrCreateRelationDef()
      .addColumn("web_site_sk", SqlDataTypeAnsi.INTEGER, false)
      .addColumn("web_site_id", SqlDataTypeAnsi.CHARACTER, 16)
      .addColumn("web_rec_start_date", SqlDataTypeAnsi.DATE)
      .addColumn("web_rec_end_date", SqlDataTypeAnsi.DATE)
      .addColumn("web_name", SqlDataTypeAnsi.CHARACTER_VARYING, 50)
      .addColumn("web_open_date_sk", SqlDataTypeAnsi.INTEGER)
      .addColumn("web_close_date_sk", SqlDataTypeAnsi.INTEGER)
      .addColumn("web_class", SqlDataTypeAnsi.CHARACTER_VARYING, 50)
      .addColumn("web_manager", SqlDataTypeAnsi.CHARACTER_VARYING, 40)
      .addColumn("web_mkt_id", SqlDataTypeAnsi.INTEGER)
      .addColumn("web_mkt_class", SqlDataTypeAnsi.CHARACTER_VARYING, 50)
      .addColumn("web_mkt_desc", SqlDataTypeAnsi.CHARACTER_VARYING, 100)
      .addColumn("web_market_manager", SqlDataTypeAnsi.CHARACTER_VARYING, 40)
      .addColumn("web_company_id", SqlDataTypeAnsi.INTEGER)
      .addColumn("web_company_name", SqlDataTypeAnsi.CHARACTER, 50)
      .addColumn("web_street_number", SqlDataTypeAnsi.CHARACTER, 10)
      .addColumn("web_street_name", SqlDataTypeAnsi.CHARACTER_VARYING, 60)
      .addColumn("web_street_type", SqlDataTypeAnsi.CHARACTER, 15)
      .addColumn("web_suite_number", SqlDataTypeAnsi.CHARACTER, 10)
      .addColumn("web_city", SqlDataTypeAnsi.CHARACTER_VARYING, 60)
      .addColumn("web_county", SqlDataTypeAnsi.CHARACTER_VARYING, 30)
      .addColumn("web_state", SqlDataTypeAnsi.CHARACTER, 2)
      .addColumn("web_zip", SqlDataTypeAnsi.CHARACTER, 10)
      .addColumn("web_country", SqlDataTypeAnsi.CHARACTER_VARYING, 20)
      .addColumn("web_gmt_offset", SqlDataTypeAnsi.DECIMAL, 5, 2)
      .addColumn("web_tax_percentage", SqlDataTypeAnsi.DECIMAL, 5, 2)
      .setPrimaryKey("web_site_sk")
      .addUniqueKey("web_site_id")  // b for business key
      .addForeignKey(dateDim, "web_open_date_sk")
      .addForeignKey(dateDim, "web_close_date_sk")
      .getDataPath();

    DataPath promotion = createAndAddDataPath(PROMOTION)
      .getOrCreateRelationDef()
      .addColumn("p_promo_sk", SqlDataTypeAnsi.INTEGER, false)
      .addColumn("p_promo_id", SqlDataTypeAnsi.CHARACTER, 16, false)
      .addColumn("p_start_date_sk", SqlDataTypeAnsi.INTEGER)
      .addColumn("p_end_date_sk", SqlDataTypeAnsi.INTEGER)
      .addColumn("p_item_sk", SqlDataTypeAnsi.INTEGER)
      .addColumn("p_cost", SqlDataTypeAnsi.DECIMAL, 15, 2)
      .addColumn("p_response_target", SqlDataTypeAnsi.INTEGER)
      .addColumn("p_promo_name", SqlDataTypeAnsi.CHARACTER, 50)
      .addColumn("p_channel_dmail", SqlDataTypeAnsi.CHARACTER, 1)
      .addColumn("p_channel_email", SqlDataTypeAnsi.CHARACTER, 1)
      .addColumn("p_channel_catalog", SqlDataTypeAnsi.CHARACTER, 1)
      .addColumn("p_channel_tv", SqlDataTypeAnsi.CHARACTER, 1)
      .addColumn("p_channel_radio", SqlDataTypeAnsi.CHARACTER, 1)
      .addColumn("p_channel_press", SqlDataTypeAnsi.CHARACTER, 1)
      .addColumn("p_channel_event", SqlDataTypeAnsi.CHARACTER, 1)
      .addColumn("p_channel_demo", SqlDataTypeAnsi.CHARACTER, 1)
      .addColumn("p_channel_details", SqlDataTypeAnsi.CHARACTER_VARYING, 100)
      .addColumn("p_purpose", SqlDataTypeAnsi.CHARACTER, 15)
      .addColumn("p_discount_active", SqlDataTypeAnsi.CHARACTER, 1)
      .setPrimaryKey("p_promo_sk")
      .addUniqueKey("p_promo_id") // b for business key
      .addForeignKey(dateDim, "p_start_date_sk")
      .addForeignKey(dateDim, "p_end_date_sk")
      .addForeignKey(item, "p_item_sk")
      .getDataPath();

    DataPath storeSales = createAndAddDataPath(STORE_SALES)
      .getOrCreateRelationDef()
      .addColumn("ss_sold_date_sk", SqlDataTypeAnsi.INTEGER)
      .addColumn("ss_sold_time_sk", SqlDataTypeAnsi.INTEGER)
      .addColumn("ss_item_sk", SqlDataTypeAnsi.INTEGER, false)
      .addColumn("ss_customer_sk", SqlDataTypeAnsi.INTEGER)
      .addColumn("ss_cdemo_sk", SqlDataTypeAnsi.INTEGER)
      .addColumn("ss_hdemo_sk", SqlDataTypeAnsi.INTEGER)
      .addColumn("ss_addr_sk", SqlDataTypeAnsi.INTEGER)
      .addColumn("ss_store_sk", SqlDataTypeAnsi.INTEGER)
      .addColumn("ss_promo_sk", SqlDataTypeAnsi.INTEGER)
      .addColumn("ss_ticket_number", SqlDataTypeAnsi.INTEGER, false)
      .addColumn("ss_quantity", SqlDataTypeAnsi.INTEGER)
      .addColumn("ss_wholesale_cost", SqlDataTypeAnsi.DECIMAL, 7, 2)
      .addColumn("ss_list_price", SqlDataTypeAnsi.DECIMAL, 7, 2)
      .addColumn("ss_sales_price", SqlDataTypeAnsi.DECIMAL, 7, 2)
      .addColumn("ss_ext_discount_amt", SqlDataTypeAnsi.DECIMAL, 7, 2)
      .addColumn("ss_ext_sales_price", SqlDataTypeAnsi.DECIMAL, 7, 2)
      .addColumn("ss_ext_wholesale_cost", SqlDataTypeAnsi.DECIMAL, 7, 2)
      .addColumn("ss_ext_list_price", SqlDataTypeAnsi.DECIMAL, 7, 2)
      .addColumn("ss_ext_tax", SqlDataTypeAnsi.DECIMAL, 7, 2)
      .addColumn("ss_coupon_amt", SqlDataTypeAnsi.DECIMAL, 7, 2)
      .addColumn("ss_net_paid", SqlDataTypeAnsi.DECIMAL, 7, 2)
      .addColumn("ss_net_paid_inc_tax", SqlDataTypeAnsi.DECIMAL, 7, 2)
      .addColumn("ss_net_profit", SqlDataTypeAnsi.DECIMAL, 7, 2)
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
      .getDataPath();

    createAndAddDataPath(STORE_RETURNS)
      .getOrCreateRelationDef()
      .addColumn("sr_returned_date_sk", SqlDataTypeAnsi.INTEGER)
      .addColumn("sr_return_time_sk", SqlDataTypeAnsi.INTEGER)
      .addColumn("sr_item_sk", SqlDataTypeAnsi.INTEGER, false)
      .addColumn("sr_customer_sk", SqlDataTypeAnsi.INTEGER)
      .addColumn("sr_cdemo_sk", SqlDataTypeAnsi.INTEGER)
      .addColumn("sr_hdemo_sk", SqlDataTypeAnsi.INTEGER)
      .addColumn("sr_addr_sk", SqlDataTypeAnsi.INTEGER)
      .addColumn("sr_store_sk", SqlDataTypeAnsi.INTEGER)
      .addColumn("sr_reason_sk", SqlDataTypeAnsi.INTEGER)
      .addColumn("sr_ticket_number", SqlDataTypeAnsi.INTEGER, false)
      .addColumn("sr_return_quantity", SqlDataTypeAnsi.INTEGER)
      .addColumn("sr_return_amt", SqlDataTypeAnsi.DECIMAL, 7, 2)
      .addColumn("sr_return_tax", SqlDataTypeAnsi.DECIMAL, 7, 2)
      .addColumn("sr_return_amt_inc_tax", SqlDataTypeAnsi.DECIMAL, 7, 2)
      .addColumn("sr_fee", SqlDataTypeAnsi.DECIMAL, 7, 2)
      .addColumn("sr_return_ship_cost", SqlDataTypeAnsi.DECIMAL, 7, 2)
      .addColumn("sr_refunded_cash", SqlDataTypeAnsi.DECIMAL, 7, 2)
      .addColumn("sr_reversed_charge", SqlDataTypeAnsi.DECIMAL, 7, 2)
      .addColumn("sr_store_credit", SqlDataTypeAnsi.DECIMAL, 7, 2)
      .addColumn("sr_net_loss", SqlDataTypeAnsi.DECIMAL, 7, 2)
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
      .addForeignKey(reason, "sr_reason_sk");

    DataPath webPage = createAndAddDataPath(WEB_PAGE)
      .getOrCreateRelationDef()
      .addColumn("wp_web_page_sk", SqlDataTypeAnsi.INTEGER, false)
      .addColumn("wp_web_page_id", SqlDataTypeAnsi.CHARACTER, 16, false)
      .addColumn("wp_rec_start_date", SqlDataTypeAnsi.DATE)
      .addColumn("wp_rec_end_date", SqlDataTypeAnsi.DATE)
      .addColumn("wp_creation_date_sk", SqlDataTypeAnsi.INTEGER)
      .addColumn("wp_access_date_sk", SqlDataTypeAnsi.INTEGER)
      .addColumn("wp_autogen_flag", SqlDataTypeAnsi.CHARACTER, 1)
      .addColumn("wp_customer_sk", SqlDataTypeAnsi.INTEGER)
      .addColumn("wp_url", SqlDataTypeAnsi.CHARACTER_VARYING, 100)
      .addColumn("wp_type", SqlDataTypeAnsi.CHARACTER, 50)
      .addColumn("wp_char_count", SqlDataTypeAnsi.INTEGER)
      .addColumn("wp_link_count", SqlDataTypeAnsi.INTEGER)
      .addColumn("wp_image_count", SqlDataTypeAnsi.INTEGER)
      .addColumn("wp_max_ad_count", SqlDataTypeAnsi.INTEGER)
      .setPrimaryKey("wp_web_page_sk")
      .addUniqueKey("wp_web_page_id")
      .addForeignKey(dateDim, "wp_creation_date_sk")
      .addForeignKey(dateDim, "wp_access_date_sk")
      .addForeignKey(customer, "wp_customer_sk")
      .getDataPath();

    DataPath catalogPage = createAndAddDataPath(CATALOG_PAGE)
      .getOrCreateRelationDef()
      .addColumn("cp_catalog_page_sk", SqlDataTypeAnsi.INTEGER, false)
      .addColumn("cp_catalog_page_id", SqlDataTypeAnsi.CHARACTER, 16, false)
      .addColumn("cp_start_date_sk", SqlDataTypeAnsi.INTEGER)
      .addColumn("cp_end_date_sk", SqlDataTypeAnsi.INTEGER)
      .addColumn("cp_department", SqlDataTypeAnsi.CHARACTER_VARYING, 50)
      .addColumn("cp_catalog_number", SqlDataTypeAnsi.INTEGER)
      .addColumn("cp_catalog_page_number", SqlDataTypeAnsi.INTEGER)
      .addColumn("cp_description", SqlDataTypeAnsi.CHARACTER_VARYING, 100)
      .addColumn("cp_type", SqlDataTypeAnsi.CHARACTER_VARYING, 100)
      .setPrimaryKey("cp_catalog_page_sk")
      .addUniqueKey("cp_catalog_page_id") // B for business column
      .addForeignKey(dateDim, "cp_start_date_sk")
      .addForeignKey(dateDim, "cp_end_date_sk")
      .getDataPath();

    createAndAddDataPath(INVENTORY)
      .getOrCreateRelationDef()
      .addColumn("inv_date_sk", SqlDataTypeAnsi.INTEGER, false)
      .addColumn("inv_item_sk", SqlDataTypeAnsi.INTEGER, false)
      .addColumn("inv_warehouse_sk", SqlDataTypeAnsi.INTEGER, false)
      .addColumn("inv_quantity_on_hand", SqlDataTypeAnsi.INTEGER)
      .setPrimaryKey("inv_date_sk", "inv_item_sk", "inv_warehouse_sk")
      .addForeignKey(dateDim, "inv_date_sk")
      .addForeignKey(item, "inv_item_sk")
      .addForeignKey(warehouse, "inv_warehouse_sk");

    DataPath catalogSales = createAndAddDataPath(CATALOG_SALES)
      .getOrCreateRelationDef()
      .addColumn("cs_sold_date_sk", SqlDataTypeAnsi.INTEGER)
      .addColumn("cs_sold_time_sk", SqlDataTypeAnsi.INTEGER)
      .addColumn("cs_ship_date_sk", SqlDataTypeAnsi.INTEGER)
      .addColumn("cs_bill_customer_sk", SqlDataTypeAnsi.INTEGER)
      .addColumn("cs_bill_cdemo_sk", SqlDataTypeAnsi.INTEGER)
      .addColumn("cs_bill_hdemo_sk", SqlDataTypeAnsi.INTEGER)
      .addColumn("cs_bill_addr_sk", SqlDataTypeAnsi.INTEGER)
      .addColumn("cs_ship_customer_sk", SqlDataTypeAnsi.INTEGER)
      .addColumn("cs_ship_cdemo_sk", SqlDataTypeAnsi.INTEGER)
      .addColumn("cs_ship_hdemo_sk", SqlDataTypeAnsi.INTEGER)
      .addColumn("cs_ship_addr_sk", SqlDataTypeAnsi.INTEGER)
      .addColumn("cs_call_center_sk", SqlDataTypeAnsi.INTEGER)
      .addColumn("cs_catalog_page_sk", SqlDataTypeAnsi.INTEGER)
      .addColumn("cs_ship_mode_sk", SqlDataTypeAnsi.INTEGER)
      .addColumn("cs_warehouse_sk", SqlDataTypeAnsi.INTEGER)
      .addColumn("cs_item_sk", SqlDataTypeAnsi.INTEGER, false)
      .addColumn("cs_promo_sk", SqlDataTypeAnsi.INTEGER)
      .addColumn("cs_order_number", SqlDataTypeAnsi.INTEGER, false)
      .addColumn("cs_quantity", SqlDataTypeAnsi.INTEGER)
      .addColumn("cs_wholesale_cost", SqlDataTypeAnsi.DECIMAL, 7, 2)
      .addColumn("cs_list_price", SqlDataTypeAnsi.DECIMAL, 7, 2)
      .addColumn("cs_sales_price", SqlDataTypeAnsi.DECIMAL, 7, 2)
      .addColumn("cs_ext_discount_amt", SqlDataTypeAnsi.DECIMAL, 7, 2)
      .addColumn("cs_ext_sales_price", SqlDataTypeAnsi.DECIMAL, 7, 2)
      .addColumn("cs_ext_wholesale_cost", SqlDataTypeAnsi.DECIMAL, 7, 2)
      .addColumn("cs_ext_list_price", SqlDataTypeAnsi.DECIMAL, 7, 2)
      .addColumn("cs_ext_tax", SqlDataTypeAnsi.DECIMAL, 7, 2)
      .addColumn("cs_coupon_amt", SqlDataTypeAnsi.DECIMAL, 7, 2)
      .addColumn("cs_ext_ship_cost", SqlDataTypeAnsi.DECIMAL, 7, 2)
      .addColumn("cs_net_paid", SqlDataTypeAnsi.DECIMAL, 7, 2)
      .addColumn("cs_net_paid_inc_tax", SqlDataTypeAnsi.DECIMAL, 7, 2)
      .addColumn("cs_net_paid_inc_ship", SqlDataTypeAnsi.DECIMAL, 7, 2)
      .addColumn("cs_net_paid_inc_ship_tax", SqlDataTypeAnsi.DECIMAL, 7, 2)
      .addColumn("cs_net_profit", SqlDataTypeAnsi.DECIMAL, 7, 2)
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
      .getDataPath();

    createAndAddDataPath(CATALOG_RETURNS)
      .getOrCreateRelationDef()
      .addColumn("cr_returned_date_sk", SqlDataTypeAnsi.INTEGER)
      .addColumn("cr_returned_time_sk", SqlDataTypeAnsi.INTEGER)
      .addColumn("cr_item_sk", SqlDataTypeAnsi.INTEGER, false)
      .addColumn("cr_refunded_customer_sk", SqlDataTypeAnsi.INTEGER)
      .addColumn("cr_refunded_cdemo_sk", SqlDataTypeAnsi.INTEGER)
      .addColumn("cr_refunded_hdemo_sk", SqlDataTypeAnsi.INTEGER)
      .addColumn("cr_refunded_addr_sk", SqlDataTypeAnsi.INTEGER)
      .addColumn("cr_returning_customer_sk", SqlDataTypeAnsi.INTEGER)
      .addColumn("cr_returning_cdemo_sk", SqlDataTypeAnsi.INTEGER)
      .addColumn("cr_returning_hdemo_sk", SqlDataTypeAnsi.INTEGER)
      .addColumn("cr_returning_addr_sk", SqlDataTypeAnsi.INTEGER)
      .addColumn("cr_call_center_sk", SqlDataTypeAnsi.INTEGER)
      .addColumn("cr_catalog_page_sk", SqlDataTypeAnsi.INTEGER)
      .addColumn("cr_ship_mode_sk", SqlDataTypeAnsi.INTEGER)
      .addColumn("cr_warehouse_sk", SqlDataTypeAnsi.INTEGER)
      .addColumn("cr_reason_sk", SqlDataTypeAnsi.INTEGER)
      .addColumn("cr_order_number", SqlDataTypeAnsi.INTEGER, false)
      .addColumn("cr_return_quantity", SqlDataTypeAnsi.INTEGER)
      .addColumn("cr_return_amount", SqlDataTypeAnsi.DECIMAL, 7, 2)
      .addColumn("cr_return_tax", SqlDataTypeAnsi.DECIMAL, 7, 2)
      .addColumn("cr_return_amt_inc_tax", SqlDataTypeAnsi.DECIMAL, 7, 2)
      .addColumn("cr_fee", SqlDataTypeAnsi.DECIMAL, 7, 2)
      .addColumn("cr_return_ship_cost", SqlDataTypeAnsi.DECIMAL, 7, 2)
      .addColumn("cr_refunded_cash", SqlDataTypeAnsi.DECIMAL, 7, 2)
      .addColumn("cr_reversed_charge", SqlDataTypeAnsi.DECIMAL, 7, 2)
      .addColumn("cr_store_credit", SqlDataTypeAnsi.DECIMAL, 7, 2)
      .addColumn("cr_net_loss", SqlDataTypeAnsi.DECIMAL, 7, 2)
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
      .addForeignKey(reason, "cr_reason_sk");

    DataPath webSales = createAndAddDataPath(WEB_SALES)
      .getOrCreateRelationDef()
      .addColumn("ws_sold_date_sk", SqlDataTypeAnsi.INTEGER)
      .addColumn("ws_sold_time_sk", SqlDataTypeAnsi.INTEGER)
      .addColumn("ws_ship_date_sk", SqlDataTypeAnsi.INTEGER)
      .addColumn("ws_item_sk", SqlDataTypeAnsi.INTEGER, false)
      .addColumn("ws_bill_customer_sk", SqlDataTypeAnsi.INTEGER)
      .addColumn("ws_bill_cdemo_sk", SqlDataTypeAnsi.INTEGER)
      .addColumn("ws_bill_hdemo_sk", SqlDataTypeAnsi.INTEGER)
      .addColumn("ws_bill_addr_sk", SqlDataTypeAnsi.INTEGER)
      .addColumn("ws_ship_customer_sk", SqlDataTypeAnsi.INTEGER)
      .addColumn("ws_ship_cdemo_sk", SqlDataTypeAnsi.INTEGER)
      .addColumn("ws_ship_hdemo_sk", SqlDataTypeAnsi.INTEGER)
      .addColumn("ws_ship_addr_sk", SqlDataTypeAnsi.INTEGER)
      .addColumn("ws_web_page_sk", SqlDataTypeAnsi.INTEGER)
      .addColumn("ws_web_site_sk", SqlDataTypeAnsi.INTEGER)
      .addColumn("ws_ship_mode_sk", SqlDataTypeAnsi.INTEGER)
      .addColumn("ws_warehouse_sk", SqlDataTypeAnsi.INTEGER)
      .addColumn("ws_promo_sk", SqlDataTypeAnsi.INTEGER)
      .addColumn("ws_order_number", SqlDataTypeAnsi.INTEGER, false)
      .addColumn("ws_quantity", SqlDataTypeAnsi.INTEGER)
      .addColumn("ws_wholesale_cost", SqlDataTypeAnsi.DECIMAL, 7, 2)
      .addColumn("ws_list_price", SqlDataTypeAnsi.DECIMAL, 7, 2)
      .addColumn("ws_sales_price", SqlDataTypeAnsi.DECIMAL, 7, 2)
      .addColumn("ws_ext_discount_amt", SqlDataTypeAnsi.DECIMAL, 7, 2)
      .addColumn("ws_ext_sales_price", SqlDataTypeAnsi.DECIMAL, 7, 2)
      .addColumn("ws_ext_wholesale_cost", SqlDataTypeAnsi.DECIMAL, 7, 2)
      .addColumn("ws_ext_list_price", SqlDataTypeAnsi.DECIMAL, 7, 2)
      .addColumn("ws_ext_tax", SqlDataTypeAnsi.DECIMAL, 7, 2)
      .addColumn("ws_coupon_amt", SqlDataTypeAnsi.DECIMAL, 7, 2)
      .addColumn("ws_ext_ship_cost", SqlDataTypeAnsi.DECIMAL, 7, 2)
      .addColumn("ws_net_paid", SqlDataTypeAnsi.DECIMAL, 7, 2)
      .addColumn("ws_net_paid_inc_tax", SqlDataTypeAnsi.DECIMAL, 7, 2)
      .addColumn("ws_net_paid_inc_ship", SqlDataTypeAnsi.DECIMAL, 7, 2)
      .addColumn("ws_net_paid_inc_ship_tax", SqlDataTypeAnsi.DECIMAL, 7, 2)
      .addColumn("ws_net_profit", SqlDataTypeAnsi.DECIMAL, 7, 2)
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
      .getDataPath();

    createAndAddDataPath(WEB_RETURNS)
      .getOrCreateRelationDef()
      .addColumn("wr_returned_date_sk", SqlDataTypeAnsi.INTEGER)
      .addColumn("wr_returned_time_sk", SqlDataTypeAnsi.INTEGER)
      .addColumn("wr_item_sk", SqlDataTypeAnsi.INTEGER, false)
      .addColumn("wr_refunded_customer_sk", SqlDataTypeAnsi.INTEGER)
      .addColumn("wr_refunded_cdemo_sk", SqlDataTypeAnsi.INTEGER)
      .addColumn("wr_refunded_hdemo_sk", SqlDataTypeAnsi.INTEGER)
      .addColumn("wr_refunded_addr_sk", SqlDataTypeAnsi.INTEGER)
      .addColumn("wr_returning_customer_sk", SqlDataTypeAnsi.INTEGER)
      .addColumn("wr_returning_cdemo_sk", SqlDataTypeAnsi.INTEGER)
      .addColumn("wr_returning_hdemo_sk", SqlDataTypeAnsi.INTEGER)
      .addColumn("wr_returning_addr_sk", SqlDataTypeAnsi.INTEGER)
      .addColumn("wr_web_page_sk", SqlDataTypeAnsi.INTEGER)
      .addColumn("wr_reason_sk", SqlDataTypeAnsi.INTEGER)
      .addColumn("wr_order_number", SqlDataTypeAnsi.INTEGER, false)
      .addColumn("wr_return_quantity", SqlDataTypeAnsi.INTEGER)
      .addColumn("wr_return_amt", SqlDataTypeAnsi.DECIMAL, 7, 2)
      .addColumn("wr_return_tax", SqlDataTypeAnsi.DECIMAL, 7, 2)
      .addColumn("wr_return_amt_inc_tax", SqlDataTypeAnsi.DECIMAL, 7, 2)
      .addColumn("wr_fee", SqlDataTypeAnsi.DECIMAL, 7, 2)
      .addColumn("wr_return_ship_cost", SqlDataTypeAnsi.DECIMAL, 7, 2)
      .addColumn("wr_refunded_cash", SqlDataTypeAnsi.DECIMAL, 7, 2)
      .addColumn("wr_reversed_charge", SqlDataTypeAnsi.DECIMAL, 7, 2)
      .addColumn("wr_account_credit", SqlDataTypeAnsi.DECIMAL, 7, 2)
      .addColumn("wr_net_loss", SqlDataTypeAnsi.DECIMAL, 7, 2)
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
      .addForeignKey(reason, "wr_reason_sk");

  }

  /**
   * @param resourceName - the resource name
   */
  private DataPath createAndAddDataPath(String resourceName) {
    DataPath dataPath;
    if (this.connection instanceof TpcConnection) {
      dataPath = TpcDataPath.of((TpcConnection) this.connection, resourceName);
    } else {
      dataPath = this.connection.getDataPath(resourceName);
    }
    tables.put(dataPath.getName(), dataPath);
    return dataPath;

  }


  /**
   * Build the source table/staging tables.
   * The tables are build in the given schema. If the schema is null,
   * it will be in the schema chosen during initialization of the tpcds application.
   * <p>
   * These are the tables of the file tpcds_source.sql
   */
  void buildStagingTables() {

    createAndAddDataPath(S_CATALOG_PAGE)
      .getOrCreateRelationDef()
      .addColumn("cpag_catalog_number", SqlDataTypeAnsi.INTEGER, false)
      .addColumn("cpag_catalog_page_number", SqlDataTypeAnsi.INTEGER, false)
      .addColumn("cpag_department", SqlDataTypeAnsi.CHARACTER, 20)
      .addColumn("cpag_id", SqlDataTypeAnsi.CHARACTER, 16)
      .addColumn("cpag_start_date", SqlDataTypeAnsi.CHARACTER, 10)
      .addColumn("cpag_end_date", SqlDataTypeAnsi.CHARACTER, 10)
      .addColumn("cpag_description", SqlDataTypeAnsi.CHARACTER_VARYING, 100)
      .addColumn("cpag_type", SqlDataTypeAnsi.CHARACTER_VARYING, 100);


    createAndAddDataPath(S_ZIP_TO_GMT)
      .getOrCreateRelationDef()
      .addColumn("zipg_zip", SqlDataTypeAnsi.CHARACTER, 5, false)
      .addColumn("zipg_gmt_offset", SqlDataTypeAnsi.INTEGER, false);

    createAndAddDataPath(S_PURCHASE_LINEITEM)
      .getOrCreateRelationDef()
      .addColumn("plin_purchase_id", SqlDataTypeAnsi.INTEGER, false)
      .addColumn("plin_line_number", SqlDataTypeAnsi.INTEGER, false)
      .addColumn("plin_item_id", SqlDataTypeAnsi.CHARACTER, 16)
      .addColumn("plin_promotion_id", SqlDataTypeAnsi.CHARACTER, 16)
      .addColumn("plin_quantity", SqlDataTypeAnsi.INTEGER)
      .addColumn("plin_sale_price", SqlDataTypeAnsi.NUMERIC, 7, 2)
      .addColumn("plin_coupon_amt", SqlDataTypeAnsi.NUMERIC, 7, 2)
      .addColumn("plin_comment", SqlDataTypeAnsi.CHARACTER_VARYING, 100);

    createAndAddDataPath(S_CUSTOMER)
      .getOrCreateRelationDef()
      .addColumn("cust_customer_id", SqlDataTypeAnsi.CHARACTER, 16, false)
      .addColumn("cust_salutation", SqlDataTypeAnsi.CHARACTER, 10)
      .addColumn("cust_last_name", SqlDataTypeAnsi.CHARACTER, 20)
      .addColumn("cust_first_name", SqlDataTypeAnsi.CHARACTER, 20)
      .addColumn("cust_preffered_flag", SqlDataTypeAnsi.CHARACTER, 1)
      .addColumn("cust_birth_date", SqlDataTypeAnsi.CHARACTER, 10)
      .addColumn("cust_birth_country", SqlDataTypeAnsi.CHARACTER, 20)
      .addColumn("cust_login_id", SqlDataTypeAnsi.CHARACTER, 13)
      .addColumn("cust_email_address", SqlDataTypeAnsi.CHARACTER, 50)
      .addColumn("cust_last_login_chg_date", SqlDataTypeAnsi.CHARACTER, 10)
      .addColumn("cust_first_shipto_date", SqlDataTypeAnsi.CHARACTER, 10)
      .addColumn("cust_first_purchase_date", SqlDataTypeAnsi.CHARACTER, 10)
      .addColumn("cust_last_review_date", SqlDataTypeAnsi.CHARACTER, 10)
      .addColumn("cust_primary_machine_id", SqlDataTypeAnsi.CHARACTER, 15)
      .addColumn("cust_secondary_machine_id", SqlDataTypeAnsi.CHARACTER, 15)
      .addColumn("cust_street_number", SqlDataTypeAnsi.SMALLINT)
      .addColumn("cust_suite_number", SqlDataTypeAnsi.CHARACTER, 10)
      .addColumn("cust_street_name1", SqlDataTypeAnsi.CHARACTER, 30)
      .addColumn("cust_street_name2", SqlDataTypeAnsi.CHARACTER, 30)
      .addColumn("cust_street_type", SqlDataTypeAnsi.CHARACTER, 15)
      .addColumn("cust_city", SqlDataTypeAnsi.CHARACTER, 60)
      .addColumn("cust_zip", SqlDataTypeAnsi.CHARACTER, 10)
      .addColumn("cust_county", SqlDataTypeAnsi.CHARACTER, 30)
      .addColumn("cust_state", SqlDataTypeAnsi.CHARACTER, 2)
      .addColumn("cust_country", SqlDataTypeAnsi.CHARACTER, 20)
      .addColumn("cust_loc_type", SqlDataTypeAnsi.CHARACTER, 20)
      .addColumn("cust_gender", SqlDataTypeAnsi.CHARACTER, 1)
      .addColumn("cust_marital_status", SqlDataTypeAnsi.CHARACTER, 1)
      .addColumn("cust_educ_status", SqlDataTypeAnsi.CHARACTER, 20)
      .addColumn("cust_credit_rating", SqlDataTypeAnsi.CHARACTER, 10)
      .addColumn("cust_purch_est", SqlDataTypeAnsi.NUMERIC, 7, 2)
      .addColumn("cust_buy_potential", SqlDataTypeAnsi.CHARACTER, 15)
      .addColumn("cust_depend_cnt", SqlDataTypeAnsi.SMALLINT)
      .addColumn("cust_depend_emp_cnt", SqlDataTypeAnsi.SMALLINT)
      .addColumn("cust_depend_college_cnt", SqlDataTypeAnsi.SMALLINT)
      .addColumn("cust_vehicle_cnt", SqlDataTypeAnsi.SMALLINT)
      .addColumn("cust_annual_income", SqlDataTypeAnsi.NUMERIC, 9, 2);


    createAndAddDataPath(S_CUSTOMER_ADDRESS)
      .getOrCreateRelationDef()
      .addColumn("cadr_address_id", SqlDataTypeAnsi.CHARACTER, 16, false)
      .addColumn("cadr_street_number", SqlDataTypeAnsi.INTEGER)
      .addColumn("cadr_street_name1", SqlDataTypeAnsi.CHARACTER, 25)
      .addColumn("cadr_street_name2", SqlDataTypeAnsi.CHARACTER, 25)
      .addColumn("cadr_street_type", SqlDataTypeAnsi.CHARACTER, 15)
      .addColumn("cadr_suitnumber", SqlDataTypeAnsi.CHARACTER, 10)
      .addColumn("cadr_city", SqlDataTypeAnsi.CHARACTER, 60)
      .addColumn("cadr_county", SqlDataTypeAnsi.CHARACTER, 30)
      .addColumn("cadr_state", SqlDataTypeAnsi.CHARACTER, 2)
      .addColumn("cadr_zip", SqlDataTypeAnsi.CHARACTER, 10)
      .addColumn("cadr_country", SqlDataTypeAnsi.CHARACTER, 20);

    createAndAddDataPath(S_PURCHASE)
      .getOrCreateRelationDef()
      .addColumn("purc_purchase_id", SqlDataTypeAnsi.INTEGER, false)
      .addColumn("purc_store_id", SqlDataTypeAnsi.CHARACTER, 16)
      .addColumn("purc_customer_id", SqlDataTypeAnsi.CHARACTER, 16)
      .addColumn("purc_purchase_date", SqlDataTypeAnsi.CHARACTER, 10)
      .addColumn("purc_purchase_time", SqlDataTypeAnsi.INTEGER)
      .addColumn("purc_register_id", SqlDataTypeAnsi.INTEGER)
      .addColumn("purc_clerk_id", SqlDataTypeAnsi.INTEGER)
      .addColumn("purc_comment", SqlDataTypeAnsi.CHARACTER, 100);

    createAndAddDataPath(S_CATALOG_ORDER)
      .getOrCreateRelationDef()
      .addColumn("cord_order_id", SqlDataTypeAnsi.INTEGER, false)
      .addColumn("cord_bill_customer_id", SqlDataTypeAnsi.CHARACTER, 16)
      .addColumn("cord_ship_customer_id", SqlDataTypeAnsi.CHARACTER, 16)
      .addColumn("cord_order_date", SqlDataTypeAnsi.CHARACTER, 10)
      .addColumn("cord_order_time", SqlDataTypeAnsi.INTEGER)
      .addColumn("cord_ship_mode_id", SqlDataTypeAnsi.CHARACTER, 16)
      .addColumn("cord_call_center_id", SqlDataTypeAnsi.CHARACTER, 16)
      .addColumn("cord_order_comments", SqlDataTypeAnsi.CHARACTER_VARYING, 100);

    createAndAddDataPath(S_WEB_ORDER)
      .getOrCreateRelationDef()
      .addColumn("word_order_id", SqlDataTypeAnsi.INTEGER, false)
      .addColumn("word_bill_customer_id", SqlDataTypeAnsi.CHARACTER, 16)
      .addColumn("word_ship_customer_id", SqlDataTypeAnsi.CHARACTER, 16)
      .addColumn("word_order_date", SqlDataTypeAnsi.CHARACTER, 10)
      .addColumn("word_order_time", SqlDataTypeAnsi.INTEGER)
      .addColumn("word_ship_mode_id", SqlDataTypeAnsi.CHARACTER, 16)
      .addColumn("word_web_site_id", SqlDataTypeAnsi.CHARACTER, 16)
      .addColumn("word_order_comments", SqlDataTypeAnsi.CHARACTER, 100);

    createAndAddDataPath(S_ITEM)
      .getOrCreateRelationDef()
      .addColumn("item_item_id", SqlDataTypeAnsi.CHARACTER, 16, false)
      .addColumn("item_item_description", SqlDataTypeAnsi.CHARACTER, 200)
      .addColumn("item_list_price", SqlDataTypeAnsi.NUMERIC, 7, 2)
      .addColumn("item_wholesale_cost", SqlDataTypeAnsi.NUMERIC, 7, 2)
      .addColumn("item_size", SqlDataTypeAnsi.CHARACTER, 20)
      .addColumn("item_formulation", SqlDataTypeAnsi.CHARACTER, 20)
      .addColumn("item_color", SqlDataTypeAnsi.CHARACTER, 20)
      .addColumn("item_units", SqlDataTypeAnsi.CHARACTER, 10)
      .addColumn("item_container", SqlDataTypeAnsi.CHARACTER, 10)
      .addColumn("item_manager_id", SqlDataTypeAnsi.INTEGER)
    ;

    DataPath sCataLogOrderLineItem = createAndAddDataPath(S_CATALOG_ORDER_LINEITEM);
    sCataLogOrderLineItem.getOrCreateRelationDef()
      .addColumn("clin_order_id", SqlDataTypeAnsi.INTEGER, false)
      .addColumn("clin_line_number", SqlDataTypeAnsi.INTEGER, false)
      .addColumn("clin_item_id", SqlDataTypeAnsi.CHARACTER, 16)
      .addColumn("clin_promotion_id", SqlDataTypeAnsi.CHARACTER, 16)
      .addColumn("clin_quantity", SqlDataTypeAnsi.INTEGER)
      .addColumn("clin_sales_price", SqlDataTypeAnsi.NUMERIC, 7, 2)
      .addColumn("clin_coupon_amt", SqlDataTypeAnsi.NUMERIC, 7, 2)
      .addColumn("clin_warehouse_id", SqlDataTypeAnsi.CHARACTER, 16)
      .addColumn("clin_ship_date", SqlDataTypeAnsi.CHARACTER, 10)
      .addColumn("clin_catalog_number", SqlDataTypeAnsi.INTEGER)
      .addColumn("clin_catalog_page_number", SqlDataTypeAnsi.INTEGER)
      .addColumn("clin_ship_cost", SqlDataTypeAnsi.NUMERIC, 7, 2);

    createAndAddDataPath(S_WEB_ORDER_LINEITEM)
      .getOrCreateRelationDef()
      .addColumn("wlin_order_id", SqlDataTypeAnsi.INTEGER, false)
      .addColumn("wlin_line_number", SqlDataTypeAnsi.INTEGER, false)
      .addColumn("wlin_item_id", SqlDataTypeAnsi.CHARACTER, 16)
      .addColumn("wlin_promotion_id", SqlDataTypeAnsi.CHARACTER, 16)
      .addColumn("wlin_quantity", SqlDataTypeAnsi.INTEGER)
      .addColumn("wlin_sales_price", SqlDataTypeAnsi.NUMERIC, 7, 2)
      .addColumn("wlin_coupon_amt", SqlDataTypeAnsi.NUMERIC, 7, 2)
      .addColumn("wlin_warehouse_id", SqlDataTypeAnsi.CHARACTER, 16)
      .addColumn("wlin_ship_date", SqlDataTypeAnsi.CHARACTER, 10)
      .addColumn("wlin_ship_cost", SqlDataTypeAnsi.NUMERIC, 7, 2)
      .addColumn("wlin_web_page_id", SqlDataTypeAnsi.CHARACTER, 16)
    ;


    createAndAddDataPath(S_STORE)
      .getOrCreateRelationDef()
      .addColumn("stor_store_id", SqlDataTypeAnsi.CHARACTER, 1, false)
      .addColumn("stor_closed_date", SqlDataTypeAnsi.CHARACTER, 10)
      .addColumn("stor_name", SqlDataTypeAnsi.CHARACTER, 50)
      .addColumn("stor_employees", SqlDataTypeAnsi.INTEGER)
      .addColumn("stor_floor_space", SqlDataTypeAnsi.INTEGER)
      .addColumn("stor_hours", SqlDataTypeAnsi.CHARACTER, 20)
      .addColumn("stor_store_manager", SqlDataTypeAnsi.CHARACTER, 40)
      .addColumn("stor_market_id", SqlDataTypeAnsi.INTEGER)
      .addColumn("stor_geography_class", SqlDataTypeAnsi.CHARACTER, 100)
      .addColumn("stor_market_manager", SqlDataTypeAnsi.CHARACTER, 40)
      .addColumn("stor_tax_percentage", SqlDataTypeAnsi.NUMERIC, 5, 2)
    ;

    createAndAddDataPath(S_CALL_CENTER)
      .getOrCreateRelationDef()
      .addColumn("call_center_id", SqlDataTypeAnsi.CHARACTER, 16, false)
      .addColumn("call_open_date", SqlDataTypeAnsi.CHARACTER, 10)
      .addColumn("call_closed_date", SqlDataTypeAnsi.CHARACTER, 10)
      .addColumn("call_center_name", SqlDataTypeAnsi.CHARACTER, 50)
      .addColumn("call_center_class", SqlDataTypeAnsi.CHARACTER, 50)
      .addColumn("call_center_employees", SqlDataTypeAnsi.INTEGER)
      .addColumn("call_center_sq_ft", SqlDataTypeAnsi.INTEGER)
      .addColumn("call_center_hours", SqlDataTypeAnsi.CHARACTER, 20)
      .addColumn("call_center_manager", SqlDataTypeAnsi.CHARACTER, 40)
      .addColumn("call_center_tax_percentage", SqlDataTypeAnsi.NUMERIC, 7, 2);


    createAndAddDataPath(S_WEB_SITE)
      .getOrCreateRelationDef()
      .addColumn("wsit_web_site_id", SqlDataTypeAnsi.CHARACTER, 16, false)
      .addColumn("wsit_open_date", SqlDataTypeAnsi.CHARACTER, 10)
      .addColumn("wsit_closed_date", SqlDataTypeAnsi.CHARACTER, 10)
      .addColumn("wsit_site_name", SqlDataTypeAnsi.CHARACTER, 50)
      .addColumn("wsit_site_class", SqlDataTypeAnsi.CHARACTER, 50)
      .addColumn("wsit_site_manager", SqlDataTypeAnsi.CHARACTER, 40)
      .addColumn("wsit_tax_percentage", SqlDataTypeAnsi.DECIMAL, 5, 2);

    createAndAddDataPath(S_WAREHOUSE)
      .getOrCreateRelationDef()
      .addColumn("wrhs_warehouse_id", SqlDataTypeAnsi.CHARACTER, 16, false)
      .addColumn("wrhs_warehouse_desc", SqlDataTypeAnsi.CHARACTER, 200)
      .addColumn("wrhs_warehouse_sq_ft", SqlDataTypeAnsi.INTEGER)
    ;

    createAndAddDataPath(S_WEB_PAGE)
      .getOrCreateRelationDef()
      .addColumn("wpag_web_page_id", SqlDataTypeAnsi.CHARACTER, 16, false)
      .addColumn("wpag_create_date", SqlDataTypeAnsi.CHARACTER, 10)
      .addColumn("wpag_access_date", SqlDataTypeAnsi.CHARACTER, 10)
      .addColumn("wpag_autogen_flag", SqlDataTypeAnsi.CHARACTER, 1)
      .addColumn("wpag_url", SqlDataTypeAnsi.CHARACTER, 100)
      .addColumn("wpag_type", SqlDataTypeAnsi.CHARACTER, 50)
      .addColumn("wpag_char_cnt", SqlDataTypeAnsi.INTEGER)
      .addColumn("wpag_link_cnt", SqlDataTypeAnsi.INTEGER)
      .addColumn("wpag_image_cnt", SqlDataTypeAnsi.INTEGER)
      .addColumn("wpag_max_ad_cnt", SqlDataTypeAnsi.INTEGER);

    createAndAddDataPath(S_PROMOTION)
      .getOrCreateRelationDef()
      .addColumn("prom_promotion_id", SqlDataTypeAnsi.CHARACTER, 16, false)
      .addColumn("prom_promotion_name", SqlDataTypeAnsi.CHARACTER, 30)
      .addColumn("prom_start_date", SqlDataTypeAnsi.CHARACTER, 10)
      .addColumn("prom_end_date", SqlDataTypeAnsi.CHARACTER, 10)
      .addColumn("prom_cost", SqlDataTypeAnsi.NUMERIC, 7, 2)
      .addColumn("prom_response_target", SqlDataTypeAnsi.CHARACTER, 1)
      .addColumn("prom_channel_dmail", SqlDataTypeAnsi.CHARACTER, 1)
      .addColumn("prom_channel_email", SqlDataTypeAnsi.CHARACTER, 1)
      .addColumn("prom_channel_catalog", SqlDataTypeAnsi.CHARACTER, 1)
      .addColumn("prom_channel_tv", SqlDataTypeAnsi.CHARACTER, 1)
      .addColumn("prom_channel_radio", SqlDataTypeAnsi.CHARACTER, 1)
      .addColumn("prom_channel_press", SqlDataTypeAnsi.CHARACTER, 1)
      .addColumn("prom_channel_event", SqlDataTypeAnsi.CHARACTER, 1)
      .addColumn("prom_channel_demo", SqlDataTypeAnsi.CHARACTER, 1)
      .addColumn("prom_channel_details", SqlDataTypeAnsi.CHARACTER, 100)
      .addColumn("prom_purpose", SqlDataTypeAnsi.CHARACTER, 15)
      .addColumn("prom_discount_active", SqlDataTypeAnsi.CHARACTER, 1)
      .addColumn("prom_discount_pct", SqlDataTypeAnsi.NUMERIC, 5, 2);

    createAndAddDataPath(S_STORE_RETURNS)
      .getOrCreateRelationDef()
      .addColumn("sret_store_id", SqlDataTypeAnsi.CHARACTER, 16)
      .addColumn("sret_purchase_id", SqlDataTypeAnsi.CHARACTER, 16, false)
      .addColumn("sret_line_number", SqlDataTypeAnsi.INTEGER, false)
      .addColumn("sret_item_id", SqlDataTypeAnsi.CHARACTER, 16, false)
      .addColumn("sret_customer_id", SqlDataTypeAnsi.CHARACTER, 16)
      .addColumn("sret_return_date", SqlDataTypeAnsi.CHARACTER, 10)
      .addColumn("sret_return_time", SqlDataTypeAnsi.CHARACTER, 10)
      .addColumn("sret_ticket_number", SqlDataTypeAnsi.CHARACTER, 20)
      .addColumn("sret_return_qty", SqlDataTypeAnsi.INTEGER)
      .addColumn("sret_return_amt", SqlDataTypeAnsi.NUMERIC, 7, 2)
      .addColumn("sret_return_tax", SqlDataTypeAnsi.NUMERIC, 7, 2)
      .addColumn("sret_return_fee", SqlDataTypeAnsi.NUMERIC, 7, 2)
      .addColumn("sret_return_ship_cost", SqlDataTypeAnsi.NUMERIC, 7, 2)
      .addColumn("sret_refunded_cash", SqlDataTypeAnsi.NUMERIC, 7, 2)
      .addColumn("sret_reversed_charge", SqlDataTypeAnsi.NUMERIC, 7, 2)
      .addColumn("sret_store_credit", SqlDataTypeAnsi.NUMERIC, 7, 2)
      .addColumn("sret_reason_id", SqlDataTypeAnsi.CHARACTER, 16);

    createAndAddDataPath(S_CATALOG_RETURNS)
      .getOrCreateRelationDef()
      .addColumn("cret_call_center_id", SqlDataTypeAnsi.CHARACTER, 16)
      .addColumn("cret_order_id", SqlDataTypeAnsi.INTEGER, false)
      .addColumn("cret_line_number", SqlDataTypeAnsi.INTEGER, false)
      .addColumn("cret_item_id", SqlDataTypeAnsi.CHARACTER, 16, false)
      .addColumn("cret_return_customer_id", SqlDataTypeAnsi.CHARACTER, 16)
      .addColumn("cret_refund_customer_id", SqlDataTypeAnsi.CHARACTER, 16)
      .addColumn("cret_return_date", SqlDataTypeAnsi.CHARACTER, 10)
      .addColumn("cret_return_time", SqlDataTypeAnsi.CHARACTER, 10)
      .addColumn("cret_return_qty", SqlDataTypeAnsi.INTEGER)
      .addColumn("cret_return_amt", SqlDataTypeAnsi.NUMERIC, 7, 2)
      .addColumn("cret_return_tax", SqlDataTypeAnsi.NUMERIC, 7, 2)
      .addColumn("cret_return_fee", SqlDataTypeAnsi.NUMERIC, 7, 2)
      .addColumn("cret_return_ship_cost", SqlDataTypeAnsi.NUMERIC, 7, 2)
      .addColumn("cret_refunded_cash", SqlDataTypeAnsi.NUMERIC, 7, 2)
      .addColumn("cret_reversed_charge", SqlDataTypeAnsi.NUMERIC, 7, 2)
      .addColumn("cret_merchant_credit", SqlDataTypeAnsi.NUMERIC, 7, 2)
      .addColumn("cret_reason_id", SqlDataTypeAnsi.CHARACTER, 16)
      .addColumn("cret_shipmode_id", SqlDataTypeAnsi.CHARACTER, 16)
      .addColumn("cret_catalog_page_id", SqlDataTypeAnsi.CHARACTER, 16)
      .addColumn("cret_warehouse_id", SqlDataTypeAnsi.CHARACTER, 16)
    ;

    createAndAddDataPath(S_WEB_RETURNS)
      .getOrCreateRelationDef()
      .addColumn("wret_web_site_id", SqlDataTypeAnsi.CHARACTER, 16)
      .addColumn("wret_order_id", SqlDataTypeAnsi.INTEGER, false)
      .addColumn("wret_line_number", SqlDataTypeAnsi.INTEGER, false)
      .addColumn("wret_item_id", SqlDataTypeAnsi.CHARACTER, 16, false)
      .addColumn("wret_return_customer_id", SqlDataTypeAnsi.CHARACTER, 16)
      .addColumn("wret_refund_customer_id", SqlDataTypeAnsi.CHARACTER, 16)
      .addColumn("wret_return_date", SqlDataTypeAnsi.CHARACTER, 10)
      .addColumn("wret_return_time", SqlDataTypeAnsi.CHARACTER, 10)
      .addColumn("wret_return_qty", SqlDataTypeAnsi.INTEGER)
      .addColumn("wret_return_amt", SqlDataTypeAnsi.NUMERIC, 7, 2)
      .addColumn("wret_return_tax", SqlDataTypeAnsi.NUMERIC, 7, 2)
      .addColumn("wret_return_fee", SqlDataTypeAnsi.NUMERIC, 7, 2)
      .addColumn("wret_return_ship_cost", SqlDataTypeAnsi.NUMERIC, 7, 2)
      .addColumn("wret_refunded_cash", SqlDataTypeAnsi.NUMERIC, 7, 2)
      .addColumn("wret_reversed_charge", SqlDataTypeAnsi.NUMERIC, 7, 2)
      .addColumn("wret_account_credit", SqlDataTypeAnsi.NUMERIC, 7, 2)
      .addColumn("wret_reason_id", SqlDataTypeAnsi.CHARACTER, 16)
    ;

    createAndAddDataPath(S_INVENTORY)
      .getOrCreateRelationDef()
      .addColumn("invn_warehouse_id", SqlDataTypeAnsi.CHARACTER, 16, false)
      .addColumn("invn_item_id", SqlDataTypeAnsi.CHARACTER, 16, false)
      .addColumn("invn_date", SqlDataTypeAnsi.CHARACTER, 10, false)
      .addColumn("invn_qty_on_hand", SqlDataTypeAnsi.INTEGER);


  }


  /**
   * @return all tables for a schema
   */
  public List<DataPath> getSchemaTables(String schemaName) {

    switch (schemaName) {
      case TPCDS_SCHEMA:
        return createDataPaths();
      case TPCDS_SCHEMA_DWH:
        return createDataPaths().stream()
          .filter(s -> DWH_TABLES.contains(s.getName()))
          .collect(Collectors.toList());
      case TPCDS_SCHEMA_STG:
        return createDataPaths().stream()
          .filter(s -> stagingTables.contains(s.getName()))
          .collect(Collectors.toList());
      case TPCDS_SCHEMA_STORE_SALES:
        return Tabulars.atomic(createDataPaths().stream()
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
  public List<DataPath> createDataPaths() {

    return new ArrayList<>(tables.values());


  }

  /**
   * @param tableName - one of the static constant field that represents a table name
   * @return - the definition of this table
   */
  public DataPath getAndCreateDataPath(String tableName) {

    return tables.get(tableName);
  }

  @Override
  public List<DataPath> getDataPaths(String... tableNames) {

    return Arrays.stream(tableNames).map(name -> tables.get(name)).collect(Collectors.toList());

  }

  /**
   * Staging table data generation is not yet supported
   */
  public static boolean isStagingTable(DataPath dataPath) {
    return dataPath.getName().startsWith("s_");
  }


}

--------------------------------------------------------
--  File created - donderdag-april-05-2012   
--------------------------------------------------------
--------------------------------------------------------
--  DDL for Table H_CUSTOMER
--------------------------------------------------------

  CREATE TABLE "H_CUSTOMER" ("C_CUSTKEY" NUMBER, "C_NAME" VARCHAR2(25), "C_ADDRESS" VARCHAR2(40), "C_NATIONKEY" NUMBER, "C_PHONE" CHAR(15), "C_ACCTBAL" NUMBER, "C_MKTSEGMENT" CHAR(10), "C_COMMENT" VARCHAR2(117))
--------------------------------------------------------
--  DDL for Table H_LINEITEM
--------------------------------------------------------

  CREATE TABLE "H_LINEITEM" ("L_ORDERKEY" NUMBER, "L_PARTKEY" NUMBER, "L_SUPPKEY" NUMBER, "L_LINENUMBER" NUMBER, "L_QUANTITY" NUMBER, "L_EXTENDEDPRICE" NUMBER, "L_DISCOUNT" NUMBER, "L_TAX" NUMBER, "L_RETURNFLAG" CHAR(1), "L_LINESTATUS" CHAR(1), "L_SHIPDATE" DATE, "L_COMMITDATE" DATE, "L_RECEIPTDATE" DATE, "L_SHIPINSTRUCT" CHAR(25), "L_SHIPMODE" CHAR(10), "L_COMMENT" VARCHAR2(44))
--------------------------------------------------------
--  DDL for Table H_NATION
--------------------------------------------------------

  CREATE TABLE "H_NATION" ("N_NATIONKEY" NUMBER, "N_NAME" CHAR(25), "N_REGIONKEY" NUMBER, "N_COMMENT" VARCHAR2(152))
--------------------------------------------------------
--  DDL for Table H_ORDER
--------------------------------------------------------

  CREATE TABLE "H_ORDER" ("O_ORDERKEY" NUMBER, "O_CUSTKEY" NUMBER, "O_ORDERSTATUS" CHAR(1), "O_TOTALPRICE" NUMBER, "O_ORDERDATE" DATE, "O_ORDERPRIORITY" CHAR(15), "O_CLERK" CHAR(15), "O_SHIPPRIORITY" NUMBER, "O_COMMENT" VARCHAR2(79))
--------------------------------------------------------
--  DDL for Table H_PART
--------------------------------------------------------

  CREATE TABLE "H_PART" ("P_PARTKEY" NUMBER, "P_NAME" VARCHAR2(55), "P_MFGR" CHAR(25), "P_BRAND" CHAR(10), "P_TYPE" VARCHAR2(25), "P_SIZE" NUMBER, "P_CONTAINER" CHAR(10), "P_RETAILPRICE" NUMBER, "P_COMMENT" VARCHAR2(23))
--------------------------------------------------------
--  DDL for Table H_PARTSUPP
--------------------------------------------------------

  CREATE TABLE "H_PARTSUPP" ("PS_PARTKEY" NUMBER, "PS_SUPPKEY" NUMBER, "PS_AVAILQTY" NUMBER, "PS_SUPPLYCOST" NUMBER, "PS_COMMENT" VARCHAR2(199))
--------------------------------------------------------
--  DDL for Table H_REGION
--------------------------------------------------------

  CREATE TABLE "H_REGION" ("R_REGIONKEY" NUMBER, "R_NAME" CHAR(25), "R_COMMENT" VARCHAR2(152))
--------------------------------------------------------
--  DDL for Table H_SUPPLIER
--------------------------------------------------------

  CREATE TABLE "H_SUPPLIER" ("S_SUPPKEY" NUMBER, "S_NAME" CHAR(25), "S_ADDRESS" VARCHAR2(40), "S_NATIONKEY" NUMBER, "S_PHONE" CHAR(15), "S_ACCTBAL" NUMBER, "S_COMMENT" VARCHAR2(101))
--------------------------------------------------------
--  DDL for Index H_CUSTOMER_IDX1
--------------------------------------------------------

  CREATE UNIQUE INDEX "H_CUSTOMER_IDX1" ON "H_CUSTOMER" ("C_CUSTKEY")
--------------------------------------------------------
--  DDL for Index H_LINEITEM_IDX1
--------------------------------------------------------

  CREATE INDEX "H_LINEITEM_IDX1" ON "H_LINEITEM" ("L_ORDERKEY")
--------------------------------------------------------
--  DDL for Index H_ORDERS_IDX1
--------------------------------------------------------

  CREATE UNIQUE INDEX "H_ORDERS_IDX1" ON "H_ORDER" ("O_ORDERKEY")
--------------------------------------------------------
--  DDL for Index H_PARTSUPP_IDX1
--------------------------------------------------------

  CREATE UNIQUE INDEX "H_PARTSUPP_IDX1" ON "H_PARTSUPP" ("PS_PARTKEY", "PS_SUPPKEY")
--------------------------------------------------------
--  Constraints for Table H_CUSTOMER
--------------------------------------------------------

  ALTER TABLE "H_CUSTOMER" MODIFY ("C_CUSTKEY" NOT NULL ENABLE)
--------------------------------------------------------
--  Constraints for Table H_LINEITEM
--------------------------------------------------------

  ALTER TABLE "H_LINEITEM" MODIFY ("L_ORDERKEY" NOT NULL ENABLE)
 
  ALTER TABLE "H_LINEITEM" MODIFY ("L_PARTKEY" NOT NULL ENABLE)
 
  ALTER TABLE "H_LINEITEM" MODIFY ("L_SUPPKEY" NOT NULL ENABLE)
 
  ALTER TABLE "H_LINEITEM" MODIFY ("L_LINENUMBER" NOT NULL ENABLE)
 
  ALTER TABLE "H_LINEITEM" MODIFY ("L_QUANTITY" NOT NULL ENABLE)
 
  ALTER TABLE "H_LINEITEM" MODIFY ("L_EXTENDEDPRICE" NOT NULL ENABLE)
 
  ALTER TABLE "H_LINEITEM" MODIFY ("L_DISCOUNT" NOT NULL ENABLE)
 
  ALTER TABLE "H_LINEITEM" MODIFY ("L_TAX" NOT NULL ENABLE)
--------------------------------------------------------
--  Constraints for Table H_NATION
--------------------------------------------------------

  ALTER TABLE "H_NATION" MODIFY ("N_NATIONKEY" NOT NULL ENABLE)
--------------------------------------------------------
--  Constraints for Table H_ORDER
--------------------------------------------------------

  ALTER TABLE "H_ORDER" MODIFY ("O_ORDERKEY" NOT NULL ENABLE)
 
  ALTER TABLE "H_ORDER" MODIFY ("O_CUSTKEY" NOT NULL ENABLE)
--------------------------------------------------------
--  Constraints for Table H_PART
--------------------------------------------------------

  ALTER TABLE "H_PART" MODIFY ("P_PARTKEY" NOT NULL ENABLE)
--------------------------------------------------------
--  Constraints for Table H_PARTSUPP
--------------------------------------------------------

  ALTER TABLE "H_PARTSUPP" MODIFY ("PS_PARTKEY" NOT NULL ENABLE)
 
  ALTER TABLE "H_PARTSUPP" MODIFY ("PS_SUPPKEY" NOT NULL ENABLE)
 
  ALTER TABLE "H_PARTSUPP" MODIFY ("PS_SUPPLYCOST" NOT NULL ENABLE)
--------------------------------------------------------
--  Constraints for Table H_SUPPLIER
--------------------------------------------------------

  ALTER TABLE "H_SUPPLIER" MODIFY ("S_SUPPKEY" NOT NULL ENABLE)

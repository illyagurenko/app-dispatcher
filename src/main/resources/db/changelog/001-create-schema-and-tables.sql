--liquibase formatted sql

--changeset illya:001-create-schema
CREATE SCHEMA IF NOT EXISTS "ACC_APP_DISPATCHER";

--changeset illya:002-create-permission-table
CREATE TABLE IF NOT EXISTS "ACC_APP_DISPATCHER"."APP_DISPATCHER_PERMISSION"
(
    "ID"               uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    "LOGIN"         VARCHAR(256) UNIQUE NOT NULL,
    "PASS_BCRYPT"       VARCHAR(256) NOT NULL,
    "PERMISSION"      SMALLINT NOT NULL DEFAULT 0,
    "INS_TS"    TIMESTAMPTZ DEFAULT now(),
    "UPD_TS"      TIMESTAMPTZ DEFAULT now()
);

--changeset illya:003-create-trans-table
CREATE TABLE IF NOT EXISTS "ACC_APP_DISPATCHER"."APP_DISPATCHER_TRANS"
(
    "ID"               uuid PRIMARY KEY,
    "PROTOCOL_TYPE"    VARCHAR(256),
    "SYSTEM_ID"        VARCHAR(256),
    "RESP_DATA"        TEXT,
    "REQ_DATA"         TEXT,
    "STATUS"           VARCHAR(256),
    "RESP_CODE"        VARCHAR(16),
    "RESP_DESC"        VARCHAR(256),
    "INS_TS"           TIMESTAMPTZ DEFAULT now(),
    "UPD_TS"           TIMESTAMPTZ DEFAULT now()
);

--changeset illya:004-create-dispatcher-datasource
CREATE TABLE IF NOT EXISTS "ACC_APP_DISPATCHER"."APP_DISPATCHER_DATASOURCE"
(
    "ID"               uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    "ACCOUNT"         VARCHAR(256),
    "JDBC_URL"    VARCHAR(256),
    "JDBC_TYPE"         VARCHAR(256),
    "STATUS"              VARCHAR(256),
    "INS_TS"           TIMESTAMPTZ DEFAULT now(),
    "UPD_TS"          TIMESTAMPTZ DEFAULT now()
);
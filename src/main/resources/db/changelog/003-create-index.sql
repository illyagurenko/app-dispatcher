--liquibase formatted sql

--changeset illya:007-create-datasource-indexes
CREATE INDEX IF NOT EXISTS "idx_dispatcher_datasource_account_status"
    ON "ACC_APP_DISPATCHER"."APP_DISPATCHER_DATASOURCE" ("ACCOUNT", "STATUS");

CREATE INDEX IF NOT EXISTS "idx_dispatcher_permission_login"
    ON "ACC_APP_DISPATCHER"."APP_DISPATCHER_PERMISSION" ("LOGIN");

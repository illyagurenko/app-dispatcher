-- скрипт генерации 1 000 000 банковских счетов в базе app-dispatcher
-- реальный формат счетов из Oracle: 40817810000000000001 .. 40817810000001000000

-- 1. Создание составного индекса (если еще не создан)
CREATE INDEX IF NOT EXISTS "idx_dispatcher_datasource_account_status" 
    ON "ACC_APP_DISPATCHER"."APP_DISPATCHER_DATASOURCE" ("ACCOUNT", "STATUS");

-- 2. Вставка 1 000 000 реальных банковских счетов для Oracle GRU
INSERT INTO "ACC_APP_DISPATCHER"."APP_DISPATCHER_DATASOURCE" ("ID", "ACCOUNT", "JDBC_URL", "JDBC_TYPE", "STATUS", "INS_TS", "UPD_TS")
SELECT 
    gen_random_uuid(),
    '40817810' || LPAD(i::text, 12, '0'),
    'jdbc:postgresql://localhost:5432/changer_db',
    'POSTGRES',
    'ACTIVE',
    now(),
    now()
FROM generate_series(1, 1000000) AS i
ON CONFLICT DO NOTHING;

-- 3. Обновление статистики планировщика PostgreSQL
ANALYZE "ACC_APP_DISPATCHER"."APP_DISPATCHER_DATASOURCE";

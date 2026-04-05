-- Convert column type
ALTER TABLE pricing_catalog
ALTER COLUMN pricing_id TYPE BIGINT;

-- Fix sequence to BIGINT (important)
ALTER SEQUENCE pricing_catalog_pricing_id_seq AS BIGINT;
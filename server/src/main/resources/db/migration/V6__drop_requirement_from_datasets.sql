-- Remove the requirement foreign key and column from datasets.
-- The business_requirements table is kept for data preservation.

ALTER TABLE datasets DROP CONSTRAINT IF EXISTS fk_datasets_requirement;
ALTER TABLE datasets DROP COLUMN IF EXISTS requirement_id;
DROP INDEX IF EXISTS idx_datasets_requirement_id;

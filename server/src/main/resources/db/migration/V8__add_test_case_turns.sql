ALTER TABLE test_cases ADD COLUMN turns jsonb;
ALTER TABLE test_cases ALTER COLUMN question DROP NOT NULL;

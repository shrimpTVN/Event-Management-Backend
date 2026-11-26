-- Apply before deploying the FACULTY enum. Idempotent for existing text scope columns.
BEGIN;
UPDATE criterias SET scope = 'FACULTY' WHERE scope = 'SCHOOL';
COMMIT;

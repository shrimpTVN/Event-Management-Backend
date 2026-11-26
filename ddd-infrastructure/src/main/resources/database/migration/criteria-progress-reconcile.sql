-- Dry-run by default. Run in a maintenance transaction after deploying shared projection rules.
-- To repair, set LOCAL app.criteria_repair = 'on' after BEGIN and replace final ROLLBACK with COMMIT.
BEGIN;
CREATE TEMP TABLE criteria_expected ON COMMIT DROP AS
WITH RECURSIVE visible AS (
 SELECT id, parent_id, required_children, required_occurrences FROM criterias WHERE is_active AND parent_id IS NULL
 UNION ALL
 SELECT c.id,c.parent_id,c.required_children,c.required_occurrences FROM criterias c JOIN visible p ON c.parent_id=p.id WHERE c.is_active
), pairs AS (
 SELECT user_id,academic_year_id FROM student_criteria_transactions
 UNION SELECT user_id,academic_year_id FROM student_criteria_progress
)
SELECT p.user_id,p.academic_year_id,c.id AS criteria_id,c.parent_id,c.required_children,
 GREATEST(COALESCE(c.required_occurrences,1),1) AS required_occurrences,
 0::integer AS occurrences,0::integer AS completed_children,false AS completed,false AS ready
FROM pairs p CROSS JOIN visible c;

-- Each manual record counts once. System events retain retry deduplication.
UPDATE criteria_expected e SET
 occurrences = (SELECT count(DISTINCT CASE
 WHEN t.completion_source='MANUAL' THEN 'manual:'||t.id
 WHEN t.event_id IS NULL THEN 'system:'||t.id ELSE 'event:'||t.event_id END)
 FROM student_criteria_transactions t WHERE t.user_id=e.user_id AND t.academic_year_id=e.academic_year_id
 AND t.criteria_id=e.criteria_id AND t.is_active AND t.completion_source IN ('MANUAL','SYSTEM','EVENT_CHECKOUT'));
UPDATE criteria_expected e SET ready=true, completed=(occurrences >= required_occurrences)
WHERE NOT EXISTS(SELECT 1 FROM criteria_expected c WHERE c.user_id=e.user_id AND c.academic_year_id=e.academic_year_id AND c.parent_id=e.criteria_id);
DO $$
DECLARE changed integer;
BEGIN
 LOOP
  UPDATE criteria_expected e SET ready=true,
   completed_children=(SELECT count(*) FROM criteria_expected c WHERE c.user_id=e.user_id AND c.academic_year_id=e.academic_year_id AND c.parent_id=e.criteria_id AND c.completed),
   completed=(SELECT count(*) FILTER(WHERE c.completed) >= COALESCE(e.required_children,1) FROM criteria_expected c
       WHERE c.user_id=e.user_id AND c.academic_year_id=e.academic_year_id AND c.parent_id=e.criteria_id)
  WHERE NOT e.ready AND NOT EXISTS(SELECT 1 FROM criteria_expected c WHERE c.user_id=e.user_id AND c.academic_year_id=e.academic_year_id AND c.parent_id=e.criteria_id AND NOT c.ready);
  GET DIAGNOSTICS changed = ROW_COUNT;
  EXIT WHEN changed=0;
 END LOOP;
END $$;
SELECT e.*,p.is_completed AS stored_completed,p.completed_occurrences AS stored_occurrences,
 p.completed_children_count AS stored_children
FROM criteria_expected e LEFT JOIN student_criteria_progress p USING(user_id,academic_year_id,criteria_id)
WHERE p.id IS NULL OR p.is_completed IS DISTINCT FROM e.completed OR p.completed_occurrences IS DISTINCT FROM e.occurrences
 OR p.completed_children_count IS DISTINCT FROM e.completed_children;
SELECT p.* FROM student_criteria_progress p
WHERE NOT EXISTS(SELECT 1 FROM criteria_expected e WHERE e.user_id=p.user_id
 AND e.academic_year_id=p.academic_year_id AND e.criteria_id=p.criteria_id)
AND (p.is_completed OR p.completed_occurrences<>0 OR p.completed_children_count<>0);
DO $$
BEGIN
 IF current_setting('app.criteria_repair',true) = 'on' THEN
  UPDATE student_criteria_progress p SET is_completed=false,completed_occurrences=0,
   completed_children_count=0,completed_at=null,updated_at=now()
  WHERE NOT EXISTS(SELECT 1 FROM criteria_expected e WHERE e.user_id=p.user_id
   AND e.academic_year_id=p.academic_year_id AND e.criteria_id=p.criteria_id);
  INSERT INTO student_criteria_progress(user_id,academic_year_id,criteria_id,completed_occurrences,completed_children_count,is_completed,completed_at,created_at,updated_at,is_active)
  SELECT user_id,academic_year_id,criteria_id,occurrences,completed_children,completed,
     CASE WHEN completed THEN now() END,now(),now(),true FROM criteria_expected
  ON CONFLICT(user_id,academic_year_id,criteria_id) DO UPDATE SET
   completed_occurrences=excluded.completed_occurrences,completed_children_count=excluded.completed_children_count,
   is_completed=excluded.is_completed,
   completed_at=CASE WHEN excluded.is_completed THEN COALESCE(student_criteria_progress.completed_at,excluded.completed_at) END,
   updated_at=now();
 END IF;
END $$;
ROLLBACK;

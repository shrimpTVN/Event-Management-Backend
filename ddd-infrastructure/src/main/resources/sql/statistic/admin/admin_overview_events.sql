SELECT 
    COUNT(id)                                        AS total_events,
    COUNT(CASE WHEN status = 'PUBLISHED' THEN 1 END) AS published_events,
    COUNT(CASE WHEN status = 'COMPLETED' THEN 1 END) AS completed_events,
    COUNT(CASE WHEN status = 'CANCELLED' THEN 1 END) AS cancelled_events
FROM events
WHERE is_active = true
  AND (:year::int IS NULL OR EXTRACT(YEAR FROM date_happen) = :year::int)
  AND (:semesterId::bigint IS NULL OR semester_id = :semesterId::bigint);

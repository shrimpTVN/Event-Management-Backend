SELECT 
    COUNT(e.id)                                        AS total_events,
    COUNT(CASE WHEN e.status = 'PUBLISHED' THEN 1 END) AS published_events,
    COUNT(CASE WHEN e.status = 'COMPLETED' THEN 1 END) AS completed_events,
    COUNT(CASE WHEN e.status = 'CANCELLED' THEN 1 END) AS cancelled_events
FROM events e
LEFT JOIN semesters sem ON e.semester_id = sem.id
LEFT JOIN academic_years ay ON sem.academic_year_id = ay.id
WHERE e.is_active = true
  AND (:year::int IS NULL OR EXTRACT(YEAR FROM e.date_happen) = :year::int)
  AND (:semesterId::bigint IS NULL OR e.semester_id = :semesterId::bigint)
  AND (:academicYear::varchar IS NULL OR ay.code = :academicYear::varchar);

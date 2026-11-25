SELECT 
    COUNT(CASE WHEN r.status != 'CANCELLED' THEN 1 END)                                     AS total_registrations,
    COUNT(CASE WHEN r.status IN ('CHECKED_IN', 'CHECKED_OUT', 'COMPLETED') THEN 1 END)      AS total_attendees,
    COUNT(CASE WHEN r.status = 'ABSENT' THEN 1 END)                                         AS absent_count,
    ROUND(
        COALESCE(
            COUNT(CASE WHEN r.status IN ('CHECKED_IN', 'CHECKED_OUT', 'COMPLETED') THEN 1 END)::numeric / 
            NULLIF(COUNT(CASE WHEN r.status != 'CANCELLED' THEN 1 END), 0) * 100, 
            0
        ), 2
    )::double precision                                                                     AS attendance_rate,
    ROUND(
        COALESCE(
            COUNT(CASE WHEN r.status = 'ABSENT' THEN 1 END)::numeric / 
            NULLIF(COUNT(CASE WHEN r.status != 'CANCELLED' THEN 1 END), 0) * 100, 
            0
        ), 2
    )::double precision                                                                     AS absent_rate
FROM registrations r
JOIN events e ON r.event_id = e.id
LEFT JOIN semesters sem ON e.semester_id = sem.id
LEFT JOIN academic_years ay ON sem.academic_year_id = ay.id
WHERE e.is_active = true
  AND (:year::int IS NULL OR EXTRACT(YEAR FROM e.date_happen) = :year::int)
  AND (:semesterId::bigint IS NULL OR e.semester_id = :semesterId::bigint)
  AND (:academicYear::varchar IS NULL OR ay.code = :academicYear::varchar);

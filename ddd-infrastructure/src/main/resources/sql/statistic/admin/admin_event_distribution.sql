SELECT 
    et.id          AS event_type_id,
    et.name        AS event_type_name,
    COUNT(e.id)    AS event_count
FROM event_types et
LEFT JOIN events e ON e.event_type_id = et.id 
    AND e.is_active = true
    AND (:year::int IS NULL OR EXTRACT(YEAR FROM e.date_happen) = :year::int)
    AND (:semesterId::bigint IS NULL OR e.semester_id = :semesterId::bigint)
LEFT JOIN semesters sem ON e.semester_id = sem.id
LEFT JOIN academic_years ay ON sem.academic_year_id = ay.id
WHERE (:academicYear::varchar IS NULL OR e.id IS NULL OR ay.code = :academicYear::varchar)
GROUP BY et.id, et.name
ORDER BY event_count DESC;

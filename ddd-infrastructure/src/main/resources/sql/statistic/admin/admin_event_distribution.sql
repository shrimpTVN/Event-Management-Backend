SELECT 
    et.id          AS event_type_id,
    et.name        AS event_type_name,
    COUNT(e.id)    AS event_count
FROM event_types et
LEFT JOIN events e ON e.event_type_id = et.id 
    AND e.is_active = true
    AND (:year::int IS NULL OR EXTRACT(YEAR FROM e.date_happen) = :year::int)
    AND (:semesterId::bigint IS NULL OR e.semester_id = :semesterId::bigint)
GROUP BY et.id, et.name
ORDER BY event_count DESC;

SELECT 
    EXTRACT(MONTH FROM e.date_happen)::int              AS month,
    TO_CHAR(e.date_happen, 'Mon')                       AS month_name,
    COUNT(CASE WHEN r.status != 'CANCELLED' THEN 1 END) AS total_registrations,
    COUNT(CASE WHEN r.status = 'ABSENT' THEN 1 END)     AS absent_count,
    ROUND(
        COALESCE(
            COUNT(CASE WHEN r.status = 'ABSENT' THEN 1 END)::numeric / 
            NULLIF(COUNT(CASE WHEN r.status != 'CANCELLED' THEN 1 END), 0) * 100, 
            0
        ), 2
    )::double precision                                 AS absent_rate
FROM registrations r
JOIN events e ON r.event_id = e.id
WHERE e.fanpage_id = :fanpageId
  AND e.is_active = true
  AND (:year::int IS NULL OR EXTRACT(YEAR FROM e.date_happen) = :year::int)
  AND (:semesterId::bigint IS NULL OR e.semester_id = :semesterId::bigint)
GROUP BY EXTRACT(MONTH FROM e.date_happen), TO_CHAR(e.date_happen, 'Mon')
ORDER BY month ASC;

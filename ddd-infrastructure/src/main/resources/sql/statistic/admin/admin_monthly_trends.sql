SELECT 
    TO_CHAR(e.date_happen, 'YYYY-MM')                                                                       AS month_year,
    COUNT(DISTINCT e.id)                                                                                    AS event_count,
    COUNT(r.user_id)                                                                                        AS registration_count,
    COUNT(r.user_id) FILTER (WHERE r.status IN ('CHECKED_IN', 'CHECKED_OUT', 'COMPLETED'))                   AS attendee_count,
    COUNT(r.user_id) FILTER (WHERE r.status = 'ABSENT')                                                     AS absent_count,
    ROUND(
        COALESCE(
            COUNT(r.user_id) FILTER (WHERE r.status = 'ABSENT')::numeric /
            NULLIF(COUNT(r.user_id) FILTER (WHERE r.status != 'CANCELLED'), 0) * 100,
            0
        ), 2
    )::double precision                                                                                     AS absent_rate
FROM events e
LEFT JOIN registrations r ON r.event_id = e.id
WHERE e.is_active = true
  AND (:year::int IS NULL OR EXTRACT(YEAR FROM e.date_happen) = :year::int)
  AND (:semesterId::bigint IS NULL OR e.semester_id = :semesterId::bigint)
GROUP BY TO_CHAR(e.date_happen, 'YYYY-MM')
ORDER BY month_year ASC;

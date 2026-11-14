SELECT
    COALESCE(
        CASE
            WHEN sp.k_number IS NOT NULL THEN 'K' || sp.k_number::text
            ELSE 'Unknown'
        END,
        'Unknown'
    )                                                               AS name,
    COUNT(r.user_id)                                                AS count
FROM events e
JOIN registrations r ON r.event_id = e.id
JOIN users u ON u.id = r.user_id
LEFT JOIN student_profiles sp ON sp.user_id = u.id
WHERE e.fanpage_id = :fanpageId
  AND e.is_active = true
  AND r.status != 'CANCELLED'
  AND (:year::int IS NULL OR EXTRACT(YEAR FROM e.date_happen) = :year::int)
  AND (:semesterId::bigint IS NULL OR e.semester_id = :semesterId::bigint)
GROUP BY sp.k_number
ORDER BY count DESC;

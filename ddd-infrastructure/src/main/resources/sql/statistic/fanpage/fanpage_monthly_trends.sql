SELECT TO_CHAR(e.date_happen, 'YYYY-MM')                                         AS month_year,
       COUNT(DISTINCT e.id)                                                      AS event_count,
       COUNT(r.user_id)                                                          AS registration_count,
       COUNT(r.user_id) FILTER (WHERE r.status IN ('CHECKED_IN', 'CHECKED_OUT')) AS attendee_count
FROM events e
         LEFT JOIN registrations r ON r.event_id = e.id
WHERE e.fanpage_id = :fanpageId
  AND e.is_active = true
  AND (:year::int IS NULL OR EXTRACT(YEAR FROM e.date_happen) = :year::int)
GROUP BY TO_CHAR(e.date_happen, 'YYYY-MM')
ORDER BY month_year ASC;

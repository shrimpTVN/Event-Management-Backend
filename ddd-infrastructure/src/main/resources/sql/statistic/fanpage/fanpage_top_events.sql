SELECT e.id                                                                      AS event_id,
       e.name                                                                    AS event_name,
       e.banner_url                                                              AS banner_url,
       e.date_happen                                                             AS date_happen,
       e.capacity                                                                AS capacity,
       COUNT(r.user_id)                                                          AS registration_count,
       COUNT(r.user_id) FILTER (WHERE r.status IN ('COMPLETED', 'CONFIRMED'))    AS attendee_count
FROM events e
         LEFT JOIN registrations r ON r.event_id = e.id
WHERE e.fanpage_id = :fanpageId
  AND e.is_active = true
GROUP BY e.id, e.name, e.banner_url, e.date_happen, e.capacity
ORDER BY attendee_count DESC
LIMIT :limit;

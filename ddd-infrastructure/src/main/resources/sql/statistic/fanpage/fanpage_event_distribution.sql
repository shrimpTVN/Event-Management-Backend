SELECT et.name     AS event_type_name,
       COUNT(e.id) AS event_count
FROM events e
         JOIN event_types et ON et.id = e.event_type_id
WHERE e.fanpage_id = :fanpageId
  AND e.is_active = true
GROUP BY et.name
ORDER BY event_count DESC;

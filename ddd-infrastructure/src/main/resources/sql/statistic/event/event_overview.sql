SELECT e.capacity                     AS capacity,
       COALESCE(e.male_quantity, 0)   AS male_quantity,
       COALESCE(e.female_quantity, 0) AS female_quantity,
       COUNT(r.user_id)               AS total_registrations,
       COUNT(r.user_id)
       FILTER (WHERE r.status IN ('CHECKED_IN', 'SELF_CHECKED_IN', 'SPENDING_CONFIRM', 'CONFIRMED', 'COMPLETED'))
                                      AS total_attended
FROM events e
         LEFT JOIN registrations r ON r.event_id = e.id
WHERE e.id = :eventId
GROUP BY e.id, e.capacity, e.male_quantity, e.female_quantity;
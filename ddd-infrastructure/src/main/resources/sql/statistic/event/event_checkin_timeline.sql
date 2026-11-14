SELECT TO_CHAR(date_bin('15 minutes', r.check_in_at, TIMESTAMP '2000-01-01 00:00:00'),
               'YYYY-MM-DD HH24:MI') AS time_slot,
       COUNT(r.user_id)              AS check_in_count
FROM registrations r
WHERE r.event_id = :eventId
  AND r.check_in_at IS NOT NULL
GROUP BY time_slot
ORDER BY time_slot ASC;

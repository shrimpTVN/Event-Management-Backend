SELECT COALESCE(u.gender, 'UNKNOWN') AS label,
       COUNT(r.user_id)              AS student_count
FROM registrations r
         JOIN users u ON u.id = r.user_id
WHERE r.event_id = :eventId
GROUP BY u.gender
ORDER BY student_count DESC;

SELECT COALESCE(m.name, 'Other') AS label,
       COUNT(r.user_id)          AS student_count
FROM registrations r
         JOIN users u ON u.id = r.user_id
         LEFT JOIN student_profiles sp ON sp.user_id = u.id
         LEFT JOIN majors m ON m.id = sp.major_id
WHERE r.event_id = :eventId
GROUP BY m.name
ORDER BY student_count DESC;

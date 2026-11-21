SELECT 
    sp.student_id                                AS student_id,
    u.id                                         AS user_id,
    TRIM(CONCAT(u.first_name, ' ', u.last_name)) AS full_name,
    u.email                                      AS email,
    u.avatar_url                                 AS avatar_url,
    COUNT(r.event_id)                            AS absence_count
FROM registrations r
JOIN events e ON r.event_id = e.id
JOIN student_profiles sp ON r.user_id = sp.user_id
JOIN users u ON sp.user_id = u.id
WHERE e.fanpage_id = :fanpageId
  AND e.is_active = true
  AND e.semester_id = :semesterId
  AND r.status = 'ABSENT'
GROUP BY sp.student_id, u.id, u.first_name, u.last_name, u.email, u.avatar_url
HAVING COUNT(r.event_id) >= :minAbsences
ORDER BY absence_count DESC, sp.student_id ASC;

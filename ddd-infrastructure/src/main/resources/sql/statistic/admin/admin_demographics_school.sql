SELECT COALESCE(s.name, 'UNKNOWN') AS label,
       COUNT(DISTINCT r.user_id)    AS count
FROM events e
JOIN registrations r ON r.event_id = e.id
JOIN users u ON u.id = r.user_id
LEFT JOIN student_profiles sp ON sp.user_id = u.id
LEFT JOIN majors m ON sp.major_id = m.id
LEFT JOIN schools s ON m.school_id = s.id
WHERE e.is_active = true
  AND r.status != 'CANCELLED'
  AND (:year::int IS NULL OR EXTRACT(YEAR FROM e.date_happen) = :year::int)
  AND (:semesterId::bigint IS NULL OR e.semester_id = :semesterId::bigint)
GROUP BY s.name
ORDER BY count DESC;

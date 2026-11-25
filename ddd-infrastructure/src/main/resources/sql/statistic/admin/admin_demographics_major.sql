SELECT COALESCE(m.name, 'UNKNOWN') AS label,
       COUNT(DISTINCT r.user_id)    AS count
FROM events e
LEFT JOIN semesters sem ON e.semester_id = sem.id
LEFT JOIN academic_years ay ON sem.academic_year_id = ay.id
JOIN registrations r ON r.event_id = e.id
JOIN users u ON u.id = r.user_id
LEFT JOIN student_profiles sp ON sp.user_id = u.id
LEFT JOIN majors m ON sp.major_id = m.id
WHERE e.is_active = true
  AND r.status != 'CANCELLED'
  AND (:year::int IS NULL OR EXTRACT(YEAR FROM e.date_happen) = :year::int)
  AND (:semesterId::bigint IS NULL OR e.semester_id = :semesterId::bigint)
  AND (:academicYear::varchar IS NULL OR ay.code = :academicYear::varchar)
GROUP BY m.name
ORDER BY count DESC;

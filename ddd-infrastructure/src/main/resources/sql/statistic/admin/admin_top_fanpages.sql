SELECT fp.id                                                                                  AS fanpage_id,
       fp.name                                                                                AS fanpage_name,
       fp.avatar_url                                                                          AS avatar_url,
       COUNT(DISTINCT e.id)                                                                   AS event_count,
       COUNT(DISTINCT fol.user_id)                                                            AS follower_count,
       COUNT(DISTINCT (r.event_id, r.user_id)) FILTER (WHERE r.status IN ('CHECKED_IN', 'CHECKED_OUT', 'COMPLETED')) AS attendee_count
FROM fanpages fp
         LEFT JOIN events e ON fp.id = e.fanpage_id
    AND e.is_active = true
    AND (:year::int IS NULL OR EXTRACT(YEAR FROM e.date_happen) = :year::int)
    AND (:semesterId::bigint IS NULL OR e.semester_id = :semesterId::bigint)
         LEFT JOIN semesters sem ON e.semester_id = sem.id
         LEFT JOIN academic_years ay ON sem.academic_year_id = ay.id
         LEFT JOIN registrations r ON r.event_id = e.id
         LEFT JOIN followings fol ON fol.fanpage_id = fp.id AND fol.fol_status = 'FOLLOWING' AND fol.is_active = true
WHERE fp.is_active = true
  AND fp.status = 'ACTIVE'
  AND (:academicYear::varchar IS NULL OR e.id IS NULL OR ay.code = :academicYear::varchar)
GROUP BY fp.id, fp.name, fp.avatar_url
ORDER BY event_count DESC, attendee_count DESC
LIMIT COALESCE(:limit::int, 2147483647);

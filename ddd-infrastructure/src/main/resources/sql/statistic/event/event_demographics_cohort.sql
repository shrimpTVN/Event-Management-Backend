SELECT COALESCE(
           CASE
               WHEN sp.k_number IS NOT NULL THEN 'K' || CAST(sp.k_number AS VARCHAR)
               ELSE 'UNKNOWN'
           END,
           'UNKNOWN'
       ) AS label,
       COUNT(distinct r.user_id)                         AS student_count
FROM registrations r
         JOIN users u ON u.id = r.user_id
         LEFT JOIN student_profiles sp ON sp.user_id = u.id
WHERE r.event_id = :eventId
GROUP BY sp.k_number
ORDER BY student_count DESC;

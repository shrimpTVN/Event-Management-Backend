SELECT COUNT(DISTINCT e.id)        AS total_events,
       (SELECT COUNT(*)
        FROM followings f
        WHERE f.fanpage_id = :fanpageId
          AND f.fol_status = 'FOLLOWING'
          AND f.is_active = true)  AS total_followers,
       (SELECT COUNT(*)
        FROM fanpage_members fm
        WHERE fm.fanpage_id = :fanpageId
          AND fm.is_active = true) AS total_members,
       COUNT(r.user_id)            AS total_registrations,
       COUNT(r.user_id)               FILTER (WHERE r.status NOT IN ('REGISTERED', 'CANCELLED', 'ABSENT'))
                                   AS total_attendees
FROM events e
         LEFT JOIN registrations r ON r.event_id = e.id
WHERE e.fanpage_id = :fanpageId
  AND e.is_active = true;

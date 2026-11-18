SELECT COUNT(r.user_id) FILTER (WHERE r.status = 'REGISTERED')       AS registered_count,
       COUNT(r.user_id) FILTER (WHERE r.status = 'SELF_CHECKED_IN')  AS self_checked_in_count,
       COUNT(r.user_id) FILTER (WHERE r.status = 'CHECKED_IN')       AS checked_in_count,
       COUNT(r.user_id) FILTER (WHERE r.status = 'SPENDING_CONFIRM') AS spending_confirm_count,
       COUNT(r.user_id) FILTER (WHERE r.status = 'CONFIRMED')        AS confirmed_count,
       COUNT(r.user_id) FILTER (WHERE r.status = 'COMPLETED')        AS completed_count,
       COUNT(r.user_id) FILTER (WHERE r.status = 'ABSENT')           AS absent_count,
       COUNT(r.user_id) FILTER (WHERE r.status = 'CANCELLED')        AS cancelled_count
FROM registrations r
WHERE r.event_id = :eventId;

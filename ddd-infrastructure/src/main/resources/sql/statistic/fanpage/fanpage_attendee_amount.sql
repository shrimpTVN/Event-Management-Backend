SELECT COUNT(r.id) AS attendee_amountnt
FROM fanpages fp LEFT JOIN registrations r ON fp.id = r.fanpage_id
where fp.id = :fanpageId
    AND fp.is_active = true
    AND r.is_active = true
    AND r.status NOT IN ('CANCELLED', 'ABSENT', 'REGISTERED');
SELECT COUNT(DISTINCT fm.user_id) AS member_count
FROM fanpages fp LEFT JOIN fanpage_members fm ON fp.id = fm.fanpage_id
WHERE fp.id = :fanpageId
  AND fp.is_active = true
  AND fm.is_active = true;

SELECT *
FROM fanpage_members
    JOIN users u ON fanpage_members.user_id = u.id
    JOIN student_profiles sp ON u.id = sp.user_id
    JOIN fanpage_admin_profiles fap ON fanpage_members.user_id = u.id
WHERE fanpage_members.fanpage_id = 13 AND fanpage_members.is_active = true;
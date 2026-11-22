SELECT 
    COUNT(u.id)                                                      AS total_users,
    COUNT(DISTINCT sp.user_id)                                       AS total_students,
    COUNT(DISTINCT fp.user_id)                                       AS total_staff,
    COUNT(CASE WHEN u.is_active = true THEN 1 END)                   AS total_active_users
FROM users u
LEFT JOIN student_profiles sp ON u.id = sp.user_id
LEFT JOIN fanpage_admin_profiles fp ON u.id = fp.user_id;

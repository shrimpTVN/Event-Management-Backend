SELECT COUNT(id)                                      AS total_fanpages,
       COUNT(CASE WHEN status = 'ACTIVE' THEN 1 END)  AS active_fanpages,
       COUNT(CASE WHEN status = 'PENDING' THEN 1 END) AS pending_fanpages,
       COUNT(CASE WHEN status = 'BANNED' THEN 1 END)  AS banned_fanpages
FROM fanpages
WHERE is_active = true;

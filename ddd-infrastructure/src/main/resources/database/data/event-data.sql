-- Insert 10 Events with fanpage_id = 1, criteria_id in [1, 5], event_type_id in [1, 4]
INSERT INTO events (name, description, date_open, date_close, date_happen, location, capacity, male_quantity,
                    female_quantity, banner_url, status, is_active, event_type_id, criteria_id, fanpage_id, created_by,
                    updated_by)
    OVERRIDING SYSTEM VALUE
VALUES ('Tech Seminar 2026', 'A seminar about the future of tech', NOW(), NOW() + INTERVAL '5 days',
        NOW() + INTERVAL '10 days', 'Hall A', 100, 50, 50, 'https://example.com/banner1.png', 'PUBLISHED', true, 1, 1,
        13, 1, 1),
       ('Spring Boot Workshop', 'Hands-on workshop for Spring Boot', NOW(), NOW() + INTERVAL '5 days',
        NOW() + INTERVAL '10 days', 'Room 101', 50, 25, 25, 'https://example.com/banner2.png', 'PUBLISHED', true, 2, 2,
        13, 1, 1),
       ('Data Science Bootcamp', 'Intensive bootcamp for data science', NOW(), NOW() + INTERVAL '5 days',
        NOW() + INTERVAL '10 days', 'Lab 3', 30, 15, 15, 'https://example.com/banner3.png', 'PUBLISHED', true, 3, 3, 13,
        1, 1),
       ('UI/UX Design Contest', 'Show your design skills', NOW(), NOW() + INTERVAL '5 days', NOW() + INTERVAL '10 days',
        'Online', 200, 100, 100, 'https://example.com/banner4.png', 'PUBLISHED', true, 4, 4, 13, 1, 1),
       ('AI Hackathon', 'Hackathon for AI enthusiasts', NOW(), NOW() + INTERVAL '5 days', NOW() + INTERVAL '10 days',
        'Main Campus', 150, 75, 75, 'https://example.com/banner5.png', 'PUBLISHED', true, 1, 5, 13, 1, 1),
       ('Cybersecurity Talk', 'Learn about the latest threats', NOW(), NOW() + INTERVAL '5 days',
        NOW() + INTERVAL '10 days', 'Hall B', 80, 40, 40, 'https://example.com/banner6.png', 'PUBLISHED', true, 2, 1, 13,
        1, 1),
       ('Cloud Computing 101', 'Introduction to Cloud', NOW(), NOW() + INTERVAL '5 days', NOW() + INTERVAL '10 days',
        'Room 202', 60, 30, 30, 'https://example.com/banner7.png', 'PUBLISHED', true, 3, 2, 13, 1, 1),
       ('DevOps Meeting', 'DevOps community meetup', NOW(), NOW() + INTERVAL '5 days', NOW() + INTERVAL '10 days',
        'Cafe Tech', 40, 20, 20, 'https://example.com/banner8.png', 'PUBLISHED', true, 4, 3, 13, 1, 1),
       ('Web3 Conference', 'Explore the decentralized web', NOW(), NOW() + INTERVAL '5 days',
        NOW() + INTERVAL '10 days', 'Conference Center', 300, 150, 150, 'https://example.com/banner9.png', 'PUBLISHED',
        true, 1, 4, 13, 1, 1),
       ('Open Source Contribution', 'Contribute to OSS', NOW(), NOW() + INTERVAL '5 days', NOW() + INTERVAL '10 days',
        'Online', 500, 250, 250, 'https://example.com/banner10.png', 'PUBLISHED', true, 2, 5, 13, 1, 1);

-- Insert Event Points with point_category_id in [5, 9] for the created events
INSERT INTO event_points (event_id, point_category_id, point, created_by, updated_by)
    OVERRIDING SYSTEM VALUE
VALUES (18, 5, 10, 1, 1),
       (19, 6, 15, 1, 1),
       (20, 7, 20, 1, 1),
       (21, 8, 25, 1, 1),
       (22, 9, 30, 1, 1),
       (23, 5, 10, 1, 1),
       (24, 6, 15, 1, 1),
       (25, 7, 20, 1, 1),
       (26, 8, 25, 1, 1),
       (27, 9, 30, 1, 1);


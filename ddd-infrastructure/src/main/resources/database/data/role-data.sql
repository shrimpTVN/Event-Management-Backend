INSERT INTO roles (name, description, created_by)
VALUES ('ADMIN', 'Administrator role with full access', 1),
       ('FANPAGE_ADMIN',
        'Fanpage administrator role can manage fanpage and its content. One fanpage admin can only manage one fanpage at the same time',
        1),
       ('STUDENT', 'Regular user role with limited access', 1);

package com.ddd.application.service.registration.impl;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;

import java.nio.charset.StandardCharsets;
import java.util.UUID;

final class RegistrationPostgresFixtures implements AutoCloseable {
    private final String schema = "registration_test_" + UUID.randomUUID().toString().replace("-", "");
    private final JdbcTemplate admin;
    final HikariDataSource pool;
    final JdbcTemplate db;

    RegistrationPostgresFixtures() throws Exception {
        String url = setting("REGISTRATION_TEST_JDBC_URL");
        if (!url.startsWith("jdbc:postgresql:")) {
            throw new IllegalArgumentException("REGISTRATION_TEST_JDBC_URL must identify PostgreSQL");
        }
        String user = setting("REGISTRATION_TEST_DB_USER");
        String password = setting("REGISTRATION_TEST_DB_PASSWORD");
        admin = new JdbcTemplate(new DriverManagerDataSource(url, user, password));
        HikariConfig config = new HikariConfig();
        config.setJdbcUrl(url);
        config.setUsername(user);
        config.setPassword(password);
        config.setSchema(schema);
        config.setMaximumPoolSize(14);
        config.setConnectionTimeout(5000);
        config.addDataSourceProperty("ApplicationName", schema);
        admin.execute("create schema " + schema);
        try {
            pool = new HikariDataSource(config);
        } catch (RuntimeException failure) {
            dropSchema();
            throw failure;
        }
        db = new JdbcTemplate(pool);
        try {
            // Send the whole PostgreSQL script so dollar-quoted trigger bodies stay intact.
            db.execute(new ClassPathResource("database/schema.sql").getContentAsString(StandardCharsets.UTF_8));
            db.update("insert into majors(code,name) values ('M','Major')");
            db.update("insert into fanpages(name,org_type,org_name,parent_org) values ('Page','Club','Club','School')");
            db.update("insert into event_types(name) values ('Activity')");
            db.update("insert into semesters(number,start_date,end_date) values (1,current_date,current_date+90)");
            db.update("insert into point_categories(name) values ('Service')");
            for (int i = 1; i <= 12; i++) {
                db.update("insert into users(email) values (?)", email(i));
                db.update("insert into student_profiles(user_id, student_id, major_id, k_number) values (?,?,1,1)", i, "S" + i);
            }
            db.update("insert into fanpage_members(fanpage_id,user_id) values (1,12)");
        } catch (Exception failure) {
            close();
            throw failure;
        }
    }

    static String email(int id) {
        return "student" + id + "@example.test";
    }

    private static String setting(String key) {
        String value = System.getProperty(key, System.getenv(key));
        if (value == null || value.isBlank()) {
            throw new IllegalStateException("PostgreSQL concurrency tests require " + key);
        }
        return value;
    }

    long event(int capacity) {
        return event(capacity, 0, 0, false);
    }

    long event(int capacity, int maleQuantity, int femaleQuantity, boolean genderConstraint) {
        return db.queryForObject("""
                insert into events(name,description,location,date_open,date_close,date_happen,capacity,
                    male_quantity,female_quantity,gender_constraint,banner_url,status,event_type_id,fanpage_id,semester_id)
                values ('Event','Description','Hall',now()-interval '1 day',now()+interval '1 day',
                    now()+interval '2 days',?,?,?,?,'banner','PUBLISHED',1,1,1) returning id
                """, Long.class, capacity, maleQuantity, femaleQuantity, genderConstraint);
    }

    void setUserGender(int user, String gender) {
        db.update("update users set gender=? where id=?", gender, user);
    }

    int occupiedByGender(long event, String gender) {
        return db.queryForObject("""
                select count(*) from registrations r join users u on u.id = r.user_id
                where r.event_id=? and r.status <> 'CANCELLED' and upper(u.gender)=upper(?)
                """, Integer.class, event, gender);
    }

    void seed(long event, int user, String status) {
        db.update("insert into registrations(event_id,user_id,status) values (?,?,?)", event, user, status);
    }

    int occupied(long event) {
        return db.queryForObject("select count(*) from registrations where event_id=? and status <> 'CANCELLED'", Integer.class, event);
    }

    String status(long event, int user) {
        return db.queryForObject("select status from registrations where event_id=? and user_id=?", String.class, event, user);
    }

    private void dropSchema() {
        admin.execute("drop schema " + schema + " cascade");
    }

    @Override
    public void close() {
        try {
            pool.close();
        } finally {
            dropSchema();
        }
    }
}

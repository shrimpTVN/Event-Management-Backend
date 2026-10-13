package com.ddd.domain.model;

import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class UserTest {

    @Test
    void testMultipleRolesAssignmentAndVerification() {
        User user = new User();
        user.setEmail("test@university.edu");

        Role studentRole = new Role();
        studentRole.setId(1L);
        studentRole.setName("STUDENT");

        Role adminRole = new Role();
        adminRole.setId(2L);
        adminRole.setName("ADMIN");

        user.addRole(studentRole);
        user.addRole(adminRole);

        assertTrue(user.hasRole("STUDENT"));
        assertTrue(user.hasRole("student"));
        assertTrue(user.hasRole("ADMIN"));
        assertFalse(user.hasRole("TEACHER"));

        Set<String> roleNames = user.getRoleNames();
        assertEquals(2, roleNames.size());
        assertTrue(roleNames.contains("STUDENT"));
        assertTrue(roleNames.contains("ADMIN"));

        user.removeRole(studentRole);
        assertFalse(user.hasRole("STUDENT"));
        assertTrue(user.hasRole("ADMIN"));
        assertEquals(1, user.getRoleNames().size());
    }
}

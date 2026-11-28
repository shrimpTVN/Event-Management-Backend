package com.ddd.api.controller;

import org.junit.jupiter.api.Test;
import org.springframework.security.access.prepost.PreAuthorize;

import java.lang.reflect.Method;

import static org.junit.jupiter.api.Assertions.*;

class AdminAuthorizationSecurityTest {

    @Test
    void roleControllerRequiresAdminAtClassLevel() {
        PreAuthorize annotation = RoleController.class.getAnnotation(PreAuthorize.class);
        assertNotNull(annotation);
        assertEquals("hasRole('ADMIN')", annotation.value());
    }

    @Test
    void catalogAdminMethodsRequireAdminRole() throws NoSuchMethodException {
        assertMethodHasAdminPreAuthorize(SchoolController.class.getDeclaredMethod("createSchool", com.ddd.api.dto.school.req.CreateSchoolRequestDto.class));
        assertMethodHasAdminPreAuthorize(SchoolController.class.getDeclaredMethod("updateSchool", Long.class, com.ddd.api.dto.school.req.UpdateSchoolRequestDto.class));
        assertMethodHasAdminPreAuthorize(SchoolController.class.getDeclaredMethod("deleteSchool", Long.class));

        assertMethodHasAdminPreAuthorize(MajorController.class.getDeclaredMethod("createMajor", com.ddd.api.dto.major.req.CreateMajorRequestDto.class));
        assertMethodHasAdminPreAuthorize(MajorController.class.getDeclaredMethod("updateMajor", Long.class, com.ddd.api.dto.major.req.UpdateMajorRequestDto.class));
        assertMethodHasAdminPreAuthorize(MajorController.class.getDeclaredMethod("deleteMajor", Long.class));

        assertMethodHasAdminPreAuthorize(EventTypeController.class.getDeclaredMethod("create", com.ddd.api.dto.eventtype.req.CreateEventTypeRequestDto.class));
        assertMethodHasAdminPreAuthorize(EventTypeController.class.getDeclaredMethod("update", Long.class, com.ddd.api.dto.eventtype.req.UpdateEventTypeRequestDto.class));
        assertMethodHasAdminPreAuthorize(EventTypeController.class.getDeclaredMethod("delete", Long.class));

        assertMethodHasAdminPreAuthorize(AssociationController.class.getDeclaredMethod("create", com.ddd.api.dto.association.req.CreateAssociationRequestDto.class));
        assertMethodHasAdminPreAuthorize(AssociationController.class.getDeclaredMethod("update", Long.class, com.ddd.api.dto.association.req.UpdateAssociationRequestDto.class));
        assertMethodHasAdminPreAuthorize(AssociationController.class.getDeclaredMethod("delete", Long.class));
    }

    private static void assertMethodHasAdminPreAuthorize(Method method) {
        PreAuthorize annotation = method.getAnnotation(PreAuthorize.class);
        assertNotNull(annotation, () -> method.getDeclaringClass().getSimpleName() + "." + method.getName() + " must be annotated with @PreAuthorize");
        assertEquals("hasRole('ADMIN')", annotation.value());
    }
}

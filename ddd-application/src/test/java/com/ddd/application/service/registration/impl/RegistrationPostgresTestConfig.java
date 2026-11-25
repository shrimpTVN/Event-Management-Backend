package com.ddd.application.service.registration.impl;

import com.ddd.application.service.criteriaprogress.StudentCriteriaTransactionCommandService;
import com.ddd.application.service.event.cache.RecommendationCacheService;
import com.ddd.application.service.event.impl.EventCommandServiceImpl;
import com.ddd.application.service.semesterpoint.StudentSemesterPointCommandService;
import com.ddd.domain.model.port.security.SecurityContextPort;
import com.ddd.infrastructure.repository.*;
import jakarta.persistence.EntityManagerFactory;
import org.springframework.context.annotation.*;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.orm.jpa.JpaTransactionManager;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
import org.springframework.orm.jpa.vendor.HibernateJpaVendorAdapter;
import org.springframework.transaction.annotation.EnableTransactionManagement;

import javax.sql.DataSource;

import static org.mockito.Mockito.mock;

@Configuration
@EnableJpaAuditing
@EnableTransactionManagement
@EnableJpaRepositories(basePackages = "com.ddd.infrastructure.repository.jpaRepository")
@ComponentScan({"com.ddd.infrastructure.mapper", "com.ddd.application.mapper"})
@Import({RegistrationCommandServiceImpl.class, EventCommandServiceImpl.class, RecommendationCacheService.class,
        EventRepositoryImpl.class, RegistrationRepositoryImpl.class, UserRepositoryImpl.class,
        StudentProfileRepositoryImpl.class, FanpageMemberRepositoryImpl.class, EventPointRepositoryImpl.class,
        PointTransactionRepositoryImpl.class, FanpageRepositoryImpl.class, EventTypeRepositoryImpl.class,
        CriteriaRepositoryImpl.class, SemesterRepositoryImpl.class, SchoolRepositoryImpl.class,
        PointCategoryRepositoryImpl.class, FollowingRepositoryImpl.class})
class RegistrationPostgresTestConfig {
    @Bean
    LocalContainerEntityManagerFactoryBean entityManagerFactory(DataSource dataSource) {
        var factory = new LocalContainerEntityManagerFactoryBean();
        factory.setDataSource(dataSource);
        factory.setPackagesToScan("com.ddd.infrastructure.entity");
        factory.setJpaVendorAdapter(new HibernateJpaVendorAdapter());
        factory.getJpaPropertyMap().put("hibernate.physical_naming_strategy",
                "org.hibernate.boot.model.naming.PhysicalNamingStrategySnakeCaseImpl");
        return factory;
    }

    @Bean
    JpaTransactionManager transactionManager(EntityManagerFactory factory) {
        return new JpaTransactionManager(factory);
    }

    @Bean
    StudentSemesterPointCommandService semesterPoints() {
        return mock(StudentSemesterPointCommandService.class);
    }

    @Bean
    StudentCriteriaTransactionCommandService criteriaTransactions() {
        return mock(StudentCriteriaTransactionCommandService.class);
    }

    @Bean
    TestIdentity securityContext() {
        return new TestIdentity();
    }

    static final class TestIdentity implements SecurityContextPort {
        private final ThreadLocal<Long> userId = new ThreadLocal<>();

        void as(int user, Runnable action) {
            userId.set((long) user);
            try {
                action.run();
            } finally {
                userId.remove();
            }
        }

        public Long getCurrentUserId() { return userId.get(); }
        public String getCurrentUserEmail() { return RegistrationPostgresFixtures.email(userId.get().intValue()); }
        public boolean hasRole(String role) { return false; }
        public boolean isAuthenticated() { return userId.get() != null; }
    }
}

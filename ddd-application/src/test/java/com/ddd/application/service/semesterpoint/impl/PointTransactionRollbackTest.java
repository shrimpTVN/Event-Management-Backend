package com.ddd.application.service.semesterpoint.impl;

import com.ddd.application.dto.semesterpoint.PointTransactionRequestInfoDto;
import com.ddd.application.mapper.PointTransactionDtoMapper;
import com.ddd.application.service.semesterpoint.*;
import com.ddd.domain.model.PointTransaction;
import com.ddd.domain.model.port.security.SecurityContextPort;
import com.ddd.domain.repository.PointTransactionRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.*;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.EnableTransactionManagement;
import javax.sql.DataSource;
import java.sql.Connection;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@SpringJUnitConfig(PointTransactionRollbackTest.Config.class)
class PointTransactionRollbackTest {
    @Autowired PointTransactionCommandService service;
    @Autowired PointTransactionRepository transactions;
    @Autowired StudentSemesterPointCommandService rebuild;
    @Autowired PointTransactionDtoMapper mapper;
    @Autowired SecurityContextPort security;
    @Autowired DataSource dataSource;
    @Test void rebuildFailureRollsBackSpringTransactionInsteadOfCommitting() throws Exception {
        var connection=mock(Connection.class);
        when(dataSource.getConnection()).thenReturn(connection);
        when(connection.getAutoCommit()).thenReturn(true);
        when(security.getCurrentUserId()).thenReturn(7L);
        var request=new PointTransactionRequestInfoDto(9L,2L,null,8,"note");
        when(mapper.toDomain(request)).thenReturn(new PointTransaction());
        doThrow(new IllegalStateException("rebuild failed")).when(rebuild).rebuildSemesterPoints(7L,9L);
        assertThrows(IllegalStateException.class,()->service.createPointTransactionForStudent(request));
        verify(transactions).save(any());
        verify(connection).rollback();verify(connection,never()).commit();
    }
    @Configuration
    @EnableTransactionManagement
    static class Config {
        @Bean DataSource dataSource(){return mock(DataSource.class);}
        @Bean PlatformTransactionManager transactionManager(DataSource source){return new DataSourceTransactionManager(source);}
        @Bean PointTransactionRepository transactions(){return mock(PointTransactionRepository.class);}
        @Bean StudentSemesterPointCommandService rebuild(){return mock(StudentSemesterPointCommandService.class);}
        @Bean PointTransactionDtoMapper mapper(){return mock(PointTransactionDtoMapper.class);}
        @Bean SecurityContextPort security(){return mock(SecurityContextPort.class);}
        @Bean SemesterPointCategoryPolicy policy(){return mock(SemesterPointCategoryPolicy.class);}
        @Bean PointTransactionCommandService service(PointTransactionDtoMapper mapper,PointTransactionRepository transactions,
                SemesterPointCategoryPolicy policy,StudentSemesterPointCommandService rebuild,SecurityContextPort security){
            return new PointTransactionCommandServiceImpl(mapper,transactions,policy,rebuild,security);
        }
    }
}

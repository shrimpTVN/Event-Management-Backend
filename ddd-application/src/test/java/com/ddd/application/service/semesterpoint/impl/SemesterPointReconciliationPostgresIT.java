package com.ddd.application.service.semesterpoint.impl;

import com.ddd.application.service.semesterpoint.StudentSemesterPointCommandService;
import com.ddd.domain.model.StudentSemesterPointScope;
import com.ddd.domain.repository.*;
import com.ddd.infrastructure.repository.jpaRepository.StudentSemesterPointJpaRepository;
import org.junit.jupiter.api.*;
import org.springframework.aop.framework.ProxyFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.transaction.annotation.AnnotationTransactionAttributeSource;
import org.springframework.transaction.interceptor.TransactionInterceptor;
import org.springframework.data.jpa.repository.Query;
import java.sql.*;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static com.ddd.application.service.semesterpoint.impl.StudentSemesterPointCalculatorTest.*;

/** Explicit PostgreSQL integration test; creates and removes its own schema. */
class SemesterPointReconciliationPostgresIT {
    String schema;
    DriverManagerDataSource source;
    JdbcTemplate jdbc;
    Connection admin;
    @BeforeEach void setup() throws Exception {
        String url=System.getProperty("POINTS_TEST_JDBC_URL");
        assertNotNull(url,"Set POINTS_TEST_JDBC_URL for an isolated PostgreSQL test database");
        admin=DriverManager.getConnection(url,"postgres","");
        schema="points_test_"+UUID.randomUUID().toString().replace("-","");
        admin.createStatement().execute("CREATE SCHEMA "+schema);
        source=new DriverManagerDataSource(url+(url.contains("?")?"&":"?")+"currentSchema="+schema,"postgres","");
        jdbc=new JdbcTemplate(source);
        jdbc.execute("CREATE TABLE student_semester_points(user_id bigint, semester_id bigint, total_point int)");
        jdbc.execute("CREATE TABLE point_transactions(user_id bigint, semester_id bigint)");
    }
    @AfterEach void cleanup() throws Exception {
        if(admin!=null) {try(var statement=admin.createStatement()){statement.execute("DROP SCHEMA "+schema+" CASCADE");}finally{admin.close();}}
    }
    @Test void nativeScopeUnionIncludesLedgerOnlyPairsAndDeduplicates() throws Exception {
        jdbc.update("INSERT INTO student_semester_points VALUES (7,9,10),(8,10,20)");
        jdbc.update("INSERT INTO point_transactions VALUES (7,9),(7,9),(9,11)");
        String query=StudentSemesterPointJpaRepository.class.getMethod("findAllScopes").getAnnotation(Query.class).value();
        var scopes=jdbc.query(query,(row,index)->new StudentSemesterPointScope(row.getLong(1),row.getLong(2)));
        assertEquals(Set.of(new StudentSemesterPointScope(7L,9L),new StudentSemesterPointScope(8L,10L),new StudentSemesterPointScope(9L,11L)),new HashSet<>(scopes));
        assertEquals(3,scopes.size());
    }
    @Test void failedSecondScopeRollsBackFirstSnapshotWrite() {
        var points=mock(StudentSemesterPointRepository.class);
        var ledger=mock(PointTransactionRepository.class);
        var policy=mock(SemesterPointCategoryPolicy.class);
        var command=mock(StudentSemesterPointCommandService.class);
        when(points.findAllScopes()).thenReturn(List.of(new StudentSemesterPointScope(7L,9L),new StudentSemesterPointScope(7L,10L)));
        when(policy.effectiveCategories(anyLong())).thenReturn(List.of(category(1,null,10,false)));
        AtomicInteger writes=new AtomicInteger();
        doAnswer(invocation->{
            jdbc.update("INSERT INTO student_semester_points VALUES (7,?,5)",invocation.<Long>getArgument(1));
            if(writes.incrementAndGet()==2) throw new IllegalStateException("second rebuild failed");
            return null;
        }).when(command).rebuildSemesterPoints(anyList(),anyLong());
        var target=new SemesterPointReconciliationService(points,ledger,policy,new StudentSemesterPointCalculator(),command);
        var factory=new ProxyFactory(target);factory.setProxyTargetClass(true);
        factory.addAdvice(new TransactionInterceptor(new DataSourceTransactionManager(source),new AnnotationTransactionAttributeSource()));
        var service=(SemesterPointReconciliationService)factory.getProxy();
        assertThrows(IllegalStateException.class,()->service.repair(false));assertEquals(2,writes.get());
        assertEquals(0,jdbc.queryForObject("SELECT COUNT(*) FROM student_semester_points",Integer.class));
        assertEquals(0,jdbc.queryForObject("SELECT COUNT(*) FROM point_transactions",Integer.class));
    }
    @Test void repairsHundredStudentFixtureTwiceWithoutChangingLedger() {
        jdbc.execute("ALTER TABLE student_semester_points ADD COLUMN point_category_id bigint, ADD COLUMN system_point_record int DEFAULT 0, ADD COLUMN manual_point_record int DEFAULT 0, ADD UNIQUE(user_id,semester_id,point_category_id)");
        jdbc.execute("ALTER TABLE point_transactions ADD COLUMN id bigint GENERATED ALWAYS AS IDENTITY, ADD COLUMN point_category_id bigint, ADD COLUMN points int, ADD COLUMN type varchar(40)");
        jdbc.execute("INSERT INTO point_transactions(user_id,semester_id,point_category_id,points,type) SELECT u,9,2,p,'MANUAL' FROM generate_series(7,106) u CROSS JOIN (VALUES(8),(7)) v(p)");
        jdbc.execute("INSERT INTO student_semester_points(user_id,semester_id,point_category_id,total_point,manual_point_record) SELECT u,9,1,15,15 FROM generate_series(7,106) u");
        var points=mock(StudentSemesterPointRepository.class);
        var ledger=mock(PointTransactionRepository.class);
        var policy=mock(SemesterPointCategoryPolicy.class);
        when(policy.effectiveCategories(9L)).thenReturn(List.of(category(1,null,100,false),category(2,1L,10,false)));
        when(points.findAllScopes()).thenAnswer(i->jdbc.query("SELECT user_id,semester_id FROM student_semester_points UNION SELECT user_id,semester_id FROM point_transactions",(row,index)->new StudentSemesterPointScope(row.getLong(1),row.getLong(2))));
        org.springframework.jdbc.core.RowMapper<com.ddd.domain.model.StudentSemesterPoint> mapper=(row,index)->{
            var p=new com.ddd.domain.model.StudentSemesterPoint(row.getLong("user_id"),row.getLong("semester_id"),row.getLong("point_category_id"),row.getInt("total_point"));
            p.setSystemPointRecord(row.getInt("system_point_record"));p.setManualPointRecord(row.getInt("manual_point_record"));return p;
        };
        when(points.findAllByUserIdAndSemesterId(anyLong(),eq(9L))).thenAnswer(i->jdbc.query("SELECT * FROM student_semester_points WHERE user_id=? AND semester_id=9",mapper,(Object)i.getArgument(0)));
        when(points.findAllByUserIdsAndSemesterId(anyList(),eq(9L))).thenAnswer(i->jdbc.query("SELECT * FROM student_semester_points WHERE semester_id=9",mapper));
        when(ledger.findAllByUserIdsAndSemesterId(anyList(),eq(9L))).thenAnswer(i->{
            List<Long> userIds=i.getArgument(0);
            return jdbc.query("SELECT * FROM point_transactions WHERE semester_id=9",(row,index)->{
                var t=new com.ddd.domain.model.PointTransaction();t.setId(row.getLong("id"));t.setUserId(row.getLong("user_id"));t.setSemesterId(9L);t.setPointCategoryId(2L);t.setPoints(row.getInt("points"));t.setType(row.getString("type"));return t;
            }).stream().filter(t->userIds.contains(t.getUserId())).toList();
        });
        doAnswer(i->{for(var p:i.<List<com.ddd.domain.model.StudentSemesterPoint>>getArgument(0)) {
            jdbc.update("INSERT INTO student_semester_points(user_id,semester_id,point_category_id,total_point,system_point_record,manual_point_record) VALUES (?,?,?,?,?,?) ON CONFLICT(user_id,semester_id,point_category_id) DO UPDATE SET total_point=EXCLUDED.total_point,system_point_record=EXCLUDED.system_point_record,manual_point_record=EXCLUDED.manual_point_record",
                p.getUserId(),p.getSemesterId(),p.getPointCategoryId(),p.getTotalPoint(),p.getSystemPointRecord(),p.getManualPointRecord());
        }return null;}).when(points).saveAll(anyList());
        var calculator=new StudentSemesterPointCalculator();
        var command=new StudentSemesterPointCommandServiceImpl(points,ledger,policy,calculator);
        var service=new SemesterPointReconciliationService(points,ledger,policy,calculator,command);
        var preview=service.preview();assertEquals(100,preview.scopes().size());
        assertTrue(preview.scopes().stream().allMatch(scope->scope.error()==null),preview.toString());
        assertEquals(200,preview.scopes().stream().mapToInt(scope->scope.changes().size()).sum());
        assertEquals(15,jdbc.queryForObject("SELECT total_point FROM student_semester_points WHERE user_id=7 AND point_category_id=1",Integer.class));
        var first=service.repair(false);assertTrue(first.applied());
        assertEquals(10,jdbc.queryForObject("SELECT total_point FROM student_semester_points WHERE user_id=7 AND point_category_id=1",Integer.class));
        assertEquals(15,jdbc.queryForObject("SELECT total_point FROM student_semester_points WHERE user_id=7 AND point_category_id=2",Integer.class));
        assertTrue(service.repair(false).scopes().stream().allMatch(scope->scope.changes().isEmpty()));
        assertEquals(200,jdbc.queryForObject("SELECT COUNT(*) FROM point_transactions",Integer.class));
        assertEquals(200,jdbc.queryForObject("SELECT COUNT(*) FROM student_semester_points",Integer.class));
        System.out.println("POINTS_FIXTURE_100_USERS preview_ms="+preview.durationMillis()+" repair_ms="+first.durationMillis());
    }
}

package com.ddd.application.service.semesterpoint.impl;

import com.ddd.application.service.semesterpoint.StudentSemesterPointCommandService;
import com.ddd.domain.exception.BusinessException;
import com.ddd.domain.model.*;
import com.ddd.domain.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class SemesterPointReconciliationService {
    private final StudentSemesterPointRepository snapshots;
    private final PointTransactionRepository transactions;
    private final SemesterPointCategoryPolicy policy;
    private final StudentSemesterPointCalculator calculator;
    private final StudentSemesterPointCommandService command;

    public record PointChange(Long categoryId,Integer beforeTotal,int afterTotal,Integer beforeSystem,int afterSystem,Integer beforeManual,int afterManual) {}
    public record Issue(Long transactionId,Long categoryId,String reason) {}
    public record ManualRecord(Long id,Long categoryId,Integer points,String description) {}
    public record ScopeReport(Long userId,Long semesterId,List<PointChange> changes,List<Issue> issues,List<ManualRecord> manualRecords,boolean missingLedger,String error) {}
    public record Report(boolean applied,long durationMillis,List<ScopeReport> scopes) {}

    @Transactional(readOnly=true)
    public Report preview() { return inspect(); }

    private Report inspect() {
        long started=System.nanoTime();
        List<ScopeReport> reports=new ArrayList<>();
        for (var scope : snapshots.findAllScopes()) {
            try {
                var categories=policy.effectiveCategories(scope.semesterId());
                var ledger=transactions.findAllByUserIdsAndSemesterId(List.of(scope.userId()),scope.semesterId());
                var stored=snapshots.findAllByUserIdAndSemesterId(scope.userId(),scope.semesterId()).stream()
                        .collect(Collectors.toMap(StudentSemesterPoint::getPointCategoryId,Function.identity()));
                Set<Long> ids=categories.stream().map(PointCategory::getId).collect(Collectors.toSet());
                Set<Long> parents=categories.stream().map(PointCategory::getParentCategoryId).collect(Collectors.toSet());
                var issues=ledger.stream().filter(t->!ids.contains(t.getPointCategoryId()) || parents.contains(t.getPointCategoryId()))
                        .map(t->new Issue(t.getId(),t.getPointCategoryId(),"NON_EFFECTIVE_OR_NON_LEAF_CATEGORY")).toList();
                Set<Long> backed=ledger.stream().map(PointTransaction::getPointCategoryId).collect(Collectors.toSet());
                boolean missing=stored.values().stream().anyMatch(p->p.getTotalPoint()>0 &&
                        (ledger.isEmpty() || (!parents.contains(p.getPointCategoryId()) && !backed.contains(p.getPointCategoryId()))));
                var expected=calculator.calculate(scope.userId(),scope.semesterId(),categories,ledger).points().stream()
                        .collect(Collectors.toMap(StudentSemesterPoint::getPointCategoryId,Function.identity()));
                Set<Long> all=new TreeSet<>(stored.keySet());all.addAll(expected.keySet());
                List<PointChange> changes=new ArrayList<>();
                for (Long id:all) {
                    var old=stored.get(id);var point=expected.getOrDefault(id,new StudentSemesterPoint(scope.userId(),scope.semesterId(),id,0));
                    if (old==null || !Objects.equals(old.getTotalPoint(),point.getTotalPoint()) ||
                            !Objects.equals(old.getSystemPointRecord(),point.getSystemPointRecord()) || !Objects.equals(old.getManualPointRecord(),point.getManualPointRecord())) {
                        changes.add(new PointChange(id,old==null?null:old.getTotalPoint(),point.getTotalPoint(),old==null?null:old.getSystemPointRecord(),
                                point.getSystemPointRecord(),old==null?null:old.getManualPointRecord(),point.getManualPointRecord()));
                    }
                }
                reports.add(new ScopeReport(scope.userId(),scope.semesterId(),changes,issues,ledger.stream().filter(t->"MANUAL".equals(t.getType())).map(t->new ManualRecord(t.getId(),t.getPointCategoryId(),t.getPoints(),t.getDescription())).toList(),missing,null));
            } catch (RuntimeException exception) {
                reports.add(new ScopeReport(scope.userId(),scope.semesterId(),List.of(),List.of(),List.of(),false,exception.getMessage()==null?exception.getClass().getSimpleName():exception.getMessage()));
            }
        }
        return new Report(false,(System.nanoTime()-started)/1_000_000,List.copyOf(reports));
    }

    @Transactional
    public Report repair(boolean allowMissingLedger) {
        long started=System.nanoTime();
        var report=inspect();
        if (report.scopes().stream().anyMatch(s->s.error()!=null || (!allowMissingLedger && s.missingLedger()))) {
            throw new BusinessException("Review reconciliation errors or missing-ledger balances before repair");
        }
        var scopes=report.scopes().stream().collect(Collectors.groupingBy(ScopeReport::semesterId));
        for (var entry:scopes.entrySet()) {
            command.rebuildSemesterPoints(entry.getValue().stream().map(ScopeReport::userId).toList(),entry.getKey());
        }
        return new Report(true,(System.nanoTime()-started)/1_000_000,report.scopes());
    }
}

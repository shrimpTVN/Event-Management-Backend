package com.ddd.application.service.semesterpoint.impl;

import com.ddd.application.dto.semesterpoint.*;
import com.ddd.application.service.semesterpoint.StudentSemesterPointQueryService;
import com.ddd.domain.enums.RegistrationStatusEnum;
import com.ddd.domain.exception.UnauthorizedException;
import com.ddd.domain.exception.ResourceNotFoundException;
import com.ddd.domain.model.*;
import com.ddd.domain.model.port.security.SecurityContextPort;
import com.ddd.domain.repository.*;
import com.ddd.infrastructure.repository.jpaRepository.RegistrationJpaRepository;
import com.ddd.infrastructure.repository.jpaRepository.UserJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class StudentSemesterPointQueryServiceImpl implements StudentSemesterPointQueryService {
    private static final Comparator<PointCategory> ORDER = Comparator
            .comparing(PointCategory::getChildrenOrder, Comparator.nullsLast(Comparator.naturalOrder()))
            .thenComparing(PointCategory::getId);
    private record Rank(int minimum, String label) {}
    private static final List<Rank> RANKS = List.of(new Rank(0,"Kém"),new Rank(35,"Yếu"),
            new Rank(50,"Trung Bình"),new Rank(80,"Tốt"),new Rank(90,"Xuất sắc"));
    private final UserJpaRepository users;
    private final StudentSemesterPointRepository snapshots;
    private final PointTransactionRepository transactions;
    private final SemesterPointCategoryPolicy policy;
    private final StudentSemesterPointCalculator calculator;
    private final SecurityContextPort security;
    private final RegistrationJpaRepository registrations;

    private record ReadModel(List<PointCategory> categories, Map<Long,StudentSemesterPoint> stored,
                             Map<Long,StudentSemesterPoint> points, StudentSemesterPointCalculation calculation) {}

    private Long currentUser() {
        Long id=security.getCurrentUserId();
        if (id==null) throw new UnauthorizedException("User is not authenticated");
        return id;
    }

    private ReadModel read(Long userId, Long semesterId) {
        if (userId==null || semesterId==null) throw new IllegalArgumentException("User and semester are required");
        var categories=policy.effectiveCategories(semesterId);
        var stored=snapshots.findAllByUserIdAndSemesterId(userId,semesterId).stream()
                .collect(Collectors.toMap(StudentSemesterPoint::getPointCategoryId,Function.identity()));
        var calculation=calculator.calculate(userId,semesterId,categories,
                transactions.findAllByUserIdsAndSemesterId(List.of(userId),semesterId));
        var points=calculation.points().stream().collect(Collectors.toMap(StudentSemesterPoint::getPointCategoryId,Function.identity()));
        return new ReadModel(categories,stored,points,calculation);
    }

    @Override
    public List<StudentSemesterPointInfoDto> getSemesterPointsForStudent(String email, Long semesterId) {
        var user=users.findByEmail(email);
        if (user==null) throw new ResourceNotFoundException("User not found: " + email);
        var model=read(user.getId(),semesterId);
        return model.categories().stream().sorted(ORDER).map(category -> {
            var point=model.points().get(category.getId());
            var stored=model.stored().get(category.getId());
            return new StudentSemesterPointInfoDto(stored==null?null:stored.getId(),category.getId(),
                    point.getTotalPoint(),point.getSystemPointRecord(),point.getManualPointRecord(),stored==null?null:stored.getFinalizedAt());
        }).toList();
    }

    @Override
    public List<StudentSemesterPointGroupedTreeDto> getSemesterPointTreeForCurrentStudent(Long semesterId) {
        return getSemesterPointTreeForStudent(currentUser(),semesterId);
    }

    @Override
    public List<StudentSemesterPointGroupedTreeDto> getSemesterPointTreeForStudent(Long userId, Long semesterId) {
        var model=read(userId,semesterId);
        var children=model.categories().stream().filter(c->c.getParentCategoryId()!=null && c.getParentCategoryId()!=0L)
                .collect(Collectors.groupingBy(PointCategory::getParentCategoryId));
        return roots(model).stream().map(root -> {
            List<StudentSemesterPointFlattenedItemDto> items=new ArrayList<>();
            flatten(root.getId(),children,model,items);
            var point=model.points().get(root.getId());var stored=model.stored().get(root.getId());
            var meta=model.calculation().nodeMetadata().get(root.getId());
            return new StudentSemesterPointGroupedTreeDto(root.getId(),root.getName(),root.getNote(),root.getMaximum(),
                    root.getDateApply(),root.getLevel(),root.getParentCategoryId(),root.isActive(),root.getChildrenOrder(),root.isMaxChild(),
                    stored==null?null:stored.getId(),point.getTotalPoint(),point.getSystemPointRecord(),point.getManualPointRecord(),
                    stored==null?null:stored.getFinalizedAt(),meta.isAtMaximum(),meta.completionPercentage(),meta.selectedChildId(),meta.contributesToParent(),items);
        }).toList();
    }

    private List<PointCategory> roots(ReadModel model) {
        return model.categories().stream().filter(c->c.getParentCategoryId()==null || c.getParentCategoryId()==0L).sorted(ORDER).toList();
    }

    private void flatten(Long parentId, Map<Long,List<PointCategory>> children, ReadModel model,
                         List<StudentSemesterPointFlattenedItemDto> items) {
        for (var child : children.getOrDefault(parentId,List.of()).stream().sorted(ORDER).toList()) {
            var point=model.points().get(child.getId());var stored=model.stored().get(child.getId());
            var meta=model.calculation().nodeMetadata().get(child.getId());
            items.add(new StudentSemesterPointFlattenedItemDto(child.getId(),child.getName(),child.getNote(),child.getMaximum(),
                    child.getDateApply(),child.getLevel(),child.getParentCategoryId(),child.isActive(),child.getChildrenOrder(),
                    !children.containsKey(child.getId()),child.isMaxChild(),stored==null?null:stored.getId(),point.getTotalPoint(),
                    point.getSystemPointRecord(),point.getManualPointRecord(),stored==null?null:stored.getFinalizedAt(),
                    meta.isAtMaximum(),meta.completionPercentage(),meta.selectedChildId(),meta.contributesToParent()));
            flatten(child.getId(),children,model,items);
        }
    }

    @Override
    public StudentSemesterPointSummaryDto getSummaryForCurrentStudent(Long semesterId) {
        Long userId=currentUser();var model=read(userId,semesterId);
        int total=Math.toIntExact(roots(model).stream().mapToLong(c->Math.min(c.getMaximum(),model.points().get(c.getId()).getTotalPoint())).sum());
        int effective=Math.min(100,total);
        int rankIndex=0;
        while (rankIndex+1<RANKS.size() && effective>=RANKS.get(rankIndex+1).minimum()) rankIndex++;
        var rank=RANKS.get(rankIndex);
        Rank next=rankIndex+1<RANKS.size()?RANKS.get(rankIndex+1):null;
        int activities=Math.toIntExact(registrations.countByUserIdAndEventSemesterIdAndStatusIn(userId,semesterId,
                List.of(RegistrationStatusEnum.COMPLETED,RegistrationStatusEnum.CONFIRMED,RegistrationStatusEnum.RECORDED)));
        return new StudentSemesterPointSummaryDto(semesterId,total,100,rank.label(),next==null?null:next.label(),next==null?0:next.minimum()-effective,activities);
    }
}

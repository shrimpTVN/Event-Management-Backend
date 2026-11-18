package com.ddd.application.service.semesterpoint.impl;

import com.ddd.application.dto.semesterpoint.PointTransactionRequestInfoDto;
import com.ddd.application.mapper.PointTransactionDtoMapper;
import com.ddd.application.service.semesterpoint.PointTransactionCommandService;
import com.ddd.application.service.semesterpoint.StudentSemesterPointCommandService;
import com.ddd.domain.enums.PointTransactionStatusEnum;
import com.ddd.domain.enums.PointTransactionTypeEnum;
import com.ddd.domain.exception.ResourceNotFoundException;
import com.ddd.domain.model.PointTransaction;
import com.ddd.domain.model.port.security.SecurityContextPort;
import com.ddd.domain.repository.PointCategoryRepository;
import com.ddd.domain.repository.PointTransactionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Objects;

@Service
@RequiredArgsConstructor
@Transactional
public class PointTransactionCommandServiceImpl implements PointTransactionCommandService {
    private final PointTransactionDtoMapper pointTransactionDtoMapper;
    private final PointTransactionRepository pointTransactionRepository;
    private final PointCategoryRepository pointCategoryRepository;
    private final StudentSemesterPointCommandService studentSemesterPointCommandService;
    private final SecurityContextPort securityContextPort;

    @Override
    public void createPointTransactionForStudent(PointTransactionRequestInfoDto pointTransactionRequestInfoDto) {
        Long userId = securityContextPort.getCurrentUserId();
        boolean isLeaf = !pointCategoryRepository.hasChildren(pointTransactionRequestInfoDto.pointCategoryId());
        if (!isLeaf) {
            throw new IllegalArgumentException("Only can create point transaction for leaf point category");
        }

        PointTransaction pointTransaction = createPointTransaction(pointTransactionRequestInfoDto, userId);
        pointTransactionRepository.save(pointTransaction);

        // Record leaf point and propagate upward to all ancestors
        studentSemesterPointCommandService.recordPointAndPropagate(
                userId,
                pointTransactionRequestInfoDto.semesterId(),
                pointTransactionRequestInfoDto.pointCategoryId(),
                pointTransactionRequestInfoDto.points(),
                PointTransactionTypeEnum.MANUAL
        );
    }

    @Override
    public void deletePointTransactionForStudent(Long pointTransactionId) {
        Long userId = securityContextPort.getCurrentUserId();
        PointTransaction transaction = pointTransactionRepository.findById(pointTransactionId)
                .orElseThrow(() -> new ResourceNotFoundException("Point transaction not found with id: " + pointTransactionId));

        if (!Objects.equals(transaction.getUserId(), userId) || !PointTransactionTypeEnum.MANUAL.name().equals(transaction.getType())) {
            throw new IllegalArgumentException("You can't delete this transaction because you have not created this transaction by yourself.");
        }

        pointTransactionRepository.delete(transaction);

        // Deduct leaf points and propagate upward to all ancestors
        studentSemesterPointCommandService.recordPointAndPropagate(
                userId,
                transaction.getSemesterId(),
                transaction.getPointCategoryId(),
                -transaction.getPoints(),
                PointTransactionTypeEnum.MANUAL
        );
    }

    private PointTransaction createPointTransaction(PointTransactionRequestInfoDto pointTransactionRequestInfoDto, Long userId) {
        PointTransaction pointTransaction = pointTransactionDtoMapper.toDomain(pointTransactionRequestInfoDto);
        pointTransaction.setUserId(userId);
        pointTransaction.setType(PointTransactionTypeEnum.MANUAL.name());
        pointTransaction.setDescription(pointTransaction.getDescription() == null ?
                "Manual point transaction" : pointTransaction.getDescription());
        pointTransaction.setStatus(PointTransactionStatusEnum.CONFIRMED.name());
        return pointTransaction;
    }
}

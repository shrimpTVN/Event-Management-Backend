package com.ddd.application.service.semesterpoint.impl;

import com.ddd.application.dto.semesterpoint.PointTransactionInfoDto;
import com.ddd.application.dto.semesterpoint.PointTransactionRequestInfoDto;
import com.ddd.application.mapper.PointTransactionDtoMapper;
import com.ddd.application.service.semesterpoint.PointTransactionCommandService;
import com.ddd.domain.enums.PointTransactionStatusEnum;
import com.ddd.domain.enums.PointTransactionTypeEnum;
import com.ddd.domain.model.PointTransaction;
import com.ddd.domain.model.StudentSemesterPoint;
import com.ddd.domain.model.User;
import com.ddd.domain.repository.PointTransactionRepository;
import com.ddd.domain.repository.StudentSemesterPointRepository;
import com.ddd.domain.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class PointTransactionCommandServiceImpl implements PointTransactionCommandService {
    private final PointTransactionDtoMapper pointTransactionDtoMapper;
    private final PointTransactionRepository pointTransactionRepository;
    private final UserRepository userRepository;
    private final StudentSemesterPointRepository studentSemesterPointRepository;


    @Override
    @Transactional
    public void createPointTransactionForStudent(String email, PointTransactionRequestInfoDto pointTransactionRequestInfoDto) {
        User user = userRepository.findByEmail(email);
        PointTransaction pointTransaction = createPointTransaction(pointTransactionRequestInfoDto, user);
        StudentSemesterPoint studentSemesterPoint = createStudentSemesterPoint(pointTransactionRequestInfoDto, user);

        pointTransactionRepository.save(pointTransaction);
        //update student semester point
        studentSemesterPointRepository.save(studentSemesterPoint);
    }

    private PointTransaction createPointTransaction(PointTransactionRequestInfoDto pointTransactionRequestInfoDto, User user){
        PointTransaction pointTransaction = pointTransactionDtoMapper.toDomain(pointTransactionRequestInfoDto);
        pointTransaction.setUserId(user.getId());
        pointTransaction.setType(PointTransactionTypeEnum.MANUAL.name());
        pointTransaction.setDescription( pointTransaction.getDescription() == null ?
                "Manual point transaction" : pointTransaction.getDescription());
        pointTransaction.setStatus(PointTransactionStatusEnum.CONFIRMED.name());
        return pointTransaction;
    }

    private StudentSemesterPoint createStudentSemesterPoint( PointTransactionRequestInfoDto pointTransactionRequestInfoDto, User user) {
        StudentSemesterPoint studentSemesterPoint = studentSemesterPointRepository.findByUserIdAndSemesterIdAndPointCategoryId(user.getId(),
                pointTransactionRequestInfoDto.semesterId(), pointTransactionRequestInfoDto.pointCategoryId());

        if (studentSemesterPoint == null) {
            studentSemesterPoint = new StudentSemesterPoint();
            studentSemesterPoint.setUserId(user.getId());
            studentSemesterPoint.setSemesterId(pointTransactionRequestInfoDto.semesterId());
            studentSemesterPoint.setPointCategoryId(pointTransactionRequestInfoDto.pointCategoryId());
            studentSemesterPoint.setTotalPoint(pointTransactionRequestInfoDto.points());
        } else {
            studentSemesterPoint.setTotalPoint(studentSemesterPoint.getTotalPoint() + pointTransactionRequestInfoDto.points());
        }
        return studentSemesterPoint;
    }
}

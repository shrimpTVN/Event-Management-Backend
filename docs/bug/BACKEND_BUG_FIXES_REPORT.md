# BÁO CÁO TỔNG HỢP LỖI BACKEND CẦN KHẮC PHỤC
**Dự án:** Event Management System (Backend Spring Boot DDD)  
**Ngày báo cáo:** 05/11/2026  
**Người tổng hợp:** Frontend Team / System Pair Programmer  

---

## MỤC LỤC
1. [Lỗi 1 (Nghiêm trọng): Lỗi 500 NullPointerException khi Sinh viên Tự Đánh Giá Tiêu Chí (POST `/api/criteria-transactions/me`)](#lỗi-1-nghiêm-trọng-lỗi-500-nullpointerexception-khi-sinh-viên-tự-đánh-giá-tiêu-chí)
2. [Lỗi 2 (Nghiêm trọng): Lỗi vi phạm ràng buộc NOT NULL cột `is_completed` trong bảng `student_criteria_progresses`](#lỗi-2-nghiêm-trọng-lỗi-vi-phạm-ràng-buộc-not-null-cột-is_completed)
3. [Lỗi 3 (Logic nghiệp vụ): Tiêu chuẩn và Tiêu chí cha Sinh viên 5 tốt không cập nhật trạng thái "Đạt" khi các tiêu chí con đã đạt](#lỗi-3-logic-nghiệp-vụ-tiêu-chuẩn-và-tiêu-chí-cha-không-cập-nhật-trạng-thái-đạt)
4. [Lỗi 4: API `/api/academic-years` luôn trả về `"isActive": false` dù trong Database là `true`](#lỗi-4-api-apiacademic-years-luôn-trả-về-isactive-false)
5. [Lỗi 5: Thiếu phương thức trong `FanpageMemberRepository` sau khi merge code](#lỗi-5-thiếu-phương-thức-trong-fanpagememberrepository)

---

### LỖI 1 (NGHIÊM TRỌNG): Lỗi 500 NullPointerException khi Sinh viên Tự Đánh Giá Tiêu Chí
- **Endpoint bị lỗi:** `POST /api/criteria-transactions/me`
- **Hiện tượng:** Khi sinh viên gửi minh chứng tự đánh giá, hệ thống trả về HTTP Status `500 Internal Server Error`:
  ```
  java.lang.NullPointerException: element cannot be mapped to a null key
      at java.base/java.util.Objects.requireNonNull(Objects.java:233)
      at java.base/java.util.stream.Collectors.lambda$groupingBy$70(Collectors.java:1255)
      ...
      at com.ddd.application.service.criteriaprogress.impl.StudentCriteriaProgressCommandServiceImpl.upsertProgressTree(StudentCriteriaProgressCommandServiceImpl.java:126)
  ```
- **Nguyên nhân gốc rễ:**
  - File: `ddd-infrastructure/src/main/java/com/ddd/infrastructure/mapper/StudentCriteriaProgressMapper.java`
  - MapStruct khi map từ `StudentCriteriaProgressJpaEntity` sang Domain Model `StudentCriteriaProgress` thiếu cấu hình mapping các trường ID quan hệ: `user.id -> userId`, `criteria.id -> criteriaId`, `academicYear.id -> academicYearId`.
  - Kết quả là `StudentCriteriaProgress.getUserId()` trả về `null`.
  - Khi `StudentCriteriaProgressCommandServiceImpl.java` dòng 126 thực hiện:
    ```java
    Map<Long, List<StudentCriteriaProgress>> progressByUser = progresses.stream()
        .collect(Collectors.groupingBy(StudentCriteriaProgress::getUserId));
    ```
    Java Stream `Collectors.groupingBy` cấm null key nên quăng ra `NullPointerException`.

- **Vị trí cần sửa:**
  - File: `ddd-infrastructure/src/main/java/com/ddd/infrastructure/mapper/StudentCriteriaProgressMapper.java`

- **Đoạn code cần sửa:**
  ```java
  // TRƯỚC KHI SỬA:
  @Mapper(componentModel = "spring")
  public interface StudentCriteriaProgressMapper {
      StudentCriteriaProgress toModel(StudentCriteriaProgressJpaEntity entity);
      StudentCriteriaProgressJpaEntity toEntity(StudentCriteriaProgress model);
  }
  ```

  ```java
  // SAU KHI SỬA (Bổ sung @Mapping):
  @Mapper(componentModel = "spring")
  public interface StudentCriteriaProgressMapper {
      @Mapping(source = "user.id", target = "userId")
      @Mapping(source = "criteria.id", target = "criteriaId")
      @Mapping(source = "academicYear.id", target = "academicYearId")
      StudentCriteriaProgress toModel(StudentCriteriaProgressJpaEntity entity);

      @Mapping(source = "userId", target = "user.id")
      @Mapping(source = "criteriaId", target = "criteria.id")
      @Mapping(source = "academicYearId", target = "academicYear.id")
      StudentCriteriaProgressJpaEntity toEntity(StudentCriteriaProgress model);
  }
  ```

---

### LỖI 2 (NGHIÊM TRỌNG): Lỗi vi phạm ràng buộc NOT NULL cột `is_completed`
- **Hiện tượng:** Sau khi fix lỗi 1, lưu đánh giá có thể gặp lỗi DB:
  `null value in column "is_completed" of relation "student_criteria_progresses" violates not-null constraint`
- **Nguyên nhân gốc rễ:**
  - Trong domain model `StudentCriteriaProgress.java`, trường `isCompleted` không có giá trị mặc định (`null`).
  - Trong `StudentCriteriaProgressCommandServiceImpl.java` (hàm tính tiến độ tiêu chí cha `parentProgress`), khi tiêu chí cha chưa đủ số lượng con đạt điều kiện (`requiredChildren`), biến boolean hoặc getter `isCompleted` bị bỏ qua hoặc mang giá trị `null`.
  - Khi JPA insert/update vào database, cột `is_completed` bị gán `null`, vi phạm ràng buộc DB.

- **Vị trí cần sửa:**
  1. `ddd-domain/src/main/java/com/ddd/domain/model/StudentCriteriaProgress.java`
  2. `ddd-application/src/main/java/com/ddd/application/service/criteriaprogress/impl/StudentCriteriaProgressCommandServiceImpl.java`

- **Đoạn code cần sửa:**
  - Trong `StudentCriteriaProgress.java`:
    ```java
    // Đảm bảo isCompleted luôn có giá trị boolean mặc định (false):
    private Boolean isCompleted = Boolean.FALSE;

    // Trong constructor hoặc setter:
    public void setIsCompleted(Boolean isCompleted) {
        this.isCompleted = isCompleted != null ? isCompleted : Boolean.FALSE;
    }
    ```
  - Trong `StudentCriteriaProgressCommandServiceImpl.java` (vị trí tính parent progress ~ dòng 140 - 155):
    ```java
    // Đảm bảo luôn gán giá trị rõ ràng (true hoặc false), không để null:
    boolean completed = childProgresses.stream()
        .filter(p -> Boolean.TRUE.equals(p.getIsCompleted()))
        .count() >= (parent.getRequiredChildren() != null ? parent.getRequiredChildren() : 1);
    
    parentProgress.setIsCompleted(completed);
    ```

---

### LỖI 3 (LOGIC NGHIỆP VỤ): Tiêu chuẩn và Tiêu chí cha không cập nhật trạng thái "Đạt"
- **Endpoint:** `GET /api/criteria-progress/me?academicYearId={id}`
- **Hiện tượng:**
  - Khi sinh viên đã hoàn thành đủ các tiêu chí con của một Tiêu chuẩn (ví dụ Đạo đức tốt, Học tập tốt...), trên giao diện tiêu chuẩn cha vẫn hiển thị "Chưa đạt" (hoặc `isCompleted: false`).
  - Dữ liệu trả về từ API chỉ phản ánh bản ghi có sẵn trong DB cho tiêu chí lá; các node cha cấp 1 (5 Tiêu chuẩn) và cấp 0 không được tính toán tổng hợp (Rollup calculation).
  - Ngoài ra, cấu trúc trả về đang bọc thêm 1 cấp node gốc (Level 0: "Sinh viên 5 tốt cấp Trường") thay vì trả về trực tiếp danh sách 5 Tiêu chuẩn.
- **Nguyên nhân gốc rễ:**
  - File: `ddd-application/src/main/java/com/ddd/application/service/criteriaprogress/impl/StudentCriteriaProgressQueryServiceImpl.java`
  - Hàm `attachProgress` chỉ lấy giá trị `progressMap.get(node.id())`. Nếu DB chưa kịp ghi hoặc chưa có bản ghi cha, `isCompleted` luôn là `false`.
  - Cần tính toán đệ quy từ dưới lên (Bottom-up rollup): Một tiêu chuẩn/tiêu chí cha được coi là ĐẠT (`isCompleted = true`) nếu:
    - `progressMap.get(node.id())` đánh dấu hoàn thành, **HOẶC**
    - Số lượng tiêu chí con hoàn thành (`child.isCompleted == true`) $\ge$ `node.requiredChildren` (nếu `requiredChildren` null thì lấy tổng số tiêu chí con).

- **Đoạn code đề xuất sửa:**
  Trong `StudentCriteriaProgressQueryServiceImpl.java`:
  ```java
  private CriteriaProgressItemDto attachProgress(
          CriteriaNodeDto node,
          Map<Long, StudentCriteriaProgress> progressMap) {
      
      List<CriteriaProgressItemDto> childDtos = node.children() == null ? List.of() :
              node.children().stream()
                      .map(child -> attachProgress(child, progressMap))
                      .toList();

      StudentCriteriaProgress nodeProgress = progressMap.get(node.id());
      boolean dbCompleted = nodeProgress != null && Boolean.TRUE.equals(nodeProgress.getIsCompleted());

      // Rollup logic: Nếu có node con, kiểm tra điều kiện đạt từ các node con
      boolean calculatedCompleted = dbCompleted;
      if (!childDtos.isEmpty()) {
          long completedChildrenCount = childDtos.stream()
                  .filter(CriteriaProgressItemDto::isCompleted)
                  .count();
          int required = node.requiredChildren() != null && node.requiredChildren() > 0 
                  ? node.requiredChildren() 
                  : childDtos.size();
          calculatedCompleted = dbCompleted || (completedChildrenCount >= required);
      }

      int completedCount = nodeProgress != null && nodeProgress.getCompletedCount() != null
              ? nodeProgress.getCompletedCount()
              : (int) childDtos.stream().filter(CriteriaProgressItemDto::isCompleted).count();

      return new CriteriaProgressItemDto(
              node.id(),
              node.name(),
              node.criteriaCode(),
              node.level(),
              node.requiredChildren(),
              calculatedCompleted,
              completedCount,
              nodeProgress != null ? nodeProgress.getEvidenceUrl() : null,
              nodeProgress != null ? nodeProgress.getStudentNote() : null,
              childDtos
      );
  }
  ```

---

### LỖI 4: API `/api/academic-years` luôn trả về `"isActive": false`
- **Endpoint:** `GET /api/academic-years`
- **Hiện tượng:** Mặc dù trong bảng `academic_years` có năm học đang được set `is_active = true` (hoặc `active = true`), nhưng JSON trả về từ API luôn là `"isActive": false`.
- **Nguyên nhân gốc rễ:**
  - File: `ddd-application/src/main/java/com/ddd/application/mapper/AcademicYearDtoMapper.java`
  - MapStruct không tự động map giữa trường `active` của Domain Model và `isActive` của DTO do khác biệt tiền tố `is`.
- **Đoạn code cần sửa:**
  ```java
  @Mapper(componentModel = "spring")
  public interface AcademicYearDtoMapper {
      @Mapping(source = "active", target = "isActive")
      AcademicYearResponseDto toDto(AcademicYear academicYear);
      
      List<AcademicYearResponseDto> toDtoList(List<AcademicYear> academicYears);
  }
  ```

---

### LỖI 5: Thiếu phương thức trong `FanpageMemberRepository`
- **Hiện tượng:** Sau khi pull code mới về từ nhánh của backend, `FanpageCommandServiceImpl.java` gọi các phương thức không tồn tại trong interface `FanpageMemberRepository.java`:
  - `isFanpageAdmin(Long fanpageId, Long userId)`
  - `isFanpageMember(Long fanpageId, Long userId)`
  - `findByFanpageIdAndUserId(Long fanpageId, Long userId)`
- **Nguyên nhân:** Có thể do conflict khi merge PR hoặc nhánh chưa đẩy đủ code domain repository interface.
- **Vị trí cần sửa:**
  - Thêm khai báo vào `ddd-domain/src/main/java/com/ddd/domain/repository/FanpageMemberRepository.java`:
    ```java
    boolean isFanpageAdmin(Long fanpageId, Long userId);
    boolean isFanpageMember(Long fanpageId, Long userId);
    Optional<FanpageMember> findByFanpageIdAndUserId(Long fanpageId, Long userId);
    ```
  - Và implement tương ứng trong `ddd-infrastructure/src/main/java/com/ddd/infrastructure/repository/FanpageMemberRepositoryImpl.java`.

---

## TỔNG KẾT HỖ TRỢ PHÍA FRONTEND
Hiện tại để đảm bảo trải nghiệm người dùng không bị gián đoạn, phía **Frontend** đã:
1. Đã fallback tính toán trạng thái "Đạt / Chưa đạt" đệ quy ngay tại giao diện để sinh viên thấy đúng tiến độ khi đủ điều kiện.
2. Đã khắc phục lỗi hiển thị điểm rèn luyện (giới hạn chặn trần điểm tối đa theo từng mục tiêu chuẩn).
3. Đã chuẩn hóa toàn bộ giao diện Modal chi tiết tiêu chí SV5T theo đúng thiết kế phẳng, gọn gàng, đồng bộ với Điểm rèn luyện.

👉 **Team Backend chỉ cần review và áp dụng các mục trên (đặc biệt Lỗi 1 và Lỗi 2) để API hoàn toàn ổn định và lưu trữ dữ liệu tự đánh giá chính xác.**


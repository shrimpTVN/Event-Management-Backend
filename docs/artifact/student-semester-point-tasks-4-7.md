# Bàn giao refactor điểm học kỳ — task 4–7

Ngày 27/11/2026. Phạm vi chốt với người dùng: **hoàn tất local**, chưa triển khai hoặc repair dữ liệu môi trường thật. Race condition tiếp tục nằm ngoài đợt này.

## Vị trí code

- BE: `/home/shrimp/code/EventManagement/ddd`.
- FE: `/home/shrimp/code/EventManagement/event_management_frontend`.
- Cả hai repo dùng nhánh `codex/semester-points-server-driven`, chỉnh trực tiếp trên repo. `/tmp` chỉ chứa log và script hỗ trợ kiểm thử, không phải bản triển khai cần chép về.
- Giữ nguyên thay đổi có sẵn của người dùng ở `StudentCriteriaProgressQueryServiceImpl.java` và hai artifact SV5T/patch task 1–3; không đưa chúng vào commit refactor này.

## Đã triển khai

### Task 4: admin thay cấu hình

`PointConfigurationRefresh` chụp cây hiệu lực trước/sau theo từng học kỳ có transaction hoặc snapshot. Khi ID, parent, maximum hoặc isMaxChild thay đổi, BE rebuild tất cả sinh viên có dữ liệu của học kỳ đó trong transaction cập nhật cấu hình. Ngày áp dụng, trạng thái hoạt động và ngày học kỳ được phản ánh qua cây hiệu lực.

Tạo/sửa/xóa danh mục và sửa học kỳ đã gọi refresh. Cho phép root có parent null, chặn chu trình trước khi lưu, cập nhật level hậu duệ khi chuyển cha. API/application DTO chấp nhận maximum bằng 0. Các node ngoài cây được đưa snapshot về 0 khi rebuild; transaction giữ nguyên.

Khi cha không còn hoạt động hoặc không thuộc bộ hiệu lực, loại cả nhánh con không còn đường đến root khỏi cây tính điểm. Đây là lọc cây hiệu lực, không tự tắt các bản ghi danh mục con trong DB.

Rebuild cấu hình vẫn đồng bộ, chưa chia job hoặc thêm khóa. Có thể phải đọc nhiều cặp user/semester; cần đo trên staging trước khi áp dụng cho dữ liệu lớn.

### Task 5: API do BE tổng hợp

- GET `/student-semester-points/grouped-tree/{semesterId}` giữ totalPoint và hai nguồn điểm **thô**. Trả thêm `isAtMaximum`, `completionPercentage`, `selectedChildId`, `contributesToParent` cho root và item.
- GET `/student-semester-points/me/semesters/{semesterId}` trả cùng raw totalPoint cho cùng node.
- GET `/student-semester-points/me/semesters/{semesterId}/summary` trả `semesterId`, `totalPoint`, `maximum`, `classificationLabel`, `nextClassificationLabel`, `pointsNeeded`, `activitiesCount`.
- Query dùng chung calculator trên ledger hiện tại, ghép ID/finalizedAt từ snapshot, không ghi DB khi GET.
- Summary cộng điểm hiệu lực root; maximum học kỳ là 100. Xếp loại dùng điểm học kỳ sau trần: dưới 35 Kém, 35–49 Yếu, 50–79 Trung Bình, 80–89 Tốt, từ 90 Xuất sắc. Giữ nhãn 65–79 theo kế hoạch đã duyệt.
- Activities chỉ đếm đăng ký của đúng sinh viên và học kỳ có status COMPLETED, CONFIRMED, RECORDED. Đây là status **registration**, không phải điều kiện lọc transaction điểm.

### Task 6: FE

Bảng, modal và sidebar hiển thị điểm node bằng `min(maximum, totalPoint)`. FE không cộng các nhánh, chọn nhánh MAX, xếp loại hoặc tính pointsNeeded. Nguồn ghi nhận và progress/badge lấy từ BE. Bỏ overlay điểm đã lưu và fallback danh mục khác học kỳ; sidebar không fallback semester ID 1.

Trang chính và sidebar dùng cùng tree/summary API, query key gồm user và semester. Mutation chờ invalidate/refetch tree, summary, history. Lỗi giữ form/draft và báo lỗi. Không thay tổng manual bằng số điểm vừa nhập.

Thêm gọi POST; sửa gọi PATCH theo transaction ID với điểm của đúng transaction; xóa gọi DELETE theo ID. Modal liệt kê từng ghi nhận tay để chọn sửa/xóa. Một giao dịch 8 sửa thành 5 còn 5; thêm mới 5 vào 8 thành 13 trước trần.

### Task 7: đối soát local

`SemesterPointReconciliationService` dùng calculator chung. Preview không ghi DB; báo chênh lệch từng node, giao dịch gắn cha/ngoài cây, snapshot có điểm thiếu ledger, cùng từng manual ID/điểm/note để người sở hữu dữ liệu xem xét. Không gộp các ghi nhận tay giống nhau và không lọc transaction theo status.

Repair dựng lại snapshot, bảo toàn transaction và ID/audit snapshot đang có; snapshot cũ ngoài cây thành 0. Lỗi một scope rollback toàn bộ repair. Mặc định chặn repair nếu có lỗi hoặc số dư thiếu ledger. `allow-missing-ledger=true` chỉ dùng sau khi đã quyết định chấp nhận mất phần số dư không có nguồn.

Consumer audit qua CodeGraph và tìm kiếm toàn source: cập nhật cả consumer JPA trong `EventRecommendationServiceImpl` và SQL snapshot trong `RecommendationRepositoryImpl` để nhận điểm hiệu lực. Không tìm thấy consumer statistic runtime khác đọc total_point. Các SQL seed/hotfix lịch sử còn chứa công thức cũ; không dùng chúng làm cơ chế repair. Nếu nạp dữ liệu demo bằng các script đó, chạy preview rồi repair bằng calculator mới trước khi đối chiếu snapshot.

## Kiểm chứng

- **79 test BE liên quan pass**: 59 test điểm/cấu hình/query/API/đối soát, cộng 20 test policy/thưởng sự kiện/event/recommendation.
- **40 test FE pass**, gồm service contract, raw total vượt trần, root và MAX metadata từ BE, modal chọn đúng transaction, POST/PATCH/DELETE và refetch/reload, request cũ khi đổi học kỳ, summary/sidebar và lỗi/thiếu cấu hình.
- TypeScript check và FE production build pass. BE `mvn -o -B -pl ddd-start -am -DskipTests package` pass.
- PostgreSQL trong container riêng dùng fixture tạm: native UNION lấy cả ledger-only scope và không trùng; lỗi scope thứ hai rollback cả lần ghi scope đầu.
- Fixture PostgreSQL 100 sinh viên, 200 manual transaction, 2 node/sinh viên: preview có 200 dòng khác, repair khớp raw leaf 15 và root 10; repair lần hai không còn chênh lệch, ledger vẫn 200 dòng. Lần đo local: preview **1114 ms**, repair **2191 ms**. Adapter fixture JDBC và bảng tối giản, không đại diện throughput JPA/DB production.
- Smoke test **jar đóng gói thật** trên PostgreSQL tạm với schema entity hiện hành: preview → repair → preview thành công. Root 999 được sửa thành 10, leaf 10 thành raw 15; hai manual 8 (PENDING) và 7 (REVOKED) đều được tính. ID, created_at, finalized_at của snapshot và ID/nội dung transaction giữ nguyên. Preview cuối không còn chênh lệch. Báo cáo fixture lưu tại `docs/artifact/student-semester-point-reconcile-local-example.json`. Schema.sql nền còn thiếu cột entity hiện hành; fixture được đồng bộ bằng Hibernate update **chỉ trên DB tạm** trước smoke test.
- Lượt review độc lập cuối không phát hiện blocker sau khi sửa validation trần 0.
- Suite RegistrationCommandServiceImplTest ngoài phạm vi có baseline 9 failure + 4 error đã được kiểm chứng ở task 1–3; không tuyên bố toàn bộ suite repository pass.

### Chạy lại BE test

Tại repo BE, dùng JDK 21 hoặc cấu hình compiler hiện có. `-javaagent` dùng đúng phiên bản Mockito trong Maven cache của máy.

```sh
mvn -o -B -pl ddd-api -am \
  -DargLine=-javaagent:/home/shrimp/.m2/repository/org/mockito/mockito-core/5.23.0/mockito-core-5.23.0.jar \
  -Dtest=PointConfigurationRebuildTest,PointConfigurationRefreshTest,SemesterPointReconciliationServiceTest,StudentSemesterPointCalculatorTest,StudentSemesterPointCommandServiceImplTest,StudentSemesterPointQueryServiceImplTest,StudentSemesterPointControllerTest,PointCategoryServiceImplTest,SemesterServiceImplTest,PointTransactionCommandServiceImplTest,PointTransactionRollbackTest,PointTransactionControllerTest,SemesterPointCategoryPolicyTest,RegistrationPointAwardTest,EventCommandServiceImplTest,EventRecommendationServiceImplTest \
  -Dsurefire.failIfNoSpecifiedTests=false test
```

Integration test chạy riêng với PostgreSQL dành riêng cho test, user postgres có quyền tạo schema. Test tạo/xóa schema riêng, không dùng schema ứng dụng:

```sh
mvn -o -B -pl ddd-application -am \
  -DargLine=-javaagent:/home/shrimp/.m2/repository/org/mockito/mockito-core/5.23.0/mockito-core-5.23.0.jar \
  -DPOINTS_TEST_JDBC_URL=jdbc:postgresql://127.0.0.1:55483/points_test \
  -Dtest=SemesterPointReconciliationPostgresIT -Dsurefire.failIfNoSpecifiedTests=false test
```

### Chạy lại FE test

```sh
npm run test -- src/features/training-points src/pages/__tests__/TrainingPointsPage.test.tsx src/features/events/components/__tests__/StudentRightSidebar.test.tsx src/features/sv5t/components/__tests__/AchievementAccordionResponsive.test.tsx
npm run typecheck
npm run build
```

## Hướng dẫn runner cho lần triển khai sau

Build jar từ repo BE:

```sh
mvn -pl ddd-start -am -DskipTests package
```

Cấu hình DATABASE_HOST, DATABASE_PORT, DATABASE_NAME, DATABASE_USERNAME, DATABASE_PASSWORD bằng cơ chế cấu hình hiện có cho **môi trường đã chọn**. Runner bật bằng profile riêng, mặc định chỉ đọc. Các tham số cấu hình khác của ứng dụng vẫn áp dụng. Ví dụ dưới dùng profile default cho log local; khi chạy môi trường khác, thay default bằng profile vận hành tương ứng (ví dụ prod) và dùng DB đã có đầy đủ migration/schema hiện hành. `server.port=0` tránh chiếm cổng của instance đang chạy; không dùng `web-application-type=none` vì ứng dụng có security web config.

```sh
java -jar ddd-start/target/ddd-start-0.0.1-SNAPSHOT.jar \
  --spring.profiles.active=default,semester-point-reconcile \
  --spring.docker.compose.enabled=false \
  --spring.jpa.hibernate.ddl-auto=none \
  --server.port=0 \
  --points.reconcile.output=/absolute/path/preview.json
```

Ứng dụng ghi JSON rồi tự đóng context. Tạo sẵn thư mục output có quyền ghi. `scopes[].changes` là giá trị trước/sau, `issues` là transaction không hợp lệ, `manualRecords` giữ ID/points/note, `missingLedger` và `error` cần được xem xét. `applied=false` nghĩa là preview. Snapshot dương không có nguồn không được tự chuyển thành manual.

Sau khi xem báo cáo, quyết định dữ liệu và chốt phạm vi repair, dùng cùng command thêm:

```sh
--points.reconcile.apply=true --points.reconcile.output=/absolute/path/repair.json
```

Nếu đã quyết định chấp nhận reset số dư thiếu nguồn, thêm `--points.reconcile.allow-missing-ledger=true`. Cờ này không bỏ qua lỗi cấu hình/calculator. Report repair mô tả các thay đổi dự kiến trước khi rebuild; sau repair chạy preview lần nữa để kiểm tra không còn chênh lệch.

Trong lúc preview/repair cần giữ ledger và cấu hình ổn định vì race condition chưa được xử lý. BE và FE phải lên cùng đợt do totalPoint nay có thể vượt maximum. Backup DB trước repair môi trường thật theo quy trình vận hành. Nếu rollback phiên bản, restore snapshot phù hợp hoặc tính lại theo phiên bản cũ; rollback code không tự đảo ngữ nghĩa dữ liệu đã repair.

## Việc dành cho lần triển khai môi trường đích

Theo lựa chọn **chỉ local trước** của người dùng, các bước này chưa thực hiện và không thuộc việc triển khai local còn dang dở:

1. Chọn staging/production, chạy preview dữ liệu thật, xem manualRecords/ngoại lệ và quyết định phạm vi repair.
2. Đo thời gian với cây/số sinh viên thực tế, sắp xếp thời điểm dữ liệu ổn định.
3. Triển khai BE/FE cùng đợt, repair rồi preview lại.
4. Smoke test UI thật: thêm/sửa/xóa manual, chốt event, admin đổi trần/cây, reload, đổi học kỳ; đối chiếu ledger, snapshot, response và màn hình.
5. Refactor race condition trong đợt riêng.

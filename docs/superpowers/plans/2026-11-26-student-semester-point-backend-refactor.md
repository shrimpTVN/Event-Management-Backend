# Kế hoạch refactor điểm học kỳ do Backend tổng hợp

> **For agentic workers** — Dùng skill `executing-plans` để triển khai từng task sau khi kế hoạch được duyệt. Các bước có checkbox để theo dõi tiến độ.

**Goal** — BE xử lý tính toán và lưu trữ điểm học kỳ. FE đọc dữ liệu BE, hiển thị điểm hiệu lực của một node bằng `min(maximum, totalPoint)`.

**Architecture** — Transaction điểm là dữ liệu nguồn. Một bộ tổng hợp chung ở BE dựng lại điểm lá và các cấp cha theo bộ danh mục có hiệu lực của học kỳ. Các thao tác ghi điểm và thay đổi cấu hình gọi cùng logic để lưu snapshot `student_semester_points`. API đọc dùng cùng calculator trên dữ liệu nguồn hiện hành để trả cây, metadata và summary đồng nhất. GET chỉ đọc dữ liệu. FE dùng response cho bảng, modal và sidebar.

**Tech Stack** — Java, Spring Boot, JPA, MapStruct, PostgreSQL, React, TypeScript, TanStack Query, Vitest.

**Spec** — Yêu cầu trong chat ngày 26/11/2026 và phần Thiết kế nghiệp vụ trong tài liệu này. Quyết định bổ sung để sửa hành vi lưu nhiều lần được ghi rõ bên dưới.

## Global Constraints

- Phạm vi lần này là thuật toán, dữ liệu lưu, contract API và hiển thị FE. Refactor race condition được dành cho giai đoạn sau.
- Việc kiểm chứng nhất quán tập trung vào các thao tác tuần tự và khả năng chạy lại rebuild.
- Giữ các route đọc hiện hữu và bổ sung API cần thiết. Thay đổi ý nghĩa raw của totalPoint cần được triển khai phối hợp với FE.
- FE được áp dụng `Math.min(node.maximum, node.totalPoint)` khi render. Cộng nguồn điểm, tổng hợp cha, chọn isMaxChild, xếp loại và tổng học kỳ đều do BE xử lý.
- Quy tắc manual của điểm học kỳ tiếp tục dùng số điểm do sinh viên nhập. Ghi nhận tiêu chí SV5T có nghiệp vụ riêng.
- Các file BE/FE đang có thay đổi của người dùng phải được bảo toàn. Khi thực thi, kiểm tra lại source tại thời điểm bắt đầu.
- Tránh thay đổi phụ thuộc, công cụ build và các tính năng ngoài luồng điểm học kỳ.

## Review Focus

- Sửa một ghi nhận tay phải giữ nguyên ID và thay đúng khoản điểm đã chọn, kể cả khi danh mục có nhiều ghi nhận tay cũ.
- Điểm lá vượt trần rồi xóa hoặc sửa một transaction vẫn phải cho kết quả đúng từ giao dịch còn lại.
- Thay trần, isMaxChild, parent hoặc bộ danh mục hiệu lực phải cập nhật cả tổng cha và snapshot đã lưu.
- Các transaction cũ ngoài bộ danh mục hiệu lực hoặc gắn trực tiếp vào cha cần có báo cáo đối soát rõ ràng.
- Màn hình chính và sidebar phải cùng dùng kết quả BE, kể cả trường hợp ngang điểm ở isMaxChild.

## Hiện trạng và vị trí thực hiện

BE tại `/home/shrimp/code/EventManagement/ddd`. FE tại `/home/shrimp/code/EventManagement/event_management_frontend`.

Các lỗi đã kiểm chứng trong review gồm FE thay tổng điểm tay bằng giá trị mới nhập trong khi POST của BE cộng thêm transaction, tổng cha bị cũ khi admin đổi trần lá, danh mục ngoài cây vẫn được cộng điểm, khoản thưởng trực tiếp vào cha bị ghi đè khi lá thay đổi và cách chọn nhánh isMaxChild khác nhau giữa BE/FE.

Các test command/query hiện tại có 10 test pass. Có thêm 5 kiểm chứng trong thư mục scratch của lượt review và 29 test FE hiện có pass. Khi triển khai, đưa các regression liên quan vào bộ test chính thức.

## Thiết kế nghiệp vụ

### Dữ liệu nguồn và điều kiện hợp lệ

- Dùng toàn bộ transaction còn tồn tại trong DB, đúng sinh viên, học kỳ và danh mục để tính điểm. Theo xác nhận bổ sung của người dùng, phép tính bỏ điều kiện status và DELETE xóa vật lý transaction.
- Status của transaction không được dùng làm điều kiện tính điểm. Bảng point_transactions hiện không có cột is_active.
- Tạo hoặc sửa ghi nhận điểm chỉ hợp lệ khi danh mục là lá đang hoạt động trong đúng bộ danh mục hiệu lực của học kỳ.
- Tính bộ danh mục hiệu lực bằng logic `findActiveEffectiveAsOf` hiện có. Ngày xét là ngày kết thúc học kỳ, fallback ngày bắt đầu theo logic đang dùng.
- Giao dịch lịch sử giữ nguyên ID, categoryId, học kỳ và nguồn gốc. Rebuild bảo toàn từng transaction MANUAL hợp lệ đang có, kể cả các khoản có giá trị giống nhau.
- Với giao dịch cũ gắn vào cha hoặc nằm ngoài bộ danh mục hiệu lực, tạo báo cáo để xử lý dữ liệu theo quyết định nghiệp vụ. Các dòng đó được đánh dấu trong báo cáo và loại khỏi cây tính điểm hợp lệ.

### Ý nghĩa các trường điểm

Ở lá, `systemPointRecord` là tổng điểm hệ thống nguồn CHECKOUT_REWARD, `manualPointRecord` là tổng điểm nguồn MANUAL. Với dữ liệu lịch sử thuộc các enum PENALTY_DEDUCTION hoặc REVOCATION, giữ cách phân nguồn hiện tại của command là nhóm ngoài CHECKOUT_REWARD vào manual, cộng giá trị có dấu rồi chặn mỗi nguồn tối thiểu bằng 0. Việc thay cách phân nguồn cho các enum này là một quyết định nghiệp vụ riêng khi phát triển luồng trừ điểm.

`totalPoint` của lá là tổng hai nguồn trước khi áp trần. Với dữ liệu thường gặp, hai nguồn đều có giá trị không âm.

Điểm hiệu lực của mọi node được tính bằng `effective(node) = min(node.maximum, node.totalPoint)`. `maximum` và `totalPoint` của response phải là số không âm, hữu hạn. Định nghĩa này xử lý maximum bằng 0 thành điểm hiệu lực 0.

Ở cha cộng dồn, `totalPoint = SUM(effective(child))` của các con trực tiếp. Ở cha isMaxChild, `totalPoint = MAX(effective(child))`. Cha tiếp tục áp trần riêng khi được tính vào cấp trên. Cha không có con đóng góp điểm sẽ có tổng bằng 0.

`totalPoint` ở cha được lưu trước trần của chính cha. Ví dụ các con đóng góp 15, cha có maximum 10 thì lưu totalPoint 15 và FE hiển thị 10. Cấp ông nhận đóng góp 10 từ cha đó.

`systemPointRecord` và `manualPointRecord` ở cha là các tổng nguồn thô trong những nhánh đóng góp. Cha cộng dồn cộng nguồn của tất cả con, cha isMaxChild lấy nguồn của con được chọn. Hai trường này là thông tin nguồn ghi nhận. Điểm hiệu lực luôn dựa trên totalPoint, vì trần ở các cấp có thể làm tổng nguồn lớn hơn điểm được tính.

### Chọn nhánh isMaxChild

BE chọn con theo thứ tự ưu tiên cố định, điểm hiệu lực cao hơn, rồi manualPointRecord thô cao hơn, rồi categoryId nhỏ hơn. Cách này giữ quy tắc ưu tiên đang có ở BE và giúp kết quả độc lập với thứ tự render. Response trả `selectedChildId` cho cha isMaxChild và `contributesToParent` cho con để FE dựng trạng thái nhánh được tính điểm.

### Thêm sửa xóa điểm tay

Quyết định thiết kế đề xuất là giữ POST để thêm một transaction mới. Bổ sung PATCH theo ID để sửa `points` và `description` của đúng transaction MANUAL thuộc sinh viên đăng nhập. FE mở form sửa với điểm của transaction được chọn, lấy từ lịch sử, thay cho tổng điểm manual của cả danh mục.

Ví dụ danh mục đang có một transaction 8 điểm. Sửa transaction đó thành 5 làm tổng còn 5. Nếu sinh viên chọn thêm một ghi nhận mới 5 điểm thì tổng là 13. Có nhiều giao dịch tay thì sửa/xóa theo từng ID, các giao dịch còn lại tiếp tục đóng góp.

Points của transaction tay tiếp tục là số nguyên dương theo validation hiện tại. Xóa dùng DELETE. Khi thêm/sửa/xóa hoàn tất, BE dựng lại snapshot từ các transaction còn hiệu lực, thay cho cộng/trừ delta vào snapshot cũ.

### Admin thay đổi cấu hình

Cấu hình danh mục và điểm học kỳ được cập nhật trong cùng luồng nghiệp vụ. Sau khi sửa trần, isMaxChild, parent, trạng thái hoạt động hoặc ngày áp dụng, BE xác định các cặp user/semester đã có transaction hoặc snapshot chịu ảnh hưởng rồi rebuild theo cấu hình mới trước khi báo thành công.

Xác định ảnh hưởng bằng cây hiệu lực trước và sau thay đổi trên từng học kỳ có dữ liệu, xét ID, quan hệ cha/con, maximum và isMaxChild. Gồm cả học kỳ bị tác động khi lựa chọn bộ danh mục thay đổi. Cấu hình học kỳ thay ngày bắt đầu/kết thúc cũng đi qua bước kiểm tra ảnh hưởng này. Với cặp chưa có dữ liệu, API dựng cây 0 điểm từ cấu hình hiện hành.

Kiểm tra chu trình trước khi lưu cây. Sửa maximum của danh mục gốc phải đi được qua API với parentCategoryId null. Làm rõ mapping PUT để null ở root được xử lý đúng. Các snapshot thuộc node đã ra khỏi cây hiệu lực được đưa về 0 hoặc loại khỏi tập snapshot hiện hành bằng cùng một chiến lược đã thống nhất trong service.

### API và trách nhiệm hiển thị

Giữ GET `/student-semester-points/grouped-tree/{semesterId}` và GET `/student-semester-points/me/semesters/{semesterId}`. Hai endpoint dùng chung nghĩa của totalPoint và cùng một phiên bản dữ liệu tổng hợp. API cây trả đầy đủ node, kể cả 0 điểm. FE chỉ áp trần hiển thị từng node.

Bổ sung GET `/student-semester-points/me/semesters/{semesterId}/summary` trả `semesterId`, `totalPoint`, `maximum`, `classificationLabel`, `nextClassificationLabel`, `pointsNeeded`, `activitiesCount`.

Summary totalPoint là tổng điểm hiệu lực các mục gốc trước trần học kỳ. Maximum học kỳ tiếp tục là 100 theo UI hiện tại. BE dùng điểm hiệu lực học kỳ để xếp loại và tính pointsNeeded. Port hành vi thực tế của hàm classifyScore đang dùng ở trang chính. Các khoảng nhãn là dưới 35 Kém, 35–49 Yếu, 50–79 Trung Bình, 80–89 Tốt, từ 90 Xuất sắc. nextClassificationLabel lấy nhãn kế tiếp tại các mốc 35, 50, 80, 90 và pointsNeeded là khoảng cách đến mốc đó. Từ 90 trở lên, nextClassificationLabel là null và pointsNeeded bằng 0. Đây là lựa chọn giữ hành vi màn hình hiện tại, cần người dùng duyệt cùng plan vì comment và sidebar đang có quy tắc khác.

ActivitiesCount do BE đếm các đăng ký trạng thái COMPLETED, CONFIRMED hoặc RECORDED trong đúng học kỳ đang xem. Đây là ánh xạ hoàn thành hiện có của FE, được giới hạn thêm theo học kỳ để thống nhất bộ lọc.

Node bổ sung `isAtMaximum`, `completionPercentage`, `selectedChildId`, `contributesToParent`. FE dùng các trường này cho badge, thanh tiến độ và cách thể hiện nhánh thắng. Tên màu/CSS được ánh xạ theo nhãn BE ở tầng trình bày.

Race condition được tách sang giai đoạn sau. Rebuild dựa trên dữ liệu nguồn sửa được lệch tích lũy của các thao tác tuần tự, nhưng độ an toàn giữa những request đồng thời vẫn cần được đánh giá riêng.

## Task 1 — Bộ tổng hợp điểm dùng chung ở BE

**Files tạo**

- `ddd-application/src/main/java/com/ddd/application/service/semesterpoint/impl/StudentSemesterPointCalculator.java`
- `ddd-application/src/main/java/com/ddd/application/dto/semesterpoint/StudentSemesterPointCalculation.java`
- `ddd-application/src/test/java/com/ddd/application/service/semesterpoint/impl/StudentSemesterPointCalculatorTest.java`

**Interfaces** — `calculate(Long userId, Long semesterId, List<PointCategory> categories, List<PointTransaction> transactions)` trả `StudentSemesterPointCalculation`. Record này gồm `List<StudentSemesterPoint> points` và `Map<Long, NodeMetadata> nodeMetadata`. NodeMetadata là nested record có `boolean isAtMaximum`, `int completionPercentage`, `Long selectedChildId`, `boolean contributesToParent`. Map key là categoryId. Root có contributesToParent true. Tỷ lệ làm tròn xuống từ effective/maximum, maximum 0 trả tỷ lệ 0 và isAtMaximum false. Node metadata được tính trong cùng bộ quy tắc, tránh sao chép thuật toán isMaxChild ở query.

- [x] Viết test lá có system 8 + manual 7, maximum 10, lưu totalPoint 15 và effective 10. Thêm test maximum 0 trả effective 0.
- [x] Viết test cha nhận 15, maximum 10, lưu totalPoint 15 và cấp ông nhận 10. Test isMaxChild ngang điểm chọn theo manual thô rồi ID.
- [x] Viết test mọi status của transaction còn trong DB đều được tính, dữ liệu ngoài sinh viên/học kỳ bị loại, signed legacy adjustment ổn định theo tổng nguồn, node ngoài cây và transaction ở cha được log để đối soát.
- [x] Chạy test và xác nhận fail trước khi có calculator. Cài đặt calculator theo Thiết kế nghiệp vụ. Phát hiện vòng lặp và báo lỗi rõ ràng.
- [x] Chạy `mvn -pl ddd-application -am -Dtest=StudentSemesterPointCalculatorTest -Dsurefire.failIfNoSpecifiedTests=false test`, yêu cầu pass. Commit riêng phần calculator.

## Task 2 — Rebuild và lưu snapshot từ transaction

**Files** — sửa `ddd-application/src/main/java/com/ddd/application/service/semesterpoint/impl/StudentSemesterPointCommandServiceImpl.java`, interface command tương ứng, `ddd-domain/src/main/java/com/ddd/domain/repository/PointTransactionRepository.java`, repository implementation và `ddd-infrastructure/src/main/java/com/ddd/infrastructure/repository/jpaRepository/PointTransactionJpaRepository.java`. Sửa test command hiện tại.

**Interfaces** — thêm `rebuildSemesterPoints(Long userId, Long semesterId)` vào command service. Thêm batch-read transaction cho user/semester, fetch đủ category/source và điều kiện hiệu lực. Rebuild gọi calculator của task 1, lấy result.points(), giữ ID/audit của snapshot tồn tại rồi lưu giá trị mới.

- [x] Viết test rebuild hai lần giữ nguyên số điểm và số snapshot. Test snapshot cũ bị sai được sửa theo giao dịch.
- [x] Test lá raw 15, trần 10, xóa transaction 8 thì snapshot còn 7 và cha cập nhật đúng. Test ba cấp cha và isMaxChild đổi nhánh sau khi xóa.
- [x] Cài đặt rebuild. Query transaction phải thấy các thay đổi đã ghi trong cùng transaction. Dùng fetch/batch có giới hạn theo user/semester.
- [x] Chuyển các caller nội bộ của recordPointAndPropagate/bulkRecordPointsAndPropagate sang luồng transaction rồi rebuild. Tìm toàn bộ caller trước khi bỏ thuật toán delta. Các interface cũ còn caller phải được chuyển đổi trong cùng task.
- [x] Chạy `StudentSemesterPointCalculatorTest` và `StudentSemesterPointCommandServiceImplTest`, yêu cầu pass. Commit task.

## Task 3 — Mutation điểm tay và chốt sự kiện

**Files BE** — `ddd-api/src/main/java/com/ddd/api/controller/PointTransactionController.java`, tạo DTO `ddd-api/src/main/java/com/ddd/api/dto/semesterpoint/req/UpdatePointTransactionRequestDto.java`, command interface/implementation dưới service semesterpoint, `RegistrationCommandServiceImpl.java`, `EventCommandServiceImpl.java` và các test tương ứng.

**Interfaces** — bổ sung PATCH `/point-transactions/me/{pointTransactionId}` nhận `points`, `description`. Giữ POST thêm transaction và DELETE theo ID. Mutation ghi transaction xong gọi rebuild của task 2 trong cùng transaction nghiệp vụ.

- [x] Test POST 8 rồi POST 5 tạo hai ghi nhận, tổng raw 13. PATCH ghi nhận 8 thành 5 giữ nguyên ID, tổng raw thành 10 nếu ghi nhận 5 kia còn tồn tại. DELETE một ghi nhận chỉ bỏ đúng ID đó.
- [x] Test PATCH/DELETE transaction của người khác hoặc nguồn hệ thống bị từ chối. Test lỗi rebuild rollback cả mutation.
- [x] Test ghi nhận danh mục tồn tại nhưng ngoài bộ hiệu lực bị từ chối trước khi lưu transaction. Thêm kiểm tra lá/hiệu lực khi tạo và sửa event points, xác nhận lại lúc chốt event để bao phủ cấu hình thay đổi sau khi tạo sự kiện.
- [x] Chốt event lưu transaction CHECKOUT_REWARD xong rebuild cho các sinh viên được thưởng. Giữ cơ chế chốt sự kiện đang có để xử lý gọi lại cùng sự kiện.
- [x] Test sự kiện cấu hình vào cha bị từ chối và không còn trường hợp cha nhận 5 rồi bị lá 2 ghi đè.
- [x] Chạy test transaction command, registration và controller liên quan. Ghi rõ baseline failures nếu các test đăng ký ngoài phạm vi đang fail. Commit task.

## Task 4 — Admin đổi cấu hình cập nhật snapshot đã lưu

**Files** — `PointCategoryServiceImpl.java`, `SemesterServiceImpl.java` dưới application service, `UpdatePointCategoryRequestDto.java`, mapper update liên quan và repository tìm các cặp user/semester có dữ liệu. Test các service này.

**Interfaces** — các mutation cấu hình tìm tập cặp bị ảnh hưởng theo cây hiệu lực trước/sau, gọi rebuild task 2 cho từng cặp. Rebuild không gọi ngược service cấu hình để tạo vòng phụ thuộc bean.

- [ ] Test giảm trần lá 10 xuống 5 cập nhật leaf raw, parent và grandparent đã lưu ngay khi admin mutation thành công.
- [ ] Test tăng trần, đổi SUM sang MAX, chuyển lá từ A sang B, tắt category và đổi dateApply làm thay phiên bản hiệu lực. Cả nhánh cũ và mới được xét lại.
- [ ] Test sửa root với parent null và chặn A nằm dưới hậu duệ A. Cập nhật DTO/mapping phù hợp cho mutation hiện hữu.
- [ ] Test đổi ngày học kỳ làm tập danh mục hiệu lực thay đổi. Cặp user/semester có transaction nhưng chưa có snapshot cũng được rebuild.
- [ ] Thực hiện theo transaction hiện hữu. Đo thời gian trên tập dữ liệu mẫu, ghi lại giới hạn xử lý đồng bộ trước triển khai môi trường thật.
- [ ] Chạy `PointCategoryServiceImplTest`, `SemesterServiceImplTest` và calculator/command test. Commit task.

## Task 5 — Contract đọc thống nhất và summary do BE trả về

**Files** — `StudentSemesterPointQueryServiceImpl.java`, query interface, `StudentSemesterPointController.java`, application/API DTO và mapper semesterpoint hiện hữu. Tạo `StudentSemesterPointSummaryDto.java`, `StudentSemesterPointSummaryResponseDto.java` cùng các thư mục DTO tương ứng. Test query/controller.

**Interfaces** — giữ hai API đọc hiện hữu và thêm API summary theo Thiết kế nghiệp vụ. Query batch-read giao dịch hợp lệ và cây hiệu lực, dùng calculator task 1 trả raw totalPoint, raw nguồn ghi nhận và nodeMetadata từ cùng kết quả. Ghép ID/finalizedAt/audit của snapshot đã lưu theo categoryId khi có. Endpoint danh sách và summary dùng cùng kết quả tính này. GET chỉ đọc dữ liệu. Trong luồng tuần tự sau một mutation thành công, kết quả tính phải khớp snapshot task 2 đã lưu.

- [ ] Viết serialization test xác nhận không cắt mất raw leaf total 15 khi maximum 10. API danh sách và API cây trả cùng raw totalPoint cho cùng node.
- [ ] Test sau admin mutation, leaf/cha/summary trả khớp snapshot mới. Test cây trống, sinh viên mới có đầy đủ node 0 và không có chia cho 0.
- [ ] Test boundary xếp loại 34/35, 49/50, 64/65, 79/80, 89/90, pointsNeeded và summary vượt 100 vẫn có effective 100.
- [ ] Test activitiesCount chỉ thuộc học kỳ chọn, dùng ba trạng thái đã quy định. Sidebar và trang chính nhận cùng summary API.
- [ ] Chạy query/controller tests qua `mvn -pl ddd-api -am -Dtest=StudentSemesterPointQueryServiceImplTest,StudentSemesterPointControllerTest -Dsurefire.failIfNoSpecifiedTests=false test`. Commit task.

## Task 6 — FE hiển thị dữ liệu BE và sửa đúng transaction

**Files FE** — `src/features/training-points/services/trainingPointsService.ts`, `types/trainingPoints.types.ts`, `utils/scoreCalculator.ts`, `src/pages/TrainingPointsPage.tsx`, các component `TrainingPointsSectionAccordion.tsx`, `TrainingPointsRow.tsx`, `TrainingPointsSummary.tsx`, `CriterionDetailModal.tsx`, `SelfEvaluationModal.tsx`, cùng `src/features/events/components/StudentRightSidebar.tsx`.

**Interfaces** — service thêm getSemesterPointSummary và updatePointTransaction. Page/sidebar dùng tree và summary, query key gồm user + semester. Hàm render điểm node chỉ là `Math.min(maximum, totalPoint)` với input đã được BE kiểm tra hợp lệ.

- [ ] Test leaf manual từ response là 13 luôn hiển thị theo response sau mutation/refetch. Gỡ studentScores/studentDescriptions làm nguồn dữ liệu đã lưu và gỡ override sau POST.
- [ ] Gỡ computeSectionCategoryScores khỏi đường render, phép SUM/MAX ở accordion, chọn nhánh thắng và phần xếp loại/tính pointsNeeded ở FE. Lưu helper áp trần node nếu còn dùng. Rà soát caller trước khi xóa helper cũ.
- [ ] Bảng và modal đọc totalPoint, systemPointRecord, manualPointRecord, isAtMaximum và metadata BE. Giao dịch chỉ phục vụ hiển thị lịch sử hoặc chọn ID để thao tác.
- [ ] Form thêm gọi POST. Form sửa mở với `transaction.points`, gọi PATCH đúng ID. Form xóa gọi DELETE đúng ID. Trường hợp nhiều manual hiển thị/chọn riêng từng ghi nhận, tránh chọn ngẫu nhiên một transaction để đại diện tổng.
- [ ] Mutation chờ invalidate/refetch tree, summary và history có liên quan sau thành công. Nếu mutation lỗi thì giữ draft và thông báo lỗi, tránh hiển thị điểm như đã lưu thành công.
- [ ] Trang chính/sidebar dùng summary BE. Với mọi node, chỉ áp trần của chính node để hiển thị. Metadata badge/progress lấy từ BE. Trường hợp tải lỗi hoặc không có cấu hình hiển thị error/empty state theo response, tránh fallback sang bộ danh mục của học kỳ khác.
- [ ] Thêm test sửa 8 thành 5 hiển thị 5 ngay sau refetch và vẫn 5 sau reload, thêm mới 5 vào 8 hiển thị 13 hoặc mức trần tương ứng, xóa đúng một trong nhiều manual, nhánh isMaxChild ngang điểm, đổi học kỳ trong lúc request chạy và summary/sidebar đồng nhất.
- [ ] Chạy `npm run test -- src/features/training-points`, các test page/sidebar liên quan, `npm run typecheck` và `npm run build`. Commit task.

## Task 7 — Đối soát dữ liệu cũ và triển khai

**Files** — tạo `ddd-infrastructure/src/main/resources/database/migration/student-semester-points-reconcile.sql` hoặc runner nội bộ dùng calculator của task 1. Ưu tiên tái sử dụng calculator để chỉ duy trì một bộ quy tắc. Ghi hướng dẫn chạy trong tài liệu triển khai của chính thay đổi này.

- [ ] Dry-run trên các cặp user/semester có transaction hoặc snapshot, báo số dòng đổi trước/sau và các giao dịch không hợp lệ. Báo riêng trường hợp snapshot có điểm nhưng thiếu transaction nguồn để quyết định dữ liệu trước khi repair.
- [ ] Dữ liệu manual cũ được giữ theo từng ID. Các khoản trùng do hành vi lưu trước đây cần được người sở hữu dữ liệu xem xét qua báo cáo, tránh tự động gộp theo giá trị hoặc description.
- [ ] Repair chỉ dựng lại snapshot từ dữ liệu nguồn đã chốt. Test chạy lại hai lần giữ nguyên kết quả và không tạo thêm transaction. Test rollback khi một cặp bị lỗi.
- [ ] Kiểm tra các consumer khác đọc student_semester_points hoặc API totalPoint để cập nhật theo raw/effective mới, gồm recommendation/statistic nếu có caller. Dùng CodeGraph kiểm tra blast radius trước khi đổi semantics public.
- [ ] Triển khai phối hợp BE và FE vì totalPoint của node có thể vượt maximum trong contract mới. Chạy repair trên môi trường đích sau khi xem báo cáo và duyệt phạm vi dữ liệu.
- [ ] Smoke test thêm/sửa/xóa manual, chốt sự kiện, admin đổi trần và cây, reload, đổi học kỳ. Đối chiếu transaction, snapshot DB, response và điểm hiển thị.
- [ ] Ghi rõ các rủi ro đồng thời còn lại trong kết quả bàn giao để xử lý ở giai đoạn race condition.

## Điều kiện hoàn thành

Các thao tác tuần tự thêm/sửa/xóa, chốt sự kiện và cập nhật cấu hình cho snapshot phù hợp transaction cùng bộ danh mục hiệu lực. Raw totalPoint được giữ đúng ở lá và cha, cấp trên nhận effective của con. BE cung cấp cùng một kết quả cho bảng, modal và sidebar. FE hiển thị từng node qua min(maximum,totalPoint), dùng summary và metadata từ BE. Dữ liệu lịch sử có báo cáo đối soát và repair chạy lại an toàn. Kết quả kiểm chứng ghi rõ phạm vi thao tác tuần tự của đợt refactor này.

## Các quyết định cần người dùng duyệt cùng kế hoạch

- POST thêm một khoản điểm, PATCH sửa khoản theo ID, thay cho việc dùng POST để sửa tổng manual của danh mục.
- systemPointRecord/manualPointRecord giữ ý nghĩa nguồn thô, điểm được tính dựa trên totalPoint đã tổng hợp và trần node.
- Admin đổi cấu hình sẽ rebuild snapshot bị ảnh hưởng trước khi báo thành công.
- Xếp loại BE ban đầu giữ hành vi thực tế của hàm classifyScore ở trang chính, gồm nhãn Trung Bình cho 65–79. Các thay đổi quy chế xếp loại có thể được quyết định riêng.

## Kết quả thực hiện task 1 đến 3 ngày 27/11/2026

Đã phục hồi code từ patch dự phòng, chạy lại 33 test liên quan pass và áp code vào repo. Đã có lượt review độc lập cuối không phát hiện lỗi quan trọng trong phạm vi này. Task 1–3 được đưa vào cùng commit để cập nhật đồng bộ interface và toàn bộ caller.

Điều chỉnh theo chỉ đạo mới của người dùng là tính mọi transaction còn tồn tại bất kể status, DELETE xóa vật lý rồi rebuild. Bảng point_transactions không có is_active. Giao dịch lịch sử gắn vào cha hoặc ngoài bộ danh mục hiệu lực được log và loại khỏi phép tính, báo cáo repair có cấu trúc thuộc task 7.

Bộ registration cũ có baseline 9 failure và 4 error đã được kiểm chứng trước refactor. Các test riêng cho điểm và chốt phần thưởng đều pass. Test rollback dùng Spring transaction manager với mock JDBC connection. Chưa có integration toàn luồng với DB thật cho task 1–3.

Task 4–7 gồm cập nhật cấu hình admin, contract đọc/summary, FE và đối soát dữ liệu vẫn chờ triển khai. Refactor race condition dành cho giai đoạn sau.

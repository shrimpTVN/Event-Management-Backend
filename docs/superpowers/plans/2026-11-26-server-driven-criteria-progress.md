# Refactor hiển thị tiến độ SV5T từ Backend

> **For agentic workers:** REQUIRED SUB-SKILL — dùng executing-plans để triển khai từng task sau khi người dùng duyệt kế hoạch. Các bước dùng checkbox để theo dõi.

**Goal** — FE hiển thị trực tiếp kết quả tiến độ do BE trả về, bao gồm số lần ghi nhận, trạng thái từng tiêu chí, nguồn ghi nhận, tự đánh giá và tổng tiêu chuẩn đạt.

**Architecture** — Giữ endpoint hiện tại và mở rộng response theo cách bổ sung trường. BE tổng hợp một cây hiển thị hoàn chỉnh theo sinh viên, năm học và scope. FE dùng response làm dữ liệu render, chỉ giữ trạng thái tương tác như modal, loading và bản nháp chưa lưu.

**Tech Stack** — Spring Boot, Java, JPA, MapStruct, React, TypeScript, TanStack Query, Vitest.

**Spec** — Yêu cầu trong chat ngày 26-11-2026 và các quyết định thiết kế trong tài liệu này. Đây là kế hoạch đề xuất để duyệt trước khi triển khai.

## Phạm vi và hiện trạng đã kiểm tra

Đường dẫn BE hiện có trên máy là `/home/shrimp/code/EventManagement/ddd`. FE là `/home/shrimp/code/EventManagement/event_management_frontend`. Thư mục workspace cũ `/home/shrimp/code/ddd` hiện không tồn tại. Tài liệu được lưu ở `/tmp/ddd-refactor-plan` để người dùng mở được ngay, khi triển khai đưa vào `docs/superpowers/plans/` của checkout BE thực tế.

BE đang có thay đổi chưa commit tại controller, criteria service và progress query service cùng các test liên quan. Khi triển khai phải giữ các thay đổi đó và đối chiếu lại source hiện tại.

`StudentCriteriaProgressController` có hai GET endpoint

- `/criteria-progress/me/academic-years/{academicYearId}?scope=UNIVERSITY` trả cây và tổng tiến độ. Scope mặc định là UNIVERSITY.
- `/criteria-progress/me/criterias/{criteriaId}/academic-years/{academicYearId}` trả một node tiến độ.

`StudentCriteriaProgressResponseDto` hiện có `academicYearId`, `academicYear`, `completedMainCriteria`, `totalMainCriteria`, `criterias`.

`CriteriaProgressItemResponseDto` hiện có `id`, `name`, `note`, `scope`, `level`, `childrenOrder`, `requiredChildren`, `completedChildrenCount`, `isCompleted`, `children`.

Tầng application đã có `requiredOccurrences` và `completedOccurrences`, nhưng API response chưa khai báo hai trường này. Query hiện lấy max giữa bộ đếm con lưu DB và số con thực sự đạt, đồng thời OR cờ hoàn thành lưu DB với kết quả rollup. Cách đó có thể giữ trạng thái đạt cũ khi dữ liệu giảm.

FE `ensureSv5tHierarchy` hiện lấy max giữa số lần BE và lịch sử, tính lại trạng thái lá và cha, trộn localStorage, rồi đếm lại tổng ở `Sv5tPage`. FE còn dựng cây hardcode khi response rỗng. `Sv5tRow` và `Sv5tSummary` có phép tính trạng thái và tỷ lệ riêng cần gỡ cùng lúc.

## Các quyết định thiết kế đề xuất

1. Dùng cùng một bộ tổng hợp phía BE cho endpoint cây và endpoint một tiêu chí. Giao dịch tiêu chí là nguồn để tổng hợp trạng thái lá. Các dòng progress tổng hợp được kiểm tra và sửa để khớp giao dịch, tránh lấy max với dữ liệu cũ nhằm che sai lệch.
2. Giữ tên public hiện tại `criterias`, `id`, `completedOccurrences`, `completedChildrenCount`. FE dùng các tên này trực tiếp. Adapter tương thích chỉ đổi cấu trúc hoặc tên trong giai đoạn chuyển tiếp, không suy luận nghiệp vụ.
3. Trả cây đúng scope từ BE. FE đưa scope vào request và query key, bỏ việc lọc scope sau khi đã lấy dữ liệu.
4. Giữ năm học có cây đầy đủ với giá trị 0 khi sinh viên chưa có giao dịch. Nếu cấu hình thực sự không có tiêu chí, BE trả cây rỗng và tổng 0. FE hiện empty state. Lỗi API hiện error state.
5. Giữ tự đánh giá tách biệt với hệ thống. Giả định cần người dùng duyệt là manual được đánh dấu tự đánh giá, còn số lần hệ thống chỉ đếm sự kiện riêng biệt và giao dịch SYSTEM hợp lệ. Với lá cần hơn một lần, manual không thay thế số lần hệ thống còn thiếu. Với lá cần một lần, manual có thể làm trạng thái tổng hợp đạt như FE hiện tại. Đây là quyết định nghiệp vụ cần xác nhận trước triển khai vì BE hiện dùng chung bộ tăng/giảm occurrences cho giao dịch manual.
6. Cha đạt khi số con trực tiếp đạt đạt ngưỡng `requiredChildren`, mặc định số con thực tế nếu ngưỡng null. Tổng top-level được tính trên chính danh sách gốc của cây trả về. Không unwrap một lớp ở FE. Kiểm tra dữ liệu cấu hình để BE trả đúng các tiêu chuẩn màn hình cần hiển thị.

## Contract đích

Response ngoài giữ các trường hiện tại và bổ sung `scope`, `completionPercentage`. Phần trăm là số nguyên từ 0 đến 100, làm tròn xuống, bằng 0 khi tổng bằng 0. Scope ở đây là giá trị đã áp dụng cho response.

Node giữ toàn bộ trường public hiện tại và bổ sung

| Trường | Ý nghĩa |
|---|---|
| `requiredOccurrences` | Ngưỡng số lần, luôn >= 1 |
| `completedOccurrences` | Số lần hệ thống đã ghi nhận, không cắt ở ngưỡng |
| `totalChildren` | Số con trực tiếp trong cây trả về |
| `isLeaf` | BE xác định từ cây hiệu lực |
| `isSystemRecorded` | Có ít nhất một ghi nhận hệ thống |
| `isSystemCompleted` | Ghi nhận hệ thống đủ ngưỡng ở lá, rollup hệ thống ở cha |
| `isSelfEvaluated` | Có giao dịch manual còn hiệu lực của chính tiêu chí |
| `selfEvaluationNote` | Ghi chú manual mới nhất, null nếu không có |
| `manualTransactionId` | ID manual mới nhất để thao tác xóa hiện tại, null nếu không có |
| `completionSource` | MANUAL, EVENT_CHECKOUT, SYSTEM hoặc null, chỉ chọn nguồn hoàn thành theo quy tắc bên dưới |
| `systemEventName` | Tên sự kiện ghi nhận mới nhất, null khi không có |

`isCompleted` là kết quả tổng hợp cuối cùng. Cha tính từ `isCompleted` của con, còn `isSystemCompleted` của cha tính từ `isSystemCompleted` của con. `completionSource` ở lá ưu tiên nguồn hệ thống đã đủ ngưỡng, nếu chưa đủ mà manual giúp hoàn thành thì là MANUAL. Cha để null vì có thể đạt từ nhiều nguồn. Các badge hệ thống dùng boolean hệ thống, không suy luận từ `completionSource`.

Chọn ghi nhận mới nhất theo `createdAt DESC, id DESC`. Dùng đúng enum hiện có, kiểm tra giá trị SYSTEM thực sự được hỗ trợ trước khi khai báo public. Lịch sử chi tiết vẫn lấy riêng khi mở lịch sử, không được dùng để tính lại cây. Khi có nhiều manual cũ, xóa một giao dịch thì refetch lại, còn giao dịch nào thì BE tiếp tục trả trạng thái tương ứng.

## Global Constraints

- Duy trì route và các trường public hiện tại, bổ sung trường trước khi chuyển FE.
- Mọi truy vấn tổng hợp giới hạn theo user xác thực, academic year và cây đúng scope.
- Không ghi product code trong giai đoạn viết plan.
- Không dùng localStorage làm nguồn trạng thái đã lưu. Bản nháp modal chỉ tồn tại trong tương tác đang mở.
- Không tự sửa dữ liệu production trong request GET. Việc đối soát và repair phải là bước riêng được kiểm tra trước.
- GET hiển thị phải đọc giao dịch trong một snapshot nhất quán. Dùng read-only REPEATABLE_READ khi có nhiều query phụ thuộc để tránh checkout commit ở giữa các query.

## Review Focus

- Thêm rồi xóa manual khi vẫn còn ghi nhận hệ thống phải giữ đúng nguồn và trạng thái.
- Giao dịch cũ trùng event không được tăng số lần hai lần.
- Đổi năm học hoặc scope trong lúc request đang chạy phải hiển thị đúng bộ lọc được chọn.
- Response rỗng hoặc lỗi không được thay bằng cây hardcode có trạng thái giả.
- Sửa ngưỡng hoặc cây tiêu chí phải cho kết quả query nhất quán với chính cấu hình hiện tại, kể cả bộ đếm DB cũ đang lớn hơn.

## Task 1 — Đưa đủ trường tiến độ qua API và khóa contract

**Files BE** — `ddd-api/src/main/java/com/ddd/api/dto/criteria/res/CriteriaProgressItemResponseDto.java`, `StudentCriteriaProgressResponseDto.java` cùng thư mục, `ddd-application/src/main/java/com/ddd/application/dto/criteria/CriteriaProgressItemDto.java`, `StudentCriteriaProgressDto.java` cùng thư mục, `ddd-api/src/main/java/com/ddd/api/mapper/StudentCriteriaProgressApiMapper.java`.

**Interfaces** — GET cây và GET node trả cùng kiểu node của contract đích, các trường cũ giữ nguyên tên.

- [ ] Thêm serialization/controller test tại `ddd-api/src/test/java/com/ddd/api/controller/StudentCriteriaProgressControllerTest.java`, kiểm tra `criterias[0].id`, `requiredOccurrences = 3`, `completedOccurrences = 2`, `isCompleted = false` và cây con được map đầy đủ. Mock principal STUDENT.
- [ ] Mở rộng DTO và mapper với các trường contract đích. Cập nhật mọi nơi khởi tạo record và fixture liên quan để module biên dịch.
- [ ] Test response trường hợp không có giao dịch, null note, null manual ID, children rỗng, scope mặc định UNIVERSITY và scope khác được enum hiện tại hỗ trợ.
- [ ] Chạy `mvn -pl ddd-api -am -Dtest=StudentCriteriaProgressControllerTest -Dsurefire.failIfNoSpecifiedTests=false test`. Yêu cầu test pass và response JSON có đủ trường.
- [ ] Commit phần contract sau khi test pass.

## Task 2 — BE tạo kết quả hiển thị hoàn chỉnh

**Files BE** — `ddd-application/src/main/java/com/ddd/application/service/criteriaprogress/impl/StudentCriteriaProgressQueryServiceImpl.java`; DTO ở task 1; repository giao dịch JPA hiện có dưới `ddd-infrastructure/src/main/java/com/ddd/infrastructure/repository/jpaRepository/`; test `ddd-application/src/test/java/com/ddd/application/service/criteriaprogress/impl/StudentCriteriaProgressQueryServiceImplTest.java`.

**Interfaces** — Giữ `getProgress(String email, Long academicYearId, CriteriaScopeEnum scope)` và `getCriteriaProgress(String email, Long criteriaId, Long academicYearId)`. Endpoint đơn tìm node từ cùng kết quả tổng hợp, với cây bao gồm scope chứa tiêu chí đó.

- [ ] Thêm test lá ngưỡng 3 với 0, 2, 3, 4 sự kiện, mong đợi lần lượt false, false, true, true và số lần 4 vẫn trả 4.
- [ ] Thêm test hai giao dịch cùng event chỉ đếm một lần, manual không tăng system occurrences, hỗn hợp manual/system, nhiều manual cũ và lựa chọn giao dịch mới nhất ổn định.
- [ ] Batch load giao dịch cho user và năm học, group theo criteria ID. Tránh truy vấn từng node và lazy load event name trong vòng lặp. Dùng projection hoặc fetch join phù hợp với repository hiện tại.
- [ ] Tính lá theo contract đã duyệt, rồi rollup từ dưới lên. Gỡ `Math.max` với bộ đếm cha cũ và phép OR cờ cha lưu DB. Bộ đếm cha luôn bằng số con đạt thực tế của response.
- [ ] Sắp thứ tự con tại BE theo childrenOrder rồi ID. Trả đúng gốc tiêu chuẩn màn hình, kiểm tra cấu trúc scope thực tế trước khi thay đổi lớp gốc.
- [ ] Tính tổng và phần trăm ở BE, có test 3/5 trả 60, 0/0 trả 0. Test scope/năm học không trộn dữ liệu và endpoint đơn khớp node tương ứng trong cây.
- [ ] Test cấu hình giảm ngưỡng, tăng ngưỡng, đổi cha và bộ đếm cũ đang sai. Kết quả luôn theo cây và giao dịch hiện tại.
- [ ] Chạy `mvn -pl ddd-application -am -Dtest=StudentCriteriaProgressQueryServiceImplTest -Dsurefire.failIfNoSpecifiedTests=false test`, rồi chạy test contract task 1. Commit khi pass.

## Task 3 — Đồng bộ luồng ghi và dữ liệu tiến độ đã lưu

**Files BE** — `StudentCriteriaTransactionCommandServiceImpl.java`, `StudentCriteriaProgressCommandServiceImpl.java` dưới service `criteriaprogress/impl`, các repository tương ứng và các test command service hiện có.

**Interfaces** — Giữ API thêm/xóa hiện tại. Một mutation thành công phải để giao dịch và progress nhất quán. GET sau mutation phản ánh đầy đủ kết quả BE.

- [ ] Thêm regression test xóa manual khi cập nhật progress thất bại, yêu cầu rollback cả giao dịch. Gỡ catch nuốt lỗi quanh `markUncompleted` trong deleteTransaction.
- [ ] Đồng bộ quy tắc ghi progress với quy tắc nguồn dữ liệu ở task 2. Tái sử dụng logic tổng hợp hiện có khi đúng, tránh hai bộ quy tắc độc lập giữa command và query.
- [ ] Test chuỗi event → thêm manual → xóa manual, nhiều manual còn tồn tại, xóa lặp và request đồng thời. Tuần tự hóa ghi theo user/năm học hoặc dùng cơ chế bảo đảm tương đương bao phủ cả khi progress chưa tồn tại.
- [ ] Checkout cùng event retry phải có tính idempotent ở nguồn giao dịch. Kiểm tra duplicate hiện hữu trước khi quyết định thêm unique constraint. Unique event không áp dụng vào manual có event null.
- [ ] Tạo báo cáo đối soát read-only giữa giao dịch và progress. Chuẩn bị repair có dry-run, chạy lại an toàn và không cộng lại giao dịch. Chỉ áp dụng dữ liệu thật sau khi xem báo cáo và xác nhận phạm vi.
- [ ] Chạy các test `StudentCriteriaTransactionCommandServiceImplTest`, `StudentCriteriaProgressCommandServiceImplTest` và query test. Xác minh concurrency bằng integration test DB có hai transaction thực, không chỉ mock. Commit khi pass.

## Task 4 — FE render trực tiếp response BE

**Files FE** — `src/features/sv5t/types/sv5t.types.ts`, `services/sv5tService.ts`, `utils/sv5tTreeBuilder.ts`, `src/pages/Sv5tPage.tsx`, `src/features/sv5t/components/Sv5tRow.tsx`, `Sv5tSummary.tsx` cùng thư mục; các test service, persistence và component hiện có.

**Interfaces** — `getStudentCriteriaProgress(academicYearId, scope)` trả đúng contract task 1. Query key gồm user, academicYearId và scope. Tổng summary đọc `completedMainCriteria`, `totalMainCriteria`, `completionPercentage` từ response.

- [ ] Cập nhật kiểu TypeScript dùng tên public BE. Service chỉ unwrap BaseResponse và kiểm tra cấu trúc. Thiếu field bắt buộc hiển thị lỗi contract, tránh mặc định thành trạng thái hoàn thành khác.
- [ ] Gỡ `ensureSv5tHierarchy` khỏi đường render, bỏ cây fallback hardcode và phép trộn progress với lịch sử. Chỉ xóa phần helper không còn caller sau khi rà soát các caller khác.
- [ ] Gỡ localCompletedMap/localNoteMap làm nguồn dữ liệu đã lưu và effect merge localStorage. Dữ liệu trình duyệt cũ bị bỏ qua, không cần xóa toàn bộ localStorage của ứng dụng.
- [ ] Dùng `criterias` trực tiếp, đọc trạng thái/note/event name/counters từ node. Dùng `isSystemCompleted` cho badge đạt và màu, `isLeaf` cho kiểu dòng, `isSelfEvaluated` cho tự đánh giá. Gỡ cả phép so occurrences với ngưỡng trong Sv5tRow và phép chia phần trăm trong Sv5tSummary.
- [ ] Sau mutation thành công invalidate/refetch progress đúng user/năm học/scope và cache lịch sử liên quan. Chờ dữ liệu BE trước khi hiển thị trạng thái đã lưu mới. Modal chỉ giữ draft note/checkbox, lỗi mutation giữ draft và hiện lỗi.
- [ ] Test response có `isCompleted = false` vẫn render false dù localStorage hoặc lịch sử nói true. Test 2/3 occurrences không bị manual hay lịch sử override. Test tổng BE được dùng trực tiếp, đổi scope gửi đúng query param và dữ liệu scope cũ không xuất hiện dưới nhãn scope mới.
- [ ] Test thiếu cấu hình hiển thị empty state, HTTP lỗi hiển thị error state, refetch sau thêm/xóa cập nhật đúng, reload và thiết bị khác cho cùng kết quả. Thêm test cập nhật mới nhất trong lúc người dùng đổi scope/năm học.
- [ ] Chạy `npm run typecheck`, `npm run test -- src/features/sv5t src/pages/__tests__/Sv5tPersistence.test.tsx`, `npm run build`. Commit khi pass.

## Task 5 — Triển khai và nghiệm thu toàn luồng

- [ ] Triển khai BE có contract bổ sung và quy tắc đã duyệt trước, xác minh FE cũ vẫn đọc được trường cũ trong thời gian chuyển tiếp.
- [ ] Đối soát dữ liệu cũ theo task 3. Nếu có thay đổi semantics manual đã duyệt, ghi rõ tác động trước khi áp dụng repair.
- [ ] Triển khai FE sau khi kiểm tra payload môi trường đích có đủ contract mới. Refetch khi quay lại tab theo chính sách cache hiện tại để nhìn thấy checkout từ nơi khác.
- [ ] Smoke test sinh viên mới, 2/3 và 3/3 lần, checkout rồi chốt sự kiện, thêm/xóa manual, hai thiết bị, hai scope và hai năm học. So response Network với trạng thái, counter và summary trên màn hình.
- [ ] Kiểm tra source đường render không còn phép đếm/filter/max để xác định nghiệp vụ, không còn gọi history để phục vụ rollup. Format chuỗi như `2/3 lần`, mở modal và render cây vẫn thuộc trách nhiệm FE.

## Điều kiện hoàn thành

Mọi trạng thái, số lần, nguồn ghi nhận, tổng và tỷ lệ hiển thị khớp response BE. GET cây và GET node thống nhất. Thêm/xóa và checkout không để transaction khác progress khi báo thành công. FE không phụ thuộc cache localStorage hoặc lịch sử để quyết định đạt. Test contract, query, mutation, FE và concurrency đã pass, hoặc lỗi baseline được ghi rõ và tách khỏi kết quả nghiệm thu.

## Quyết định cần duyệt

Quy tắc manual tại mục thiết kế số 5 là giả định đề xuất nhằm giữ hành vi hiển thị hiện tại. Nếu nghiệp vụ muốn manual cũng đóng góp vào ngưỡng nhiều lần, cần sửa rõ quy tắc và expected tests trong task 2–3 trước triển khai. Các trường và lựa chọn phần trăm trong contract là thiết kế đề xuất, chưa phải dữ liệu API đang có.

## Tiến độ thực hiện ngày 26-11-2026

- Task 1–4 đã triển khai vào hai repo EventManagement. 16 test BE và 50 test FE liên quan pass, FE typecheck/build pass. Test BE dùng Mockito javaagent vì JVM không cho self-attach ổn định.
- Theo yêu cầu bổ sung, enum public BE là UNIVERSITY và FACULTY. Có migration dữ liệu SCHOOL sang FACULTY và cập nhật seed.
- Giữ adapter FE đổi tên trường đơn thuần để tương thích các component hiện hữu. Mọi trạng thái và số đếm đều từ BE. Bổ sung isEligible và completionPercentage từng node để gỡ cả phép tính ở summary, accordion và modal.
- Giữ cây nguyên vẹn qua getCriteriaHierarchy cho query/command. API danh sách tiến độ đưa các tiêu chuẩn dưới wrapper level 0 ra cấp đầu, endpoint đơn vẫn tìm được wrapper.
- Có kiểm chứng PostgreSQL trong schema test riêng cho khóa/retry, rollback, scope migration, dry-run và repair chạy lại. Đây là kiểm chứng cơ chế SQL, chưa phải integration test Spring qua HTTP với hai request đồng thời.
- Task 5 còn triển khai môi trường đích, migration và repair dữ liệu ứng dụng thực. Chưa thực hiện các bước đó. Script repair mặc định rollback.
- Các file đang sửa của người dùng được giữ lại. Chưa tạo commit tự động để tránh gộp thay đổi đang có vào commit refactor.

## Nghiệp vụ được người dùng chốt bổ sung ngày 26-11-2026

Phần này thay thế các giả định trước về manual và requiredChildren. Mỗi transaction MANUAL còn hoạt động đóng góp một lần ngay khi lưu, cộng cùng các lần hệ thống để so requiredOccurrences. Manual chỉ có note, không đối chiếu hoạt động trùng với hệ thống. Sinh viên được sửa note và xóa từng ghi nhận MANUAL của mình, không giới hạn thời gian. Thêm PATCH /criteria-transactions/me/{id} nhận duy nhất note để sửa giữ nguyên ID và số lần. Nút cha có requiredChildren null mặc định bằng 1. Tính lại các cấp cha sau thay đổi ghi nhận.

GET tiến độ luôn tính theo cấu hình hiện hành nên thay đổi ngưỡng admin có hiệu lực ở lần GET tiếp theo. Bảng student_criteria_progress được cập nhật khi có mutation ghi nhận hoặc chạy đối soát, update cấu hình admin hiện chưa tự rebuild toàn bộ dòng progress đã lưu. FE cần refetch để nhận kết quả mới.

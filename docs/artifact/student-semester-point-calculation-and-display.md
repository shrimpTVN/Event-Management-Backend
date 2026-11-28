# Cách tính điểm học kỳ ở BE và hiển thị ở FE

Cập nhật ngày 27/11/2026 theo nhánh `codex/semester-points-server-driven`. Tài liệu này mô tả **điểm rèn luyện theo học kỳ** (`point_transactions`, `student_semester_points`, `point_categories`), không phải tiến độ tiêu chí Sinh viên 5 tốt.

## 1. Dữ liệu nguồn và cây danh mục

Mỗi transaction điểm có `userId`, `semesterId`, `pointCategoryId`, `points`, `type`, `status` và `description`. BE tính từ **mọi transaction còn tồn tại** của đúng sinh viên và học kỳ; không lọc theo `status`. Xóa ghi nhận tay là xóa transaction khỏi DB. Giao dịch thuộc danh mục không còn trong cây hiệu lực hoặc gắn trực tiếp vào node cha được giữ trong ledger nhưng không góp điểm; công cụ đối soát báo các trường hợp đó.

Cây hiệu lực lấy danh mục đang hoạt động, đã đến `dateApply` tại ngày kết thúc học kỳ (nếu thiếu thì dùng ngày bắt đầu, sau đó là ngày hiện tại). Một node chỉ thuộc cây tính điểm nếu còn đường nối qua các cha hiệu lực lên root. Ghi nhận mới hoặc sửa ghi nhận chỉ được gắn vào **node lá hiệu lực**. Root có `parentCategoryId = null` hoặc `0`. BE chặn chu trình và trần âm; `maximum = 0` là hợp lệ.

Nguồn: `SemesterPointCategoryPolicy`, `StudentSemesterPointCalculator`.

## 2. Tính điểm cho từng node

| Trường | Ý nghĩa |
| --- | --- |
| `systemPointRecord` | Ở lá: tổng điểm transaction loại `CHECKOUT_REWARD`. Ở cha: tổng nguồn thô của các nhánh được tính. |
| `manualPointRecord` | Ở lá: tổng điểm của các transaction còn lại, gồm `MANUAL` và các loại trừ/thu hồi lịch sử. Ở cha: tổng nguồn thô của các nhánh được tính. |
| `totalPoint` | Điểm **trước trần của chính node**. Ở lá là tổng hai nguồn sau khi từng nguồn được chặn tối thiểu ở 0. |
| Điểm hiệu lực | `effective(node) = min(node.maximum, node.totalPoint)`. Đây là điểm node đóng góp lên cha và điểm FE hiển thị. |

BE cộng điểm transaction cùng nguồn trước, rồi chặn mỗi nguồn ở 0. Vì vậy các transaction trừ điểm lịch sử được cộng theo giá trị có dấu; kết quả không phụ thuộc thứ tự duyệt. Nếu phép cộng vượt giới hạn số nguyên, BE báo lỗi thay vì lưu số sai.

Node cha **SUM** lưu `totalPoint = Σ effective(con trực tiếp)`. Node cha **MAX** lưu `totalPoint = max(effective(con trực tiếp))`. Trần của cha chỉ được áp khi cha góp lên ông hoặc khi hiển thị. Điểm nguồn của cha vẫn là số thô của các nhánh đóng góp; không dùng `systemPointRecord + manualPointRecord` ở cha để suy ra điểm hiệu lực vì các trần cấp dưới đã được áp trước đó.

Với cha MAX, BE chọn con có điểm hiệu lực cao nhất. Nếu hòa, ưu tiên `manualPointRecord` thô cao hơn; nếu vẫn hòa, chọn `categoryId` nhỏ hơn. `selectedChildId` của cha và `contributesToParent` của con cho FE biết nhánh được chọn. Với cha SUM, mọi con đều đóng góp.

Metadata BE cho mỗi node: `isAtMaximum = maximum > 0 && effective >= maximum`; `completionPercentage = maximum == 0 ? 0 : effective * 100 / maximum` (chia nguyên). Trần 0 có điểm hiệu lực và phần trăm bằng 0, `isAtMaximum = false`.

**Ví dụ:** Lá A có trần 10, hệ thống 8 và hai ghi nhận tay 4 + 3: lá lưu `system=8`, `manual=7`, `total=15`, hiển thị **10/10** và chỉ góp **10** lên cha. Lá B có trần 20, `total=7`. Cha SUM trần 12 sẽ lưu `total=10+7=17`, hiển thị **12/12** và góp **12** lên ông. Các nguồn thô của cha có thể cộng thành 22; đó là thông tin nguồn, không phải điểm hiệu lực 12. Nếu cha là MAX, cha lưu `total=10`, chọn A. Nếu hai con cùng đạt 10 hiệu lực, quy tắc hòa ở trên quyết định con thắng.

Nguồn: `StudentSemesterPointCalculator.java`.

## 3. Khi dữ liệu thay đổi và điểm được lưu

- **Sinh viên thêm** ghi nhận tay: POST tạo một transaction mới với điểm và mô tả; BE rebuild toàn bộ cặp sinh viên/học kỳ. Thêm 5 vào transaction 8 tạo tổng thô 13, không thay transaction 8.
- **Sinh viên sửa**: PATCH theo **transaction ID** đổi `points` và `description` của đúng ghi nhận `MANUAL` thuộc sinh viên đó, giữ ID rồi rebuild. Sửa transaction 8 thành 5 còn tổng 5 nếu không có ghi nhận khác.
- **Sinh viên xóa**: DELETE đúng transaction `MANUAL` thuộc mình, xóa vật lý rồi rebuild từ các transaction còn lại.
- **Hệ thống chốt sự kiện**: lưu transaction `CHECKOUT_REWARD` và rebuild cho sinh viên nhận điểm.
- **Admin đổi cấu hình**: BE so cây hiệu lực trước/sau của các học kỳ có transaction hoặc snapshot. Khi cây, trần, nhánh MAX, ngày áp dụng hoặc ngày học kỳ làm đổi phép tính, BE rebuild các cặp sinh viên/học kỳ bị ảnh hưởng trước khi thao tác thành công. Snapshot của node ra khỏi cây được đưa về 0; transaction lịch sử vẫn còn.

Rebuild đọc ledger một lần theo batch, tính lại mọi node bằng cùng calculator, rồi cập nhật `student_semester_points`. Snapshot đã có giữ ID và thời gian `createdAt`/`finalizedAt`; chạy lại với cùng dữ liệu cho cùng kết quả. Các thao tác tuần tự đã được kiểm thử; xử lý race condition được để ở giai đoạn sau.

Nguồn: `PointTransactionCommandServiceImpl.java`, `StudentSemesterPointCommandServiceImpl.java`, `PointConfigurationRefresh.java`.

## 4. API BE trả gì

| Endpoint | Dữ liệu chính |
| --- | --- |
| `GET /student-semester-points/grouped-tree/{semesterId}` | Root và các item phẳng theo cây: `maximum`, **raw** `totalPoint`, `systemPointRecord`, `manualPointRecord`, `isAtMaximum`, `completionPercentage`, `selectedChildId`, `contributesToParent`, ID snapshot và `finalizedAt`. |
| `GET /student-semester-points/me/semesters/{semesterId}` | Danh sách node với cùng raw `totalPoint` và hai nguồn điểm. |
| `GET /student-semester-points/me/semesters/{semesterId}/summary` | `semesterId`, raw tổng học kỳ `totalPoint`, `maximum=100`, `classificationLabel`, `nextClassificationLabel`, `pointsNeeded`, `activitiesCount`. |
| `GET /point-transactions/me/semesters/{semesterId}/point-categories/{categoryId}` | Lịch sử transaction của một danh mục để xem minh chứng và chọn đúng ID ghi nhận tay khi sửa/xóa. |

GET tính lại từ ledger và cây hiệu lực hiện tại bằng calculator chung; snapshot lưu được dùng để ghép ID/thông tin chốt, **GET không ghi DB**. Vì thế kết quả đọc phản ánh cấu hình hiện hành ngay cả trước khi chạy công cụ repair dữ liệu cũ.

Summary cộng **điểm hiệu lực của các root** để có `totalPoint` học kỳ. FE hiển thị `min(100, totalPoint)`. BE dùng điểm sau trần 100 để xếp loại: 0–34 Kém, 35–49 Yếu, 50–79 Trung Bình, 80–89 Tốt, từ 90 Xuất sắc. `pointsNeeded` là số điểm còn thiếu đến ngưỡng xếp loại kế tiếp, bằng 0 ở mức cao nhất. `activitiesCount` đếm đăng ký của đúng học kỳ có trạng thái `COMPLETED`, `CONFIRMED` hoặc `RECORDED`; đây là trạng thái đăng ký, không phải bộ lọc transaction điểm.

Nguồn: `StudentSemesterPointQueryServiceImpl.java`, `StudentSemesterPointController.java`.

## 5. FE hiển thị và thao tác

Trang điểm và sidebar gọi cùng tree/summary API với query key chứa **user và semester**. Cột điểm thực tế của mọi node hiển thị `Math.min(maximum, totalPoint)`. Hai cột nguồn điểm lấy từ `systemPointRecord` và `manualPointRecord`: **node lá hiện nguyên giá trị BE kể cả vượt trần; node cha và dòng tổng mục hiện `min(maximum, điểm nguồn)` riêng cho từng cột**. Đây là quy tắc trình bày, không đổi dữ liệu BE hay phép tính `totalPoint`. Hai cột nguồn ở cha có thể cộng lại lớn hơn điểm thực tế vì chúng được áp trần riêng và vì BE đã áp trần ở các cấp con. Badge đủ trần lấy `isAtMaximum`, thanh tiến độ lấy `completionPercentage`, dấu nhánh MAX không được cộng lấy `contributesToParent`. Modal chi tiết dùng cùng dữ liệu node và lịch sử transaction để trình bày, không tính lại tổng từ lịch sử.

Điểm học kỳ, nhãn xếp loại, số điểm cần lên hạng và số hoạt động lấy từ **summary BE**. FE chỉ áp trần của summary để hiển thị con số (`min(summary.maximum, summary.totalPoint)`). FE không cộng các node, không chọn lại con thắng MAX và không tự tính xếp loại. Nếu lỗi tải tree/summary, màn hình báo lỗi; nếu chưa có học kỳ cấu hình, không fallback sang học kỳ ID khác.

Form **thêm** mở trống và POST; form **sửa** mở với `points`/`description` của transaction được chọn và PATCH đúng ID; **xóa** DELETE đúng ID. Sau mutation, FE invalidate/refetch tree, summary và lịch sử. Nếu lưu lỗi, form giữ bản nháp và báo lỗi, không giả định điểm đã được ghi. Một danh mục có nhiều ghi nhận tay sẽ hiển thị từng ghi nhận riêng để chọn sửa/xóa.

Nguồn FE: `TrainingPointsPage.tsx`, `TrainingPointsSectionAccordion.tsx`, `TrainingPointsRow.tsx`, `CriterionDetailModal.tsx`, `StudentRightSidebar.tsx`, `trainingPointsService.ts`.

## 6. Đối soát khi dữ liệu lịch sử sai

`SemesterPointReconciliationService` chạy preview từ ledger bằng cùng calculator, báo chênh lệch snapshot, transaction không hợp lệ, số dư thiếu ledger và từng manual ID/điểm/note. Repair chỉ dựng lại snapshot; không gộp hay xóa transaction. Hướng dẫn chạy và báo cáo local mẫu nằm tại `docs/artifact/student-semester-point-tasks-4-7.md` và `docs/artifact/student-semester-point-reconcile-local-example.json`.

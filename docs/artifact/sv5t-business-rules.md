# Quy tắc nghiệp vụ ghi nhận tiêu chí Sinh viên 5 tốt

Quy tắc ghi nhận và theo dõi tiêu chí Sinh viên 5 tốt được chốt ngày 26/11/2026. Hệ thống hỗ trợ sinh viên theo dõi tiến độ bằng ghi nhận tay và ghi nhận từ hệ thống. BE tính kết quả và trả dữ liệu để FE hiển thị.

# Phạm vi theo dõi

Mỗi ghi nhận gắn với một sinh viên, một tiêu chí lá và một năm học. Hai cấp xét là `UNIVERSITY` và `FACULTY`. Tiến độ được truy vấn theo sinh viên đăng nhập, năm học và cấp xét đã chọn.

# Ghi nhận tay

- Mỗi lần sinh viên thêm ghi nhận tay tạo một transaction `MANUAL`, có hiệu lực ngay khi lưu.

- Sinh viên chỉ nhập lý do trong `note`. Tiêu chí và năm học lấy từ ngữ cảnh thao tác.

- Mỗi transaction tay còn hoạt động đóng góp một lần ghi nhận.

- Sinh viên được sửa note hoặc xóa từng ghi nhận tay của mình, kể cả ở năm học cũ. Hiện không giới hạn thời gian thao tác.

- Sửa note giữ nguyên transaction, tiêu chí, năm học và số lần ghi nhận.

- Sinh viên chỉ được sửa hoặc xóa các transaction tay thuộc về mình.

- Ghi nhận tay được cộng độc lập với ghi nhận hệ thống. Hệ thống không đối chiếu lý do sinh viên nhập để gộp các hoạt động trùng nhau.

# Ghi nhận từ hệ thống

Hệ thống tạo transaction từ các luồng nghiệp vụ đủ điều kiện, chẳng hạn ghi nhận tham gia khi chốt sự kiện. Các nguồn hiện có là `EVENT_CHECKOUT` và `SYSTEM`. Các ghi nhận hệ thống hợp lệ được cộng cùng ghi nhận tay để xác định tiến độ.

Retry xử lý cùng một sự kiện phải giữ tính idempotent. Trong cách tổng hợp hiện tại, các transaction hệ thống gắn cùng một sự kiện được tính một lần. Các transaction `MANUAL` vẫn được tính riêng theo từng transaction. Transaction có `isActive = false` được loại khỏi kết quả tổng hợp.

# Điều kiện đạt của tiêu chí lá

`requiredOccurrences` là số lần cần đạt. `completedOccurrences` là tổng số lần hợp lệ từ ghi nhận tay và hệ thống. Tiêu chí lá đạt khi `completedOccurrences >= requiredOccurrences`. BE trả kết quả qua `isCompleted`. Nếu ngưỡng của tiêu chí lá chưa được khai báo, giá trị mặc định hiện tại là 1.

| Số lần cần đạt | Ghi nhận tay | Ghi nhận hệ thống | Tổng số lần | Kết quả |
| --- | --- | --- | --- | --- |
| 1 | 0 | 0 | 0 | Chưa đạt |
| 1 | 1 | 0 | 1 | Đạt |
| 3 | 1 | 1 | 2 | Chưa đạt |
| 3 | 2 | 1 | 3 | Đạt |
| 3 | 3 | 0 | 3 | Đạt |
| 3 | 0 | 3 | 3 | Đạt |

`isSelfEvaluated = true` cho biết sinh viên có ghi nhận tay còn hoạt động. Trường này được suy ra từ `student_criteria_transactions`, với `completion_source = MANUAL`, đúng sinh viên, tiêu chí và năm học. Việc có ghi nhận tay chỉ làm tiêu chí đạt khi tổng số lần đã đủ ngưỡng.

`isSystemRecorded` cho biết có ghi nhận hệ thống. `isSystemCompleted` biểu thị kết quả xét riêng nguồn hệ thống. Trạng thái đạt chung trên giao diện sử dụng `isCompleted`.

# Điều kiện đạt của tiêu chí cha

`requiredChildren` là số tiêu chí con trực tiếp cần đạt. `completedChildrenCount` là số con trực tiếp có `isCompleted = true`. Tiêu chí cha đạt khi `completedChildrenCount >= requiredChildren`. Với nút cha có `requiredChildren` trống, ngưỡng mặc định là 1.

Một tiêu chí con đóng góp tối đa một đơn vị vào số con đạt của cha, dù số lần ghi nhận của con vượt ngưỡng. Quy tắc được áp dụng từ dưới lên qua tất cả cấp tổ tiên.

Ví dụ tiêu chí 4 cần đạt một trong ba tiêu chí 4.1, 4.2 hoặc 4.3. Khi 4.3 đủ 2/2 lần và có `isCompleted = true`, tiêu chí 4 đạt. Nếu xóa một ghi nhận khiến 4.3 còn 1/2 lần và các con khác đều chưa đạt, tiêu chí 4 trở về chưa đạt.

# Xử lý khi thêm sửa hoặc xóa

1. BE xác thực người thao tác và quyền với transaction.

2. BE bảo vệ cập nhật đồng thời theo sinh viên và năm học.

3. BE tạo, sửa note hoặc xóa transaction được chọn.

4. BE tổng hợp lại tiêu chí lá và các cấp cha theo giao dịch còn hiệu lực cùng cấu hình hiện tại.

5. BE lưu tiến độ và hoàn tất thao tác. Nếu cập nhật thất bại, transaction nghiệp vụ được rollback.

6. FE tải lại tiến độ từ BE để hiển thị kết quả mới.

Thêm ghi nhận có thể làm tiêu chí chuyển từ chưa đạt sang đạt. Xóa ghi nhận có thể làm tiêu chí chuyển từ đạt về chưa đạt. Sửa note giữ nguyên số lần. FE thao tác bằng ID của từng transaction để sửa giữ nguyên bản ghi và xóa đúng ghi nhận được chọn.

# Khi admin thay đổi ngưỡng

Thay đổi `requiredOccurrences` hoặc `requiredChildren` được lưu vào cấu hình tiêu chí. API tiến độ đọc cấu hình hiện hành và tổng hợp lại từ các transaction khi sinh viên tải tiến độ. Kết quả lá, cha và tổng tiêu chuẩn đạt vì vậy phản ánh ngưỡng mới ở lần tải tiếp theo.

Ví dụ sinh viên có tổng 3 lần ghi nhận. Tăng `requiredOccurrences` từ 3 lên 4 làm tiêu chí trở về chưa đạt. Giảm ngưỡng từ 4 xuống 2 làm tiêu chí đạt. Các cấp cha được xét lại tương ứng. Khi admin chuyển một tiêu chí sang cha khác, lần truy vấn tiếp theo sử dụng cấu trúc cây hiện hành.

Trong triển khai hiện tại, thao tác sửa cấu hình của admin cập nhật bảng `criterias`. Các dòng đã lưu trong `student_criteria_progress` được tính lại khi có mutation ghi nhận hoặc chạy đối soát. Vì vậy, ngay sau khi admin sửa ngưỡng, bảng tiến độ có thể còn giá trị cũ dù response API đã tính theo ngưỡng mới. FE nhận kết quả mới khi refetch.

# Quy tắc hiển thị FE

- Trạng thái đạt hoặc chưa đạt của tiêu chí lấy từ `node.isCompleted` do BE trả về.

- FE hiển thị số lần và tổng tiến độ từ response. FE không cộng lại lịch sử hoặc dùng localStorage để quyết định đạt.

- Sinh viên có ghi nhận tay nhưng tổng số lần chưa đủ ngưỡng vẫn hiển thị “Chưa đạt”.

- Modal chi tiết hiển thị mỗi ghi nhận tay một dòng, gồm lý do, ngày ghi nhận và nút sửa hoặc xóa đúng transaction.

- Form thêm hoặc sửa ghi nhận tay chỉ nhập lý do. Kết quả đạt được BE tính sau khi lưu.

- Ngày ghi nhận trên UI hiện lấy từ `createdAt` của API giao dịch.

- Lịch sử giao dịch phục vụ xem chi tiết và thao tác từng ghi nhận. Trạng thái tiêu chí lấy từ response tiến độ.

# API hiện tại

| Thao tác | API | Dữ liệu chính |
| --- | --- | --- |
| Thêm ghi nhận tay | `POST /criteria-transactions/me` | `criteriaId`, `academicYearId`, `note` |
| Sửa lý do | `PATCH /criteria-transactions/me/{criteriaTransactionId}` | `note` |
| Xóa ghi nhận tay | `DELETE /criteria-transactions/me/{criteriaTransactionId}` | ID transaction |
| Xem lịch sử một tiêu chí | `GET /criteria-transactions/me/criterias/{criteriaId}/academic-years/{academicYearId}` | Tiêu chí và năm học |
| Xem cây tiến độ | `GET /criteria-progress/me/academic-years/{academicYearId}?scope=UNIVERSITY` | Scope `UNIVERSITY` hoặc `FACULTY` |
| Xem một node tiến độ | `GET /criteria-progress/me/criterias/{criteriaId}/academic-years/{academicYearId}?scope=UNIVERSITY` | Tiêu chí, năm học và scope |

# Các tình huống nghiệm thu

- Thêm ba ghi nhận tay cho lá cần ba lần làm lá và các cấp cha đủ điều kiện chuyển sang đạt.

- Hai ghi nhận tay cùng một ghi nhận hệ thống đáp ứng ngưỡng ba lần.

- Sửa note của một ghi nhận giữ nguyên ID và số lần.

- Xóa một ghi nhận từ tổng ba lần xuống hai lần làm lá cần ba lần trở về chưa đạt, đồng thời xét lại các cấp cha.

- Một con đạt đủ để cha có `requiredChildren` trống đạt.

- Tăng hoặc giảm ngưỡng ở admin làm response tiến độ thay đổi đúng ở lần tải tiếp theo.

- Xử lý lại cùng một sự kiện giữ nguyên số lần được ghi nhận từ sự kiện đó.

- Mỗi ghi nhận tay xuất hiện một lần trên UI và nút sửa hoặc xóa tác động đúng transaction.


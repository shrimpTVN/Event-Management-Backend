# Checkpoint điểm học kỳ task 1 đến 3

Cập nhật ngày 27/11/2026.

## Đã hoàn tất

Task 1–3 đã được phục hồi từ patch dự phòng và áp vào repo `/home/shrimp/code/EventManagement/ddd`. Code nằm trong các module ddd-application, ddd-domain, ddd-infrastructure và ddd-api. Đã chạy lại 33 test liên quan pass và review độc lập không có phát hiện quan trọng.

Calculator tính từ mọi transaction còn tồn tại trong DB, không lọc status. DELETE xóa vật lý transaction rồi rebuild. POST thêm khoản điểm mới, PATCH sửa đúng ID của transaction MANUAL thuộc sinh viên. Các luồng tạo/sửa/chốt sự kiện kiểm tra lá và bộ danh mục hiệu lực.

Plan theo dõi tại `docs/superpowers/plans/2026-11-26-student-semester-point-backend-refactor.md`. Có thể xem commit refactor trong git log để tiếp tục, thay vì áp lại patch dự phòng cũ.

## Kiểm chứng và giới hạn

Log kiểm thử ở `/tmp/semester-point-implementation/green.log`. Có 32 test application và 1 test API pass. Rollback được kiểm tra qua Spring transaction manager với mock JDBC connection. Chưa chạy integration toàn luồng trên DB thật.

Bộ RegistrationCommandServiceImplTest cũ có 9 failure và 4 error đã xác nhận là baseline trước refactor. Các test RegistrationPointAwardTest của luồng phần thưởng mới pass.

## Việc còn lại

Task 4 cập nhật snapshot khi admin đổi cấu hình. Task 5 thống nhất API đọc và summary. Task 6 chuyển FE sang dùng response BE và PATCH theo transaction. Task 7 đối soát, repair và triển khai. Phạm vi được yêu cầu thực hiện đến nay là task 1–3. Race condition được dành cho giai đoạn sau.

FE và API đọc vẫn theo luồng cũ cho tới các task tiếp theo. Metadata calculator chưa được đưa vào response.

## Bản dự phòng

Patch trước khi áp code ở `docs/artifact/student-semester-point-tasks-1-3.patch`. Đây là bản phục hồi lịch sử. Source hiện tại trong repo và git log là trạng thái tiếp tục công việc.

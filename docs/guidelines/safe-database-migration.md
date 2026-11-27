# Hướng dẫn migration PostgreSQL an toàn cho dự án DDD

## 1. Phạm vi và nguồn sự thật

Tài liệu này dựa trên `ddd-infrastructure/src/main/resources/database/schema.sql`, các file trong `ddd-infrastructure/src/main/resources/database/migration/`, `ddd-infrastructure/src/main/resources/database/refresh_token_schema.sql`, cấu hình `application.yml` và các migration integration test hiện có. Đây là quy trình vận hành; **không thực thi SQL lên database nào**.

- `schema.sql` là bản schema đầy đủ cho **database mới, trống**. Nhiều thay đổi trong `migration/` đã được gộp vào đó. Không chạy `schema.sql` trên database đã có dữ liệu: các `CREATE TABLE`, `CREATE INDEX`, `CREATE TRIGGER` không được thiết kế để chạy lại.
- `migration/` là các script thay đổi **database đang tồn tại**. Tên file không có version/timestamp và hiện không thấy Flyway/Liquibase trong `pom.xml` hoặc lịch sử migration tự động. Thứ tự theo tên file **không phải** thứ tự triển khai hợp lệ.
- `spring.jpa.hibernate.ddl-auto: update` đang bật. Hibernate có thể đổi schema khi ứng dụng khởi động, khiến kết quả migration khác giữa các môi trường. Trước khi áp dụng migration có kiểm soát ở staging/production, đặt `ddl-auto=validate` hoặc `none` bằng cấu hình môi trường, rồi chạy DDL qua một đường duy nhất. Không đổi cấu hình production khi chưa thử trên bản sao DB.
- `schema.sql` mô tả trạng thái mong muốn, **database thật mới là trạng thái xuất phát**. Không suy ra migration đã chạy chỉ từ việc cột/bảng xuất hiện: Hibernate `update` hoặc thao tác thủ công cũng có thể tạo chúng.

## 2. Các điểm nguy hiểm đã thấy trong repo

| File/nhóm | Điều phải kiểm tra trước khi chạy |
| --- | --- |
| `student-criteria-progress-migration.sql` → `student-criteria-transaction-migration.sql` → `student-criteria-progress-completed-occurrences-migration.sql` | Có quan hệ thứ tự, có backfill. Script transaction có `TRUNCATE student_criteria_progress`, `DROP COLUMN`, không bọc toàn bộ bằng `BEGIN/COMMIT`. Phần copy đọc `scp.note` dù migration progress trước đó không bảo đảm cột này. **Không chạy nguyên bản trên dữ liệu thật**; tạo bản migration đã kiểm thử riêng sau khi kiểm tra schema nguồn và đối soát số hàng. |
| `criteria-progress-reconcile.sql` | Là công cụ đối soát **dry-run** và kết thúc bằng `ROLLBACK`. Chỉ dùng để xem chênh lệch. Chú thích về đổi `COMMIT` không có hiệu lực nếu không tạo bản repair được kiểm tra riêng; không sửa trực tiếp file lịch sử rồi chạy tùy tiện. |
| `notification-type-refactor-migration.sql` → `notification-absence-alert-type-preflight.sql` → `notification-absence-alert-type-migration.sql` | Cần phân biệt schema cũ có `title` hay đã có `type`, kiểm tra dữ liệu quá 50 ký tự và giá trị enum. Script preflight yêu cầu `ddd.notification_migration_schema`; script cuối lấy `ACCESS EXCLUSIVE` lock, có timeout 5 giây/60 giây. Thử trên bản sao có dữ liệu thật và chọn cửa sổ ít tải. |
| `notification-sequence-migration.sql` | Đổi cơ chế cấp `notifications.id`; đối chiếu `MAX(id)`, sequence hiện tại và chiến lược ID của JPA trước khi chạy. Không chạy lại nếu chưa kiểm tra sequence/ownership và khả năng tương thích với entity hiện tại. |
| `event-view-batch-migration.sql` → `daily-event-recommendation.sql` | File daily tự ghi phụ thuộc event-view. Có tạo trigger/function và backfill summary, nên đo thời gian, lock và row count trên bản sao trước. |
| `staff-profile-uniqueness-migration.sql` | Đã có kiểm tra trùng `user_id`, `staff_id`, giá trị rỗng và trùng sau `trim`, nhưng vẫn khóa bảng để chuẩn hóa và tạo unique. Xử lý dữ liệu bẩn trước; không tắt kiểm tra để ép chạy. |
| `criteria-scope-faculty-migration.sql` | Đổi dữ liệu `SCHOOL` → `FACULTY`; kiểm tra trước giá trị `scope`, và chỉ triển khai khi BE đọc được giá trị mới. |
| `fanpage-rejection-reason-migration.sql` | File mới đang chưa commit cùng thay đổi ở `schema.sql`. Là `ADD COLUMN IF NOT EXISTS`; cần xác định DB đích đã có cột hay chưa. Triển khai DB trước hoặc cùng bản BE đọc/ghi cột; không chạy lại cả `schema.sql`. |
| `refresh_token_schema.sql` | Là script riêng, không phải migration versioned. Có hai FK trên cùng `user_id` với hành vi xóa khác nhau (`CASCADE`, `RESTRICT`); **không áp dụng nguyên bản** nếu chưa quyết định FK nào đúng và đối chiếu bảng `refresh_tokens` trong schema chính. |

Các migration thêm cột nhỏ như `event-self-check`, `event-gender-constraint`, `event-school`, `fanpage-evidence-url`, `point-category-is-max-child`, `student-semester-point-records` có `IF NOT EXISTS` cho một số bước. Điều đó không chứng minh toàn file idempotent: ví dụ `event-school` còn drop/add FK. Kiểm tra từng câu lệnh và object thực tế trước khi chạy lại.

## 3. Quy trình cho mỗi lần thay đổi

### A. Chuẩn bị thay đổi

1. Ghi rõ môi trường, DB, schema, phiên bản ứng dụng hiện tại, bản phát hành đích, người phụ trách và thời điểm bảo trì. Chốt một migration cụ thể; không dùng glob `migration/*.sql`.
2. Kiểm tra working tree. Hiện có thay đổi chưa commit ở `schema.sql` và `fanpage-rejection-reason-migration.sql`; xác định chúng thuộc bản phát hành nào trước khi đóng gói. Không trộn các thay đổi chưa hoàn tất vào migration khác.
3. Viết migration kiểu **expand → backfill → switch → contract**: thêm cột/bảng tương thích trước, backfill theo lô nếu bảng lớn, triển khai BE đọc/ghi cấu trúc mới, chỉ xóa cấu trúc cũ ở lần phát hành sau khi xác nhận không còn dùng. Với `NOT NULL`/unique/FK, kiểm tra dữ liệu trước và thêm ràng buộc sau khi đã sạch.
4. Migration phải có tiền điều kiện và hậu điều kiện đo được: số hàng nguồn/đích, số khóa trùng, số NULL, constraint, index, trigger và vài truy vấn nghiệp vụ đại diện. Với thao tác data rewrite, định nghĩa cách khôi phục dữ liệu trước khi chạy.
5. Gắn **mã version bất biến** cho migration mới và ghi version, checksum, thời gian, kết quả vào bảng lịch sử hoặc công cụ migration. Không sửa migration đã chạy; tạo migration sửa tiếp theo. Có thể chuyển dần sang Flyway/Liquibase sau khi baseline trạng thái các DB hiện có, nhưng không tự baseline khi chưa đối chiếu drift.

### B. Preflight trên database đích (chỉ đọc)

1. Xác nhận kết nối đúng DB/schema bằng `SELECT current_database(), current_schema(), current_user, version();`; đối chiếu với bản ghi triển khai. Dùng tài khoản chỉ đọc cho bước này.
2. Chụp inventory `information_schema.columns`, `pg_constraint`, `pg_indexes`, trigger/sequence cho **chỉ những bảng bị tác động**. So sánh với precondition của file SQL, không so sánh mù với toàn bộ `schema.sql`.
3. Đếm và lấy mẫu dữ liệu có thể làm migration thất bại: duplicate trước unique, NULL trước `NOT NULL`, giá trị ngoài enum/check, FK mồ côi, độ dài vượt cột mới. Dùng `notification-absence-alert-type-preflight.sql` khi đổi notification type.
4. Xác nhận backup/snapshot vừa hoàn tất và đã thử restore trên DB tạm; ghi lại vị trí backup, thời gian và cách khôi phục. Không dùng một câu `ROLLBACK` để hứa khôi phục sau khi transaction đã `COMMIT`.
5. Kiểm tra kích thước bảng, lưu lượng ghi, phiên giao dịch dài và thời gian khóa dự kiến. Nếu bảng lớn, thử trên bản sao dữ liệu sản xuất đã ẩn danh. Đặt ngưỡng dừng nếu khóa hoặc backfill vượt ngân sách thời gian.

### B.1. Đối chiếu file migration với database **đang chạy**

Đến thời điểm viết tài liệu, chưa có kết nối tới database đích trong lần rà soát này. Những nhận xét ở mục 2 là rủi ro **suy ra từ file trong repo**, chưa chứng minh trạng thái DB hiện tại. Trước mỗi lần chạy, tạo một bảng đối chiếu riêng cho môi trường đích; không dùng kết quả của local thay cho staging/production.

| Đối tượng/câu lệnh trong file | Trạng thái DB cần ghi lại | Quyết định |
| --- | --- | --- |
| `CREATE TABLE`, `ADD COLUMN` | Có/chưa có; tên, kiểu, nullable, default, identity và owner có khớp không? | Chưa có và tiền điều kiện đạt: có thể chạy. Đã có nhưng khác định nghĩa: dừng, làm migration điều chỉnh. |
| `ALTER TYPE`, `SET NOT NULL`, `CHECK`, `UNIQUE`, FK | Dữ liệu hiện có có vi phạm không? Constraint/index tương đương đã tồn tại dưới tên khác không? | Có vi phạm hoặc định nghĩa khác: dừng và xử lý dữ liệu/thiết kế trước. |
| `UPDATE`, `INSERT ... SELECT`, backfill | Số hàng dự kiến đổi, khóa nối, hàng không ghép được, hàng đã chuyển và cách chạy lại | Chỉ chạy sau khi có truy vấn đối soát trước/sau và quy tắc xử lý bản ghi bất thường. |
| `DROP`, `TRUNCATE`, rename | Object còn được app cũ, job, report, trigger hoặc FK dùng không? Dữ liệu đã được sao lưu/chuyển đầy đủ chưa? | Nếu chưa chứng minh được, dừng. Không coi `IF EXISTS` là bằng chứng an toàn. |
| Index, sequence, trigger, function | Định nghĩa hiện tại, phụ thuộc, `MAX(id)`, thời gian tạo/khóa, tác dụng phụ khi chạy lại | Nếu tên giống nhưng định nghĩa khác, dừng; không mặc định ghi đè. |

Các lệnh mẫu **chỉ đọc** dưới đây dùng `public` làm ví dụ; thay bằng schema đã xác nhận ở bước B.1. Lưu output cùng bản ghi triển khai:

```sql
SELECT current_database(), current_schema(), current_user, version();

SELECT table_schema, table_name, column_name, data_type,
       is_nullable, column_default, is_identity
FROM information_schema.columns
WHERE table_schema = 'public'
  AND table_name IN ('fanpages', 'student_criteria_progress', 'notifications')
ORDER BY table_name, ordinal_position;

SELECT n.nspname AS schema_name, c.relname AS table_name,
       con.conname, con.contype, pg_get_constraintdef(con.oid) AS definition
FROM pg_constraint con
JOIN pg_class c ON c.oid = con.conrelid
JOIN pg_namespace n ON n.oid = c.relnamespace
WHERE n.nspname = 'public'
  AND c.relname IN ('fanpages', 'student_criteria_progress', 'notifications')
ORDER BY c.relname, con.conname;

SELECT schemaname, tablename, indexname, indexdef
FROM pg_indexes
WHERE schemaname = 'public'
  AND tablename IN ('fanpages', 'student_criteria_progress', 'notifications')
ORDER BY tablename, indexname;
```

Sau inventory cấu trúc, viết truy vấn dữ liệu **riêng cho file sẽ chạy**. Ví dụ: trước unique của `fanpage_admin_profiles` đếm `user_id`/`staff_id` trùng và trùng sau `trim`; trước `notification-type-refactor` đếm `title` dài hơn 50 ký tự và các `type` nằm ngoài allowlist; trước chuyển criteria ghi số hàng progress, số transaction, số tổ hợp `(user_id, criteria_id, academic_year_id)` và số hàng sẽ bị `TRUNCATE`. Không dùng `COUNT(*)` của một bảng làm bằng chứng duy nhất rằng backfill đúng: phải đối chiếu theo khóa nghiệp vụ và các giá trị quan trọng.

### B.2. Câu hỏi bắt buộc trước khi chạy **một file** migration

Ghi câu trả lời và bằng chứng cho từng câu. Nếu chưa trả lời được câu liên quan tới file đó, **dừng trước khi chạy**.

1. **Đúng đích chưa?** Tên môi trường, host, database, schema, user và `search_path` đã được xác nhận độc lập chưa? File có tên bảng không kèm schema hoặc tự dùng `public`/session setting không?
2. **DB đang ở phiên bản nào?** Có lịch sử áp dụng file này chưa, checksum nào đã chạy, và object hiện tại có đúng định nghĩa kỳ vọng không? Nếu không có migration history, đã kiểm tra trực tiếp từng object chưa?
3. **Tiền điều kiện đã đủ chưa?** Các file phụ thuộc đã chạy chưa, các bảng/hàm/trigger nguồn có tồn tại không, và app đang chạy có tương thích với trạng thái trung gian không?
4. **Dữ liệu hiện tại có cho phép đổi không?** Có NULL, duplicate, orphan FK, giá trị enum cũ, chuỗi quá dài hoặc bản ghi trái quy tắc không? Có bao nhiêu hàng sẽ đổi hoặc bị xóa?
5. **File thực sự sẽ làm gì khi chạy lại?** `IF NOT EXISTS` có che một object sai kiểu không? `INSERT ... SELECT` có tạo thêm bản ghi không? `UPDATE` có ghi đè dữ liệu đã được người dùng chỉnh sửa không? Trigger có gây ghi dây chuyền không?
6. **Ranh giới transaction ở đâu?** File có `BEGIN`, `COMMIT`, `ROLLBACK`, `TRUNCATE` hay DDL không chạy trong transaction không? Nếu lỗi giữa chừng, phần nào đã commit?
7. **Ảnh hưởng vận hành là gì?** Câu nào lấy `ACCESS EXCLUSIVE` lock, scan toàn bảng hoặc build index? Có timeout, cửa sổ ít tải và ngưỡng dừng cụ thể không?
8. **Khôi phục bằng gì?** Backup đã thử restore chưa? Có thể forward fix hay phải restore? Nếu file xóa/cập nhật dữ liệu, dữ liệu gốc được giữ ở đâu?
9. **Hậu kiểm đo thế nào?** Kết quả cấu trúc, row count, tổng hợp nghiệp vụ và API nào phải đúng? Ai xác nhận đạt trước khi triển khai bước tiếp?
10. **Ai chịu trách nhiệm quyết định?** Có người duyệt khác người chạy cho migration phá hủy dữ liệu, bản ghi thời điểm chạy, checksum và kế hoạch truyền thông khi phải dừng không?

Với file chỉ thêm cột nullable như `fanpage-rejection-reason-migration.sql`, các câu 4, 6 và 7 vẫn phải trả lời: thao tác DDL có thể đợi lock, và `IF NOT EXISTS` không kiểm tra kiểu của cột đã tồn tại. Với file chuyển dữ liệu như `student-criteria-transaction-migration.sql`, cần trả lời đủ cả 10 câu trước khi lập lịch chạy.

### C. Dry-run trên bản sao rồi triển khai

1. Restore bản sao vào staging, chạy đúng version ứng dụng cũ và migration dự kiến, sau đó chạy ứng dụng mới. Kiểm tra cả hướng tương thích ngược khi cần triển khai rolling.
2. Kiểm tra script có tự `BEGIN`, `COMMIT`, `ROLLBACK` hay không. Không tự động dùng `psql --single-transaction` cho file đã tự điều khiển transaction; nó có thể phá giả định của script. Với file không có transaction, **đóng gói một bản migration an toàn** hoặc chạy qua công cụ migration với transaction được quản lý rõ ràng. Tránh dùng `CREATE INDEX CONCURRENTLY` trong transaction; tách nó thành bước riêng nếu cần giảm khóa.
3. Dùng `psql -X -v ON_ERROR_STOP=1 -f <file>` với kết nối được cấu hình ngoài command line (ví dụ `PGSERVICE`) để dừng ngay khi có lỗi. Ghi log đầu ra, version và checksum; không ghi mật khẩu vào shell history. Đặt `lock_timeout` và `statement_timeout` phù hợp với từng script, sau khi thử trên staging.
4. Triển khai từng migration theo dependency đã duyệt; xác minh hậu điều kiện và chức năng BE ngay sau mỗi bước. Chỉ đánh dấu thành công trong lịch sử khi SQL **đã commit và hậu kiểm đạt**. Nếu thất bại, dừng chuỗi triển khai; không tự chạy lại toàn bộ.
5. Quan sát lỗi API, connection pool, deadlock, lock wait, thời gian query và dữ liệu nghiệp vụ trong khoảng thời gian giám sát đã định. Với lỗi sau commit, chọn forward fix hoặc restore theo runbook đã thử; rollback ứng dụng đơn thuần không đảo ngược dữ liệu.

### D. Hậu kiểm tối thiểu

- Bảng/cột/ràng buộc/index/trigger đúng như migration dự kiến; không có đối tượng bị tạo trùng.
- Số hàng và tổng hợp nghiệp vụ trước/sau khớp quy tắc chuyển đổi; với student criteria, đối chiếu transaction và progress bằng script dry-run trước khi sửa dữ liệu.
- Ứng dụng mới khởi động với `ddl-auto=validate` và các flow đọc/ghi liên quan hoạt động. Nếu validation báo drift, dừng phát hành, không bật lại `update` để che lỗi.
- Lưu evidence gồm checksum SQL, log preflight, backup ID, thời gian chạy, log hậu kiểm và commit ứng dụng tương ứng.

## 4. Ví dụ áp dụng cho thay đổi mới `fanpages.rejection_reason`

1. Kiểm tra trên DB đích: `fanpages` có tồn tại không, cột `rejection_reason` đã có chưa, kiểu có là `text` không. Nếu cột đã có nhưng kiểu khác, dừng và viết migration điều chỉnh riêng.
2. Backup, chạy thử file `fanpage-rejection-reason-migration.sql` trên bản sao và xác nhận dữ liệu `fanpages` không đổi. Vì đây là `ADD COLUMN ... TEXT` nullable, không cần backfill để ứng dụng cũ tiếp tục hoạt động.
3. Ở DB đích chỉ áp dụng **file migration này** sau preflight; hậu kiểm cột, rồi triển khai BE trả/lưu lý do từ chối. Không áp dụng `schema.sql` lên DB đang chạy.
4. Tạo fanpage bị từ chối và kiểm tra lý do được lưu/đọc; fanpage cũ không có lý do vẫn đọc bình thường. Ghi lại version/checksum và kết quả.

## 5. Việc nên làm trước khi có pipeline migration tự động

Ưu tiên tắt `ddl-auto=update` ngoài local, kiểm kê drift giữa từng DB và `schema.sql`, lập lịch sử migration đã thực sự áp dụng, rồi chuẩn hóa migration mới theo version. Đặc biệt, viết lại đường chuyển đổi `student_criteria_transactions` thành migration có transaction, kiểm tra cột nguồn, backfill đối soát và không `TRUNCATE` trước khi chứng minh dữ liệu đã chuyển đủ. Chỉ sau đó mới đưa công cụ chạy migration tự động vào quy trình deploy.

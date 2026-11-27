# Kế hoạch hoàn thiện chức năng Super Admin giữa FE và BE

Ngày lập: 27/11/2026. Cập nhật: 27/11/2026 — **Đã triển khai và kiểm chứng mức unit test/build cho Task 1–6** (không dùng facets/tab counts, giữ lọc server-side + phân trang; `rejectionReason` Option A; ẩn/khôi phục sự kiện qua `PATCH /events/admin/{id}/visibility`; TopNav global search & notification dropdown; dọn mã học kỳ/prop thừa). Còn kiểm tra thủ công E2E với dữ liệu thật ở Task 7.

## 1. Mục tiêu và phạm vi

Mọi thao tác đang hiển thị trên trang Super Admin phải có hành vi thực, dữ liệu từ BE phù hợp với nhãn UI, và kết quả tìm kiếm/lọc/số đếm đúng trên **toàn bộ dữ liệu**, không chỉ trang đang tải. Phạm vi gồm tám tab `overview`, `users`, `fanpages`, `events`, `semesters`, `point-categories`, `sv5t-criterias`, `schools`, các modal của chúng và thanh điều hướng Super Admin.

Kế hoạch này chọn cách tận dụng API đã có trước, chỉ thêm API cho dữ liệu không thể suy ra đúng ở FE. Chưa xây quy trình người dùng gửi báo cáo vi phạm, hệ thống chat hỗ trợ, hay chức năng chọn học kỳ hiện hành thủ công vì UI hiện không có luồng nghiệp vụ hoàn chỉnh cho những chức năng đó.

## 2. Hiện trạng đã xác minh

| Khu vực | FE hiện tại | BE hiện tại | Kết luận |
| --- | --- | --- | --- |
| Người dùng | `SuperAdminUsersView` tìm kiếm và lọc vai trò trên mảng của trang 20 dòng; tab “Tất cả” yêu cầu `isActive=true` | `GET /users/admin` có `isActive`, `page`, `size`, sắp xếp; chưa có `keyword`/`role` | Cần mở rộng query BE và chuyển bộ lọc FE sang server |
| Sự kiện | Nút ẩn/khôi phục trong menu và modal gọi handler luôn báo chưa hỗ trợ | `PATCH /events/admin/{id}/visibility` đã có, nhận `hidden`, `reason` | Chỉ cần tích hợp FE và kiểm tra phản hồi/quyền |
| Sự kiện | UI hiển thị “Vi phạm / Ẩn”, gọi bộ lọc `VIOLATED`, có chỗ hiển thị `reportCount` | `EventStatusEnum` không có `VIOLATED`; `EventResponseDto` không có `reportCount`, nhưng có `isActive` và `violationReason` | Đổi UI thành “Đã ẩn”; loại bỏ ngụ ý có hệ thống báo cáo |
| Sự kiện | `OPEN`/`HAPPENING` được suy ra ở FE; số tab tính từ một trang dữ liệu | `GET /events/admin` có `status`, `isActive`, `keyword`, phân trang | Cần hợp đồng filter trạng thái hiển thị ở BE hoặc thay cách trình bày để bảo đảm kết quả toàn cục |
| Fanpage | Danh sách/modal mong đợi người đại diện, lý do từ chối, một số số liệu | `GET /fanpages/admin` không trả các trường đó; có API thành viên và số người theo dõi riêng; BE lưu lý do từ chối | Cần read model quản trị trả đủ dữ liệu, tránh request theo từng dòng |
| Thanh đầu trang | Ô tìm kiếm chung, nút lọc, chat và chuông hiển thị nhưng chưa có hành vi tương ứng | API tìm fanpage/sự kiện, API thông báo cá nhân đã có; thiếu tìm user toàn cục | Tích hợp tìm kiếm và chuông; bỏ nút chưa có sản phẩm thực |
| Học kỳ | Có handler “đặt hiện hành” nhưng view không có nút gọi | Có `GET /semesters/current`, chưa có API đặt thủ công | Dọn mã không dùng; không thêm API trong phạm vi này |

Các thao tác CRUD hiện đang hiển thị ở `point-categories`, `sv5t-criterias`, `schools`, `majors`, `semesters`, cùng duyệt/từ chối/khóa fanpage và khóa/phân quyền user đã có endpoint tương ứng. Task kiểm tra tích hợp vẫn phải xác minh DTO, quyền và lỗi thực tế của chúng.

## 3. Quyết định thiết kế và đánh đổi

### 3.1. “Vi phạm” và báo cáo sự kiện

**Chọn cho đợt này:** định nghĩa danh sách là **sự kiện đã bị ẩn bởi quản trị viên**, dùng `isActive=false` và `violationReason`; đổi nhãn, mô tả, số đếm, bỏ `reportCount` và trạng thái `VIOLATED` khỏi FE.

- Ưu điểm: dùng dữ liệu và API hiện có, không tạo trạng thái giả hoặc số báo cáo không có nguồn.
- Giới hạn: chưa thể tiếp nhận và xử lý báo cáo của người dùng. Nếu muốn sản phẩm có báo cáo thật, cần một kế hoạch riêng cho người báo cáo, chống gửi trùng, trạng thái xử lý, quyền xem và thông báo; không chỉ thêm enum `VIOLATED`.

### 3.2. Tìm kiếm chung

**Chọn:** FE gọi đồng thời ba API tìm kiếm đã được phân trang (user sau Task 1, fanpage, event), giới hạn số kết quả mỗi nhóm. Chỉ gọi khi người dùng nhập đủ ký tự và sau debounce; hủy request cũ khi thay từ khóa.

- Ưu điểm: không cần endpoint tổng hợp mới, tái sử dụng chính sách truy cập và tìm kiếm của từng miền.
- Giới hạn: một lượt tìm có thể phát sinh ba request. Nếu lưu lượng trở thành vấn đề, có thể bổ sung `GET /admin/search` sau khi đo thực tế.

### 3.3. Thông tin fanpage

**Chọn:** BE cung cấp response quản trị đã tổng hợp cho một trang fanpage. Quy tắc xác định “người đại diện” phải dùng một nguồn ổn định, được chốt trong DTO/test (ưu tiên người đăng ký fanpage; nếu dữ liệu cũ không còn người đó, hiển thị trống có chủ đích). Không lấy thành viên `ADMIN` bất kỳ làm đại diện theo thứ tự tình cờ của danh sách.

- Ưu điểm: FE chỉ render; tránh N+1 request và thông tin thay đổi khi phân trang.
- Giới hạn: BE cần query/projection và mapping riêng; cần kiểm tra fanpage cũ thiếu dữ liệu người đăng ký.

## 4. Hợp đồng API đích

### 4.1. Danh sách user

Mở rộng `GET /users/admin` theo hướng tương thích ngược:

```text
GET /users/admin?page=0&size=20&keyword=nghia&role=STUDENT&isActive=true
```

- `keyword`: tùy chọn, tìm không phân biệt hoa/thường trong họ tên, email, mã sinh viên/mã cán bộ; trim, giới hạn độ dài và escape ký tự đặc biệt của truy vấn.
- `role`: tùy chọn, nhận `STUDENT`, `FANPAGE_ADMIN`, `ADMIN`; thống nhất vai trò hiển thị khi một tài khoản có nhiều role.
- `isActive`: `true`, `false` hoặc **không truyền** để lấy tất cả. Không truyền phải khác với mặc định `true` hiện tại; lưu ý tương thích với các client đang gọi endpoint không truyền tham số.
- Response giữ `BaseResponse<Page<UserInfoResponseDto>>`; `totalElements` phản ánh các filter trước phân trang.
- Bổ sung API số đếm theo nhóm nếu tab cần số đếm toàn cục, ví dụ `GET /users/admin/facets?keyword=...`, với các trường `all`, `student`, `staff`, `admin`, `locked`. Định nghĩa rõ số đếm có chịu ảnh hưởng của `keyword` hay không; đề xuất có, và không chịu ảnh hưởng của tab vai trò hiện chọn.
- Chỉ tài khoản `ADMIN` được truy cập query và facets; kiểm tra ở BE, không dựa vào ẩn nút trên FE.

### 4.2. Danh sách event quản trị

Giữ `GET /events/admin` cho các filter thực thuộc BE (`status`, `isActive`, `keyword`). Bổ sung tham số `viewStatus` hoặc một filter tương đương để BE lọc đúng các nhóm hiển thị `OPEN`, `HAPPENING`, `COMPLETED`, `DRAFT`, `CANCELLED`, `HIDDEN` **trước phân trang**. Quy tắc thời gian cần xác định tại một chỗ trong BE; không để FE suy đoán khác BE. Với `HIDDEN`, lọc `isActive=false`; không gửi `status=VIOLATED`.

Nếu UI tiếp tục hiển thị số trên từng tab, trả facets từ một endpoint tổng hợp có cùng bộ lọc từ khóa, ví dụ `GET /events/admin/facets?keyword=...`. Không tính các facets từ `content` của một trang. Việc thêm endpoint là có điều kiện: nếu bỏ số trên tab thì không cần endpoint này.

API mutation hiện có:

```text
PATCH /events/admin/{id}/visibility
{ "hidden": true,  "reason": "Lý do ẩn" }
{ "hidden": false, "reason": null }
```

Sau mutation, BE trả thành công khi đã lưu; FE refetch danh sách, facets, overview và chi tiết đang mở. Không dùng `status=VIOLATED` hay thay `status` của lifecycle để biểu diễn hành động ẩn.

### 4.3. Fanpage quản trị

Giữ `GET /fanpages/admin` phân trang/filter. Trả read model quản trị có `rejectionReason`, thông tin người đại diện (`userId`, tên, email, mã, số điện thoại theo dữ liệu cho phép), `followersCount` và `eventsCount` nếu UI thực sự hiển thị các số liệu này. Cân nhắc DTO riêng như `AdminFanpageResponseDto` để không làm phình response công khai `FanpageResponseDto`. Truy xuất bổ sung theo **lô ID của một trang**, không query từng fanpage.

Giữ các endpoint duyệt/từ chối/ban/unban hiện có. Khi mở modal, ưu tiên chi tiết từ API quản trị hoặc cùng read model có đầy đủ dữ liệu; sau mutation invalidate danh sách và mục chờ duyệt.

### 4.4. Thông báo

Tận dụng `GET /notifications`, `GET /notifications/unread-count`, `PATCH /notifications/{id}/read` và `PATCH /notifications/read-all`. Số trên chuông là **thông báo chưa đọc của tài khoản**, không phải tổng fanpage chờ duyệt cộng sự kiện đã ẩn. Số chờ duyệt vẫn ở sidebar/overview theo dữ liệu quản trị riêng.

## 5. Breakdown task và kiểm tra cuối task

### Task 0 — Chốt contract và baseline

1. Lập bảng UI action → service FE → HTTP method/path → controller → request/response cho toàn bộ tám tab và các modal.
2. Chốt tên filter event và quy tắc `OPEN`/`HAPPENING`/`COMPLETED`, quy tắc người đại diện fanpage, nội dung facets user/event.
3. Ghi lại ví dụ response hiện tại và các case thiếu dữ liệu để so sánh sau refactor.

**Kiểm tra:** mỗi action nhìn thấy trên UI có một đường đi cụ thể; các điểm chưa có nghiệp vụ được gắn quyết định “bỏ khỏi UI” hoặc “phát triển riêng”.

### Task 1 — BE: tìm kiếm, lọc và facets user

1. Sửa request/controller trong `ddd-api/.../controller/UserController.java` và DTO filter nếu cần; kiểm tra quyền `ADMIN`.
2. Mở rộng `AdminUserQueryService` ở `ddd-application`, dùng một filter object thay vì thêm nhiều tham số rời; dùng `ServiceUtil.createPageable`.
3. Thêm query/specification ở `ddd-infrastructure` cho keyword, role, tri-state `isActive`; giữ domain repository port nếu cần truy vấn mới. Tránh nhân bản dòng vì join role/profile và tránh N+1 profile.
4. Trả facets cùng định nghĩa role với truy vấn danh sách; mapping qua `UserInfoApiMapper`, `BaseResponse`.

**Kiểm tra:** test query với dữ liệu nằm trang sau, user nhiều role, không có profile, khóa/mở khóa, keyword hoa/thường và tổ hợp filter; controller test quyền và response phân trang. Không truyền filter phải giữ hành vi tương thích đã thống nhất ở Task 0.

### Task 2 — FE: danh sách user

1. Sửa `SuperAdminPage`, `superAdminService`, `SuperAdminUsersView` để query key chứa `page`, `keyword`, `role`, `isActive`; debounce từ khóa và reset về trang 0 khi filter đổi.
2. Tab “Tất cả” không truyền `isActive`; tab “Bị khóa” truyền `false`; tab role truyền role đúng contract.
3. Hiển thị số tab từ facets BE, không đếm `users.length`; giữ trạng thái đang tải/lỗi rõ ràng.

**Kiểm tra:** test người dùng ở trang 2 vẫn tìm ra; tab “Tất cả” chứa active và locked; số tab không đổi sai khi chuyển trang; mutation khóa/phân quyền refetch danh sách và facets.

### Task 3 — FE/BE: kiểm duyệt và lọc event

1. FE nối hai nút ẩn/khôi phục với visibility API; yêu cầu lý do khi ẩn; chặn gửi lặp; xử lý lỗi và đóng modal sau khi thành công.
2. FE đổi nhãn “Vi phạm / Ẩn” thành “Đã ẩn”, bỏ trường/badge `reportCount`, bỏ `VIOLATED` khỏi filter và type dành riêng cho trang Super Admin.
3. BE triển khai filter `viewStatus`/facets nếu Task 0 xác nhận giữ các tab trạng thái và số đếm. Đặt quy tắc thời gian trong query service/specification; tránh tải một trang rồi lọc ở FE.
4. FE dùng `totalElements`/facets từ BE, loại bỏ lọc lại `content` làm rỗng trang sai.

**Kiểm tra:** test ẩn/khôi phục với lý do, quyền ADMIN, reload; test mỗi trạng thái với nhiều trang và thời điểm biên; tổng số tab khớp query BE. Không còn request `status=VIOLATED`.

### Task 4 — BE/FE: dữ liệu fanpage quản trị

1. Thêm DTO quản trị trong `ddd-api/.../dto/fanpage/res`, mapper trong `ddd-api/.../mapper`, read model/query trong `ddd-application/.../service/fanpage`.
2. Lấy dữ liệu người đại diện, lý do từ chối và số liệu theo lô trong `ddd-infrastructure`; không thay đổi API fanpage công khai nếu không cần.
3. FE sửa `getFanpagesPage` và modal để dùng response quản trị; loại fallback tự phỏng đoán người đại diện từ thành viên không ổn định.

**Kiểm tra:** fanpage pending/active/rejected/banned, dữ liệu thiếu người đăng ký, lý do từ chối sau reload; số request không tăng theo số dòng của trang; quyền xem trường quản trị.

### Task 5 — FE: tìm kiếm chung, thông báo và nút chưa có hành vi

1. Tạo kết quả tìm kiếm chung theo ba nhóm từ các API hiện có; debounce, ngưỡng ký tự, hủy request cũ; kết quả điều hướng tới đúng tab/bản ghi.
2. Nối chuông với notification API, trạng thái mở/đóng, đánh dấu đã đọc, số chưa đọc từ BE.
3. Gỡ hoặc ẩn nút “Tin nhắn & Hỗ trợ” và nút lọc trên header nếu chưa có panel/đích đến thực. Nếu cần giữ bộ lọc, dùng panel điều khiển các filter đã có thay vì nút trống.

**Kiểm tra:** tìm được user/fanpage/event ngoài trang hiện tại; không hiển thị kết quả của request cũ khi gõ nhanh; chuông cập nhật sau đọc; không còn nút bấm không phản hồi.

### Task 6 — Dọn mã học kỳ và rà soát các tab còn lại

1. Bỏ handler `handleSetCurrentSemester` không được gọi và prop không dùng; giữ các API tạo/sửa/xóa học kỳ hiện có.
2. Rà lại DTO và lỗi mutation của `point-categories`, `sv5t-criterias`, `schools`, `majors`, `fanpages` và `overview`; chỉ sửa mismatch đã xác nhận, không mở rộng tính năng mới.
3. Đồng nhất cách invalidation query sau thay đổi để overview và các tab phản ánh trạng thái BE.

**Kiểm tra:** smoke test từng tab/modal, tạo/sửa/trạng thái/xóa theo quyền; không có cảnh báo “API chưa hỗ trợ” cho thao tác đang hiển thị.

### Task 7 — Kiểm tra tích hợp và bàn giao local

1. Chạy test BE theo module liên quan, test FE cho service/view/page và build/typecheck.
2. Kiểm tra thủ công bằng dữ liệu có hơn một trang, gồm active/locked user, fanpage bị từ chối, event đã ẩn, thông báo chưa đọc.
3. Kiểm tra Network: method/path/body/response, quyền 401/403, số request, phân trang, số đếm và refresh sau mutation.
4. Ghi lại API contract cuối cùng, thay đổi hành vi UI và các chức năng cố ý chưa hỗ trợ.

**Điều kiện hoàn thành:** không còn chức năng nhìn như đã hoạt động nhưng không có hành vi, không còn số liệu chỉ tính từ trang đang tải, không có dữ liệu giả trong modal, và mọi thao tác lưu đều phản ánh dữ liệu BE sau tải lại trang.

## 6. Thứ tự triển khai và ranh giới

Task 0 → 1 → 2; Task 3 và 4 có thể thực hiện độc lập sau Task 0; Task 5 phụ thuộc Task 1; Task 6 sau các thay đổi chức năng; Task 7 cuối cùng. Mỗi task phải qua phần kiểm tra riêng trước khi chuyển task tiếp theo.

Không thêm `VIOLATED` vào enum lifecycle chỉ để khớp giao diện. Không tính số đếm tab từ `Page.content`. Không đưa JPA entity vào controller. Logic truy vấn và quyền ở application/query service, DTO API ở `ddd-api`, repository/specification ở `ddd-infrastructure`, domain port ở `ddd-domain` khi cần. Dùng `BaseResponse`, `SecurityContextPort`, domain exceptions và `ServiceUtil` theo `architecture_conventions.md`.

## 7. Tài liệu mã nguồn đối chiếu

- FE: `event_management_frontend/src/pages/SuperAdminPage.tsx`, `src/features/super-admin/services/superAdminService.ts`, các `SuperAdmin*View.tsx`, `AdminStatisticsDashboard.tsx`, `SuperAdminTopNav.tsx`, `EventModerationModal.tsx`.
- BE: `ddd-api/src/main/java/com/ddd/api/controller/{User,Event,Fanpage,Notification,Semester,PointCategory,Criteria,School,Major,AdminStatistic}Controller.java` và các DTO/mapper/service/repository liên quan.
- Quy tắc dự án: `.agents/rules/agent-rules.md`, `architecture_conventions.md`.

## 8. Điểm cần duyệt trước khi triển khai

1. Đồng ý đổi “Vi phạm / Ẩn” thành “Đã ẩn” và chưa làm quy trình báo cáo vi phạm trong đợt này.
2. Đồng ý tìm kiếm chung bằng ba API hiện có, không tạo API tổng hợp mới.
3. Chốt quy tắc người đại diện fanpage và định nghĩa trạng thái hiển thị `OPEN`/`HAPPENING` theo nghiệp vụ sự kiện hiện tại trong Task 0.

Theo quy tắc Phase 2 trong `.agents`, tài liệu này là kế hoạch để review; việc viết mã chức năng chỉ bắt đầu sau khi plan được phê duyệt.

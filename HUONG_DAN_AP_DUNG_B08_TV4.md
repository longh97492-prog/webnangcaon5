# B08 — code bảo mật và kiểm thử V4 (không phải báo cáo)

Gói này được đối chiếu với source nhóm lưu trong `webnangcaon5-1810.zip`, các bản bổ sung B07 và 5 bài test B08 đã gửi trước đó. Chỉ thay mã liên quan B08; **không sửa** `TransferController`, `TransferService`, `ImportRequest`, `FileUploadController` hay `FileUploadService`.

## Áp dụng an toàn từ PowerShell (ở thư mục gốc dự án)

**Không giải nén đè thẳng lên source.** Giải nén ZIP vào thư mục tạm rồi kiểm tra và áp dụng patch; `git apply --ignore-whitespace --check` sẽ dừng nếu file đã thay đổi không tương thích:

```powershell
$dest = Join-Path $env:TEMP 'B08_V4_TV4_20261018'
Expand-Archive -LiteralPath "$env:USERPROFILE\Downloads\B08_V4_TV4_CODE_20261018.zip" -DestinationPath $dest -Force
$patch = Join-Path $dest 'B08_V4_TV4.patch'
git status --short
git apply --ignore-whitespace --check $patch
git apply --ignore-whitespace $patch
```

Nếu `git apply --ignore-whitespace --check` báo lỗi, **không ép ghi đè**. Cần đối chiếu code hiện có với file trong ZIP (vì nhánh của bạn có thể đã được nhóm cập nhật).

## Kiểm tra

```powershell
mvn '-Dtest=B08V4RiskVerificationTest,B08ProductionHttpsTest' test
mvn clean test
powershell -ExecutionPolicy Bypass -File .\scripts\check-b08-git-history.ps1 -ShowMatches
```

Kiểm chứng trên miền HTTPS nhóm sau khi V5 triển khai:

```powershell
powershell -ExecutionPolicy Bypass -File .\scripts\test-b08-public-headers.ps1 -BaseUrl 'https://TEN-MIEN-CUA-NHOM'
```

Các script **không tự chụp ảnh** và **không tự khẳng định 12/12 đã kiểm chứng**. Người thực hiện chụp minh chứng Ảnh 16–26 khi chạy test và thử trên môi trường thật.

## Thay đổi chính

- `SecurityConfig`: cho phép lấy CSRF token trước khi đăng nhập; bắt buộc HTTPS khi `app.security.require-https=true`.
- `CsrfTokenController`: cấp CSRF header+token để dùng trong phiên hiện tại.
- `audit-logs.html`/`.js`/`.css`: chuyển script và CSS ngoài trang cho tương thích CSP; bổ sung CSRF cho login/logout; giữ chống XSS bằng `textContent`.
- `application-prod.properties`: tắt lỗi chi tiết/H2 console, bật secure session cookie, require HTTPS.
- `B08V4RiskVerificationTest`: 16 ca mới; `B08ProductionHttpsTest`: 2 ca. **Chưa chạy Maven trong môi trường tạo gói**; chỉ kiểm tra cú pháp Java 21 và JavaScript.
- `check-b08-git-history.ps1`: rà toàn bộ commit (`git rev-list --all`) theo mẫu khả nghi mà không in giá trị bí mật; cần rà soát thủ công vì có thể báo sai hoặc bỏ sót.
- `test-b08-public-headers.ps1`: kiểm tra 5 response headers trên tên miền HTTPS thực.
- `verification/B08_BM1_BM12_MATRIX.csv`: danh mục mã nguồn, test, phần còn phải xác minh trên môi trường thực.

Lưu ý: `app.security.require-https=true` chỉ dùng **sau khi cấu hình reverse proxy HTTPS tin cậy**. Proxy phải loại bỏ forwarded headers do client gửi trước khi cung cấp header tin cậy; nếu không sẽ có rủi ro giả mạo scheme hoặc vòng lặp redirect. Tài khoản/role, token và ảnh bằng chứng không được đưa lên GitHub.

## Commit/push sau khi cả suite PASS

```powershell
git status --short
git diff --check
git add src/main/java/com/example/supplychain/config/SecurityConfig.java src/main/java/com/example/supplychain/controller/CsrfTokenController.java src/main/resources/application-prod.properties src/main/resources/static/audit-logs.html src/main/resources/static/audit-logs.css src/main/resources/static/audit-logs.js src/test/java/com/example/supplychain/B08V4RiskVerificationTest.java src/test/java/com/example/supplychain/B08ProductionHttpsTest.java scripts/check-b08-git-history.ps1 scripts/test-b08-public-headers.ps1 scripts/run-b08-v4.ps1 verification/B08_BM1_BM12_MATRIX.csv HUONG_DAN_AP_DUNG_B08_TV4.md
git diff --cached --stat
git commit -m 'test(security): verify B08 BM1-BM12 and secure audit UI'
git push group feature/b08-tv4
```

B08 vẫn cần kiểm chứng thực tế BM1–BM12 (đặc biệt BM4 ma trận đầy đủ, BM11 history, BM12 deploy) để được xem là hoàn tất theo sổ tay. Không đánh dấu PASS các mục này nếu chưa có kết quả.

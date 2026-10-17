GOI GIAI QUYET 3 XUNG DOT MERGE - B08 V4

1. Goi nay gom ba file Java da ghep tu OURS va GROUP/develop.
   - SecurityConfig: giu CSRF, security headers va cac quyen ADMIN; cho phep GET /api/csrf (neu endpoint B08 duoc khoi phuc).
   - AuthController: giu xu ly dang nhap, chong session fixation, ghi audit va hash mat khau.
   - GlobalExceptionHandler: giu 400/404/409/422/500; Request ID va logging tu code nhom; khong trung handler.

2. QUAN TRONG: GROUP_ErrorResponse.java trong ZIP dau vao rong 0 byte.
   Giu ErrorResponse.java lay tu group/develop, KHONG thay bang file rong.
   GlobalExceptionHandler su dung constructor 5 tham so nhu code group/develop:
   ErrorResponse(code, message, time, path, requestId).

3. CACH AP DUNG (PowerShell o thu muc du an):
   git status --short
   git fetch group --prune
   git merge group/develop
   Khi merge dung tai 3 conflict, mo thu muc ZIP nay va copy de ba file
   trong src/main/java/com/example/supplychain/... vao thu muc du an
   (giu nguyen duong dan).
   git add src/main/java/com/example/supplychain/config/SecurityConfig.java src/main/java/com/example/supplychain/controller/AuthController.java src/main/java/com/example/supplychain/exception/GlobalExceptionHandler.java
   git diff --cached --check
   git commit -m "Merge group develop into B08 TV4 and resolve security conflicts"

4. SAU KHI MERGE THANH CONG:
   git stash list
   git stash apply 'stash@{0}'
   Neu co conflict: DUNG VA GUI git status --short, KHONG xoa stash.
   Neu thanh cong: mvn clean test
   Khong push khi Maven chua PASS.

5. Goi nay chua duoc chay Maven tren may cua ban; phai xac nhan bang build/test.

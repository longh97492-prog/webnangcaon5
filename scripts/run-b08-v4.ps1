$ErrorActionPreference = 'Stop'
if (-not (Test-Path 'pom.xml')) { throw 'Hay chay trong thu muc goc Maven project.' }
Write-Host 'B08 V4: chay test doi tuong, CSRF, upload, login, HTTPS, headers.'
& mvn '-Dtest=B08V4RiskVerificationTest,B08ProductionHttpsTest' test
if ($LASTEXITCODE -ne 0) { throw 'B08 tests FAILED; khong push.' }
Write-Host 'B08 test PASS. Tiep tuc chay toan bo Maven tests.'
& mvn clean test
if ($LASTEXITCODE -ne 0) { throw 'Full Maven tests FAILED; khong push.' }
Write-Host 'Maven tests PASS. Chua du dieu kien khang dinh BM11/BM12 tren production.'

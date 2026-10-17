$ErrorActionPreference = 'Stop'
if (-not (Test-Path '.git')) { throw 'Chay script tu thu muc goc repository.' }
Write-Host 'B08 - Repo safety checklist (chi hien TEN FILE, khong in gia tri bi mat)'
Write-Host "Branch: $(git branch --show-current)"
Write-Host 'Tracked files with potentially sensitive names:'
$names = git ls-files
$matches = @($names | Where-Object {
    $_ -match '(^|/)(\.env($|\.)|.*\.pem$|.*\.key$|id_rsa$|.*credentials.*|.*secret.*|.*\.p12$|.*\.jks$)'
})
if ($matches.Count -eq 0) { Write-Host 'Khong phat hien ten file nhay cam theo quy tac.' }
else { $matches | ForEach-Object { Write-Warning $_ } }
Write-Host 'Tracked files with possible hardcoded credentials (review locally; no values printed):'
$patterns = '(?i)(api[_-]?key|client[_-]?secret|private[_-]?key|password\s*[:=]\s*[^\s$\{])'
$found = @()
foreach ($path in $names) {
    if ($path -match '\.(java|properties|ya?ml|json|xml|env|txt)$' -and (Test-Path -LiteralPath $path)) {
        if (Select-String -LiteralPath $path -Pattern $patterns -Quiet) { $found += $path }
    }
}
$found | Sort-Object -Unique | ForEach-Object { Write-Host "REVIEW: $_" }
Write-Host 'Kiem tra lich su git/HTTPS/headers tren moi truong deploy la buoc RIENG, script nay KHONG chung minh BM11/BM12 hoan thanh.'

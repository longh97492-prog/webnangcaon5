param(
    [switch]$ShowMatches
)
$ErrorActionPreference = 'Stop'
if (-not (Test-Path '.git')) {
    throw 'Hay chay script tai thu muc goc Git repository.'
}

$branch = git branch --show-current
$commits = @(git rev-list --all)
if ($LASTEXITCODE -ne 0) { throw 'Khong doc duoc lich su git.' }
if ($commits.Count -eq 0) { throw 'Git history rong, khong the xac minh BM11.' }
Write-Host ('BM11: branch={0}, commits={1}' -f $branch, $commits.Count)

# Heuristic only. This is intentionally not an assertion that secrets are absent.
$secretNames = '(?i)(^|/)(\.env($|\.)|id_rsa$|id_ed25519$|[^/]*\.(pem|p12|pfx|jks|key)$|[^/]*(credentials|secrets)[^/]*)'
$credentialLine = '(api[_-]?key|client[_-]?secret|access[_-]?token|secret[_-]?key|password)[[:space:]]*[:=][[:space:]]*["''][^"''][^"'']{5,}|-----BEGIN[[:space:]]+([A-Z]+[[:space:]]+)?PRIVATE[[:space:]]+KEY-----'

$flaggedPaths = New-Object 'System.Collections.Generic.HashSet[string]'
$examples = New-Object 'System.Collections.Generic.List[string]'
$scanned = 0
foreach ($commit in $commits) {
    $commit = $commit.Trim()
    if ($commit.Length -eq 0) { continue }
    $scanned++
    $paths = @(git ls-tree -r --name-only $commit)
    if ($LASTEXITCODE -ne 0) { throw "git ls-tree failed at $commit" }
    foreach ($p in $paths) {
        if ($p -match $secretNames) {
            $null = $flaggedPaths.Add($p)
            if ($examples.Count -lt 20) { $examples.Add("FILE: $p") }
        }
    }

    # git grep -l prints only commit:path, not credential values.
    $matchedFiles = @(git grep -I -i -l -E -- $credentialLine $commit -- 2>$null)
    if ($LASTEXITCODE -ne 0 -and $LASTEXITCODE -ne 1) {
        throw "git grep failed at $commit with exit code $LASTEXITCODE"
    }
    foreach ($match in $matchedFiles) {
        if (-not [string]::IsNullOrWhiteSpace($match)) {
            $path = $match -replace '^[0-9a-fA-F]{40}:', ''
            $null = $flaggedPaths.Add($path)
            if ($examples.Count -lt 20) { $examples.Add("CONTENT: $path") }
        }
    }
}

Write-Host ('Da quet {0} commits. So duong dan can xem xet: {1}' -f $scanned, $flaggedPaths.Count)
if ($flaggedPaths.Count -gt 0) {
    if ($ShowMatches) {
        $examples | Select-Object -Unique | ForEach-Object { Write-Warning $_ }
    } else {
        Write-Warning 'Co ket qua can kiem tra. Dung -ShowMatches de xem TEN FILE, khong in gia tri bi mat.'
    }
    Write-Warning 'Khong duoc ket luan BM11 PASS. Neu ro ri that: thu hoi/doi khoa, phoi hop nhom lam sach history.'
    exit 2
}
Write-Host 'Khong phat hien dau hieu theo bo quy tac heuristic; VAN CAN review thu cong va GitHub secret scanning.'
exit 0

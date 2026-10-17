param(
    [Parameter(Mandatory = $true)]
    [string]$BaseUrl
)
$ErrorActionPreference = 'Stop'
$uri = [Uri]$BaseUrl
if ($uri.Scheme -ne 'https') {
    throw 'BaseUrl phai bat dau bang https:// va la domain production cua nhom.'
}
$target = '{0}://{1}{2}/api/test/protected' -f $uri.Scheme, $uri.Authority, $uri.AbsolutePath.TrimEnd('/')

# GET without credentials should be 401 but still include Spring Security headers.
try {
    $response = Invoke-WebRequest -Uri $target -Method Get -UseBasicParsing -MaximumRedirection 0 -ErrorAction Stop
    $headers = $response.Headers
    $status = [int]$response.StatusCode
} catch {
    $resp = $_.Exception.Response
    if ($null -eq $resp) { throw }
    $headers = $resp.Headers
    $status = [int]$resp.StatusCode
}
Write-Host "GET $target => HTTP $status"
$required = @(
    'Content-Security-Policy',
    'X-Content-Type-Options',
    'X-Frame-Options',
    'Referrer-Policy',
    'Strict-Transport-Security'
)
$missing = @()
foreach ($name in $required) {
    $value = $headers[$name]
    if ([string]::IsNullOrWhiteSpace([string]$value)) {
        Write-Warning "MISSING $name"
        $missing += $name
    } else {
        Write-Host "OK $name"
    }
}
if ($missing.Count -gt 0) {
    throw ('BM12 FAIL: missing {0} required headers.' -f $missing.Count)
}
Write-Host 'BM12 header check PASS cho HTTPS endpoint. Can kiem tra redirect HTTP->HTTPS va cookie Secure bang browser.'

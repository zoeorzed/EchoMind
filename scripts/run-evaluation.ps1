param(
    [string]$BaseUrl = "http://localhost:8080",
    [string]$Dataset = "evaluation/eval-dataset.json",
    [string]$Output = "target/evaluation-report.json"
)

$body = Get-Content -Raw -LiteralPath $Dataset
$report = Invoke-RestMethod -Method Post -Uri "$BaseUrl/eval/run" -ContentType "application/json; charset=utf-8" -Body ([Text.Encoding]::UTF8.GetBytes($body))
$outputDirectory = Split-Path -Parent $Output
if ($outputDirectory) {
    New-Item -ItemType Directory -Force -Path $outputDirectory | Out-Null
}
$report | ConvertTo-Json -Depth 30 | Set-Content -Encoding utf8 -LiteralPath $Output
$report | Select-Object total, passed, pass_rate, avg_scores, regressions | Format-List
Write-Host "Full report: $Output"

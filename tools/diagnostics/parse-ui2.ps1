$projectRoot = Resolve-Path (Join-Path $PSScriptRoot '..\..')
$content = Get-Content (Join-Path $projectRoot 'research\device-captures\ui\ui_mine.xml') -Raw -Encoding UTF8
$matches = [regex]::Matches($content, 'resource-id="[^"]*setting[^"]*"[^>]*bounds="([^"]*)"')
foreach ($m in $matches) {
    Write-Host "bounds=$($m.Groups[1].Value)"
}
Write-Host "---content-desc setting---"
$matches2 = [regex]::Matches($content, 'content-desc="[^"]*[Ss]etting[^"]*"[^>]*bounds="([^"]*)"')
foreach ($m in $matches2) {
    Write-Host "bounds=$($m.Groups[1].Value)"
}

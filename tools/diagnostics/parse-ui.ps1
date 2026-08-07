$projectRoot = Resolve-Path (Join-Path $PSScriptRoot '..\..')
[xml]$doc = Get-Content (Join-Path $projectRoot 'research\device-captures\ui\ui_main.xml') -Raw
$nodes = $doc.SelectNodes("//node[@text='我的']")
foreach ($n in $nodes) {
    Write-Host "text=$($n.text) bounds=$($n.bounds) rid=$($n.'resource-id')"
}

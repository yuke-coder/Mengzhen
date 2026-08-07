param([string]$file)
[Console]::OutputEncoding = [System.Text.Encoding]::UTF8
$content = [System.IO.File]::ReadAllText($file, [System.Text.Encoding]::UTF8)
$pattern = 'text="([^"]+)"[^>]*?bounds="(\[\d+,\d+\]\[\d+,\d+\])"'
$matches = [regex]::Matches($content, $pattern)
$seen = @{}
$count = 0
foreach ($m in $matches) {
    $text = $m.Groups[1].Value
    $bounds = $m.Groups[2].Value
    if ($text -and -not $seen.ContainsKey($text)) {
        $seen[$text] = $true
        $count++
        Write-Host "$count. $text | $bounds"
    }
}
Write-Host "Unique: $count"

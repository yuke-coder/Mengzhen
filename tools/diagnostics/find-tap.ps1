param([string]$file, [string]$findText)
[Console]::OutputEncoding = [System.Text.Encoding]::UTF8
$content = [System.IO.File]::ReadAllText($file, [System.Text.Encoding]::UTF8)
# 找到指定文本节点的 bounds
$pattern = 'text="' + [regex]::Escape($findText) + '"[^>]*?bounds="(\[(\d+),(\d+)\]\[(\d+),(\d+)\])"'
$m = [regex]::Match($content, $pattern)
if ($m.Success) {
    $x1 = [int]$m.Groups[2].Value
    $y1 = [int]$m.Groups[3].Value
    $x2 = [int]$m.Groups[4].Value
    $y2 = [int]$m.Groups[5].Value
    $cx = [math]::Floor(($x1 + $x2) / 2)
    $cy = [math]::Floor(($y1 + $y2) / 2)
    Write-Host "FOUND: $findText bounds=$($m.Groups[1].Value) center=($cx,$cy)"
} else {
    Write-Host "NOTFOUND: $findText"
}

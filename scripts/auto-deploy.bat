@echo off
cls

echo.
echo   Auto Deploy Service Started
echo   --------------------------
echo   Watching for code changes...
echo   Service runs until terminal is closed
echo.

powershell -Command "$host.UI.RawUI.FlushInputBuffer(); while(1) { git diff --quiet; if(-not $?) { $date = Get-Date -Format 'yyyy-MM-dd HH:mm:ss'; git add .; git commit -m ('Auto deploy ' + $date); git push origin main; git push gitee main; Write-Host 'Deploy completed!' }; Start-Sleep 3 }"
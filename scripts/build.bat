@echo off
chcp 65001 >nul

:: 构建前自动更新 Service Worker 缓存版本号
set SW_FILE=public\sw.js

:: 获取时间戳
for /f "tokens=2 delims==" %%a in ('wmic os get localdatetime /value') do set datetime=%%a
set TIMESTAMP=%datetime:~0,8%%datetime:~8,6%

echo Updating SW cache version...
powershell -Command "(Get-Content '%SW_FILE%') -replace 'const CACHE_VERSION = ''v[0-9]*''', 'const CACHE_VERSION = ''v%TIMESTAMP%''' | Set-Content '%SW_FILE%'"
echo ✓ SW cache version updated to v%TIMESTAMP%

:: 执行 Next.js 构建并导出静态文件
echo Building Next.js...
npx next build
echo Exporting static files...
npx next export -o out
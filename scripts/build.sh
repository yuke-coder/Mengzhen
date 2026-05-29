#!/bin/bash
# 构建前自动更新 Service Worker 缓存版本号
# 确保每次部署后用户刷新即可获取最新版本

SW_FILE="public/sw.js"
TIMESTAMP=$(date +%s)

if [ -f "$SW_FILE" ]; then
  # 替换缓存版本号为当前时间戳
  sed -i "s/const CACHE_VERSION = 'v[0-9]*'/const CACHE_VERSION = 'v${TIMESTAMP}'/" "$SW_FILE"
  echo "✓ SW cache version updated to v${TIMESTAMP}"
else
  echo "⚠ sw.js not found at $SW_FILE"
fi

# 执行 Next.js 构建
npx next build

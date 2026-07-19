import type { CapacitorConfig } from '@capacitor/cli';

const config: CapacitorConfig = {
  appId: 'com.mengzhen.app',
  appName: '梦枕',
  webDir: 'out',
  server: {
    androidScheme: 'https',
    // 使用部署的 URL，WebView 加载远程页面
    // 原生插件接管定时播放
    url: process.env.NODE_ENV === 'production'
      ? 'https://mengzhen.vercel.app'
      : 'http://10.0.2.2:5000',
  },
};

export default config;

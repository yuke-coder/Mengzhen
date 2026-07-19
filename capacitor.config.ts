import type { CapacitorConfig } from '@capacitor/cli';

const config: CapacitorConfig = {
  appId: 'com.mengzhen.app',
  appName: '梦枕',
  webDir: 'out',
  server: {
    androidScheme: 'https',
    // WebView 加载部署的 URL，原生插件接管定时播放
    url: 'https://mengzhen-chi.vercel.app',
  },
};

export default config;

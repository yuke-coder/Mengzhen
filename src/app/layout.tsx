import type { Metadata, Viewport } from "next";
import Script from "next/script";
import "./globals.css";
import { ThemeProvider } from "@/lib/theme-context";
import { AuthProvider } from "@/lib/auth-context";
import Navbar from "@/components/navbar";
import { Toaster } from "@/components/sonner";
import ClientProviders from "@/components/client-providers";
import { ProfileToastListener } from "@/components/profile-toast-listener";

export const viewport: Viewport = {
  themeColor: [
    { media: '(prefers-color-scheme: light)', color: '#ffffff' },
    { media: '(prefers-color-scheme: dark)', color: '#0a0f1a' },
  ],
};

export const metadata: Metadata = {
  title: "梦枕",
  description: "专为浅眠人群设计的睡眠音频播放器，支持自定义定时、淡入淡出、全自动运行",
  keywords: ["助眠", "睡眠", "白噪音", "定时播放", "音频", "梦枕", "免登录"],
  authors: [{ name: "梦枕" }],
  icons: {
    icon: "/favicon.png",
    apple: "/favicon.png",
  },
  manifest: "/manifest.json",
  appleWebApp: {
    capable: true,
    statusBarStyle: "black-translucent",
    title: "梦枕",
  },
  openGraph: {
    title: "梦枕 - 睡眠音频播放器",
    description: "专为浅眠人群设计的睡眠音频播放器，支持自定义定时、淡入淡出、全自动运行",
    type: "website",
  },
  other: {
    "dns-prefetch": "https://br-epic-clam-5a2fd709.supabase2.aidap-global.cn-beijing.volces.com",
  },
};

const THEME_INJECTION_SCRIPT = `(function(){try{var d=document.documentElement,b=document.body,t=localStorage.getItem('theme-mode')||'auto';if(t==='system')t='auto';var r=t==='auto'?(window.matchMedia('(prefers-color-scheme: dark)').matches?'dark':'light'):t;if(r==='dark'){d.classList.add('dark');if(b)b.setAttribute('theme-mode','dark')}}catch(e){}})()`;
const PWA_INSTALL_CAPTURE_SCRIPT = `(function(){if(window.__dreamPwaCaptureReady)return;window.__dreamPwaCaptureReady=true;window.addEventListener("beforeinstallprompt",function(event){event.preventDefault();window.__dreamPwaInstallPrompt=event;window.dispatchEvent(new Event("dream:pwa-install-prompt-available"))});window.addEventListener("appinstalled",function(){window.__dreamPwaInstallPrompt=null;window.__dreamPwaAppInstalled=true;window.dispatchEvent(new Event("dream:pwa-app-installed"))})})()`;
const PWA_SCRIPT = process.env.NODE_ENV === "production"
  ? `if("serviceWorker"in navigator){window.addEventListener("load",function(){navigator.serviceWorker.register("/sw.js",{updateViaCache:"none"}).catch(function(error){console.warn("PWA service worker registration failed:",error)})})}`
  : `window.addEventListener("load",function(){if("serviceWorker"in navigator){navigator.serviceWorker.getRegistrations().then(function(registrations){registrations.forEach(function(registration){registration.unregister()})})}if("caches"in window){caches.keys().then(function(keys){keys.forEach(function(key){caches.delete(key)})})}})`;

export default function RootLayout({
  children,
}: Readonly<{
  children: React.ReactNode;
}>) {
  return (
    <html lang="zh-CN" suppressHydrationWarning>
      <body suppressHydrationWarning>
        <Script
          id="theme-injection"
          strategy="beforeInteractive"
        >{THEME_INJECTION_SCRIPT}</Script>
        <Script
          id="pwa-install-capture"
          strategy="beforeInteractive"
        >{PWA_INSTALL_CAPTURE_SCRIPT}</Script>

        {/* 全局导航栏 + 页面内容 */}
        <ThemeProvider>
          <AuthProvider>
            <ClientProviders>
              {/* 导航栏：position: fixed + z-index 确保在所有页面内容之上 */}
              <Navbar />

              <div className="pt-12 sm:pt-14">
                {children}
                <ProfileToastListener />
              </div>
            </ClientProviders>
            <Toaster position="top-right" />
          </AuthProvider>
        </ThemeProvider>

        <Script id="pwa" strategy="afterInteractive">{PWA_SCRIPT}</Script>

      </body>
    </html>
  );
}

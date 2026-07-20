"use client";

import { useCallback, useEffect, useState, useSyncExternalStore } from "react";
import Image from "next/image";
import { cn } from "@/lib/utils";
import {
  getPwaInstallServerStatus,
  getPwaInstallStatus,
  subscribePwaInstallStatus,
} from "@/lib/pwa";
import {
  Drawer,
  DrawerClose,
  DrawerContent,
  DrawerDescription,
  DrawerFooter,
  DrawerHeader,
  DrawerTitle,
} from "@/components/ui/drawer";
import "./install-button.css";

interface InstallButtonProps {
  className?: string;
}

type InstallPlatform = "android" | "ios" | "desktop";

const installGuides: Record<
  InstallPlatform,
  {
    description: string;
    steps: Array<{ title: string; detail: string }>;
    note: string;
  }
> = {
  android: {
    description: "当前浏览器没有向网页提供一键安装入口，请从浏览器菜单完成安装。",
    steps: [
      {
        title: "打开浏览器菜单",
        detail: "点击页面底部或右上角的菜单按钮，图标通常是“⋮”或“≡”。",
      },
      {
        title: "选择安装选项",
        detail: "点击“安装应用”“添加到主屏幕”或“添加至桌面”。",
      },
      {
        title: "确认安装",
        detail: "完成后即可从手机桌面直接打开梦枕。",
      },
    ],
    note: "如果菜单中没有安装选项，请刷新一次；仍未出现时，可用 Chrome、Edge 或系统浏览器重新打开。",
  },
  ios: {
    description: "iPhone 和 iPad 需要通过 Safari 的分享菜单添加到主屏幕。",
    steps: [
      {
        title: "使用 Safari 打开",
        detail: "复制当前网址，并在 Safari 中访问梦枕。",
      },
      {
        title: "打开分享菜单",
        detail: "点击 Safari 工具栏中的分享按钮。",
      },
      {
        title: "添加到主屏幕",
        detail: "选择“添加到主屏幕”，然后点击“添加”。",
      },
    ],
    note: "安装后请从主屏幕图标进入，梦枕会以独立应用方式运行。",
  },
  desktop: {
    description: "当前浏览器没有向网页提供一键安装入口，可以从浏览器界面完成安装。",
    steps: [
      {
        title: "查看地址栏或菜单",
        detail: "寻找地址栏右侧的安装图标，或打开浏览器主菜单。",
      },
      {
        title: "选择安装应用",
        detail: "点击“安装梦枕”“安装应用”或“创建快捷方式”。",
      },
      {
        title: "确认安装",
        detail: "安装完成后可从桌面或应用列表启动梦枕。",
      },
    ],
    note: "如果没有安装选项，请使用最新版 Chrome 或 Edge 打开。",
  },
};

function InstallGuide({
  open,
  onOpenChange,
  platform,
}: {
  open: boolean;
  onOpenChange: (open: boolean) => void;
  platform: InstallPlatform;
}) {
  const guide = installGuides[platform];

  return (
    <Drawer open={open} onOpenChange={onOpenChange}>
      <DrawerContent
        className="!border-0 rounded-t-2xl bg-background text-foreground shadow-[0_-8px_24px_rgba(0,0,0,0.22)]"
        overlayClassName="!bg-black/55 backdrop-blur-[2px]"
      >
        <div className="mx-auto w-full max-w-lg px-2 pb-[env(safe-area-inset-bottom)]">
          <DrawerHeader className="px-5 pb-3 pt-5 text-left">
            <DrawerTitle className="text-xl font-semibold">将梦枕安装到桌面</DrawerTitle>
            <DrawerDescription className="mt-2 text-left text-sm leading-6 text-muted-foreground">
              {guide.description}
            </DrawerDescription>
          </DrawerHeader>

          <ol className="space-y-4 px-5 py-2">
            {guide.steps.map((step, index) => (
              <li key={step.title} className="flex gap-3">
                <span
                  className="flex size-7 shrink-0 items-center justify-center rounded-full bg-[rgb(40,184,148)] text-sm font-bold text-black"
                  aria-hidden="true"
                >
                  {index + 1}
                </span>
                <div className="min-w-0 pt-0.5">
                  <p className="text-sm font-semibold text-foreground">{step.title}</p>
                  <p className="mt-1 text-sm leading-6 text-muted-foreground">{step.detail}</p>
                </div>
              </li>
            ))}
          </ol>

          <p className="mx-5 mt-5 rounded-xl bg-muted/60 px-4 py-3 text-xs leading-5 text-muted-foreground">
            {guide.note}
          </p>

          <DrawerFooter className="px-5 pb-5 pt-4">
            <DrawerClose asChild>
              <button
                type="button"
                className="h-12 rounded-lg bg-[rgb(40,184,148)] px-5 text-sm font-bold text-black transition-colors hover:bg-[rgb(54,204,166)] focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-[rgb(40,184,148)]"
              >
                知道了
              </button>
            </DrawerClose>
          </DrawerFooter>
        </div>
      </DrawerContent>
    </Drawer>
  );
}

function detectInstallPlatform(): InstallPlatform {
  if (typeof navigator === "undefined") return "desktop";
  if (/iPad|iPhone|iPod/.test(navigator.userAgent)) return "ios";
  if (/Android/i.test(navigator.userAgent)) return "android";
  return "desktop";
}

export function InstallButton({ className }: InstallButtonProps) {
  const installStatus = useSyncExternalStore(
    subscribePwaInstallStatus,
    getPwaInstallStatus,
    getPwaInstallServerStatus,
  );
  const [platform, setPlatform] = useState<InstallPlatform>("desktop");
  const [guideOpen, setGuideOpen] = useState(false);
  const [isPrompting, setIsPrompting] = useState(false);

  useEffect(() => {
    setPlatform(detectInstallPlatform());
  }, []);

  const handleClick = useCallback(async () => {
    // 触发 APK 下载（不再走 PWA 安装流程）
    // 优先使用环境变量配置的下载地址，兜底使用相对路径
    const apkUrl = process.env.NEXT_PUBLIC_APK_DOWNLOAD_URL || 'https://br-epic-clam-5a2fd709.supabase2.aidap-global.cn-beijing.volces.com/storage/v1/object/public/apk/mengzhen-latest.apk';
    
    if (installStatus.mode === 'installed') {
      return;
    }

    setIsPrompting(true);
    
    try {
      const a = document.createElement('a');
      a.href = apkUrl;
      a.download = 'mengzhen.apk';
      a.rel = 'noopener';
      // 如果是跨域 URL，在新标签打开；同域直接下载
      try {
        const url = new URL(apkUrl, window.location.origin);
        if (url.origin !== window.location.origin) {
          a.target = '_blank';
        }
      } catch {
        // 相对路径，无需处理
      }
      document.body.appendChild(a);
      a.click();
      document.body.removeChild(a);
    } catch (e) {
      // 兜底：直接跳转
      window.location.href = apkUrl;
    }
    
    setIsPrompting(false);
  }, [installStatus.mode]);

  if (installStatus.mode === "installed") {
    return (
      <div
        className={cn(
          "inline-flex h-10 items-center gap-2 rounded bg-white/10 px-4 text-sm text-white/80",
          className,
        )}
        role="status"
      >
        <span className="size-2 rounded-full bg-[rgb(40,184,148)]" aria-hidden="true" />
        <span>已安装</span>
      </div>
    );
  }

  const isChecking = installStatus.mode === "checking";
  const isDisabled = isChecking || isPrompting;
  const buttonText = isPrompting
    ? "正在下载"
    : isChecking
      ? "正在检测"
      : "安装梦枕";

  return (
    <>
      <div className={cn("install-button-root inline-block relative", className)}>
        <button
          type="button"
          onClick={handleClick}
          disabled={isDisabled}
          aria-busy={isChecking || isPrompting}
          title={undefined}
          className={cn(
            "special-button arrow-hover-container color-#000 p-1px primary medium",
            isDisabled && "disabled",
          )}
        >
          <div className="prefix-container">
            <Image
              src="/logo.png"
              alt=""
              width={48}
              height={48}
              className="rounded object-contain"
            />
          </div>
          <div className="text-lg ShuHeiTi button-content font-700">
            <span className="button-text" aria-live="polite">{buttonText}</span>
          </div>
          <div className="arrow-hover medium default" aria-hidden="true">
            <div className="arrow-icon">
              <div className="arrow-icon-white">
                <svg xmlns="http://www.w3.org/2000/svg" width="30" height="30" viewBox="0 0 30 30" fill="currentColor">
                  <path d="M3.7546 29.5444L0.632812 26.4936L23.4077 3.29306L24.188 4.71205H4.74788V0.455078H26.4584L29.3674 3.36401V25.0746H25.1105V5.63439L26.5294 6.41485L3.7546 29.5444Z" fill="white" />
                </svg>
              </div>
              <div className="arrow-icon-black">
                <svg xmlns="http://www.w3.org/2000/svg" width="30" height="30" viewBox="0 0 30 30" fill="currentColor">
                  <path d="M3.7546 29.5444L0.632812 26.4936L23.4077 3.29306L24.188 4.71205H4.74788V0.455078H26.4584L29.3674 3.36401V25.0746H25.1105V5.63439L26.5294 6.41485L3.7546 29.5444Z" fill="white" />
                </svg>
              </div>
            </div>
          </div>
        </button>
      </div>

      <InstallGuide open={guideOpen} onOpenChange={setGuideOpen} platform={platform} />
    </>
  );
}

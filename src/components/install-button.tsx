"use client";

import { useEffect, useState, useCallback } from "react";
import Image from "next/image";
import { cn } from "@/lib/utils";
import { initPwaInstallListener, promptInstall, getPwaStatus } from "@/lib/pwa";
import { toast } from "@/components/sonner";
import "./install-button.css";

interface InstallButtonProps {
  className?: string;
}

export function InstallButton({ className }: InstallButtonProps) {
  const [canInstall, setCanInstall] = useState(false);
  const [isInstalled, setIsInstalled] = useState(false);

  useEffect(() => {
    getPwaStatus().then((status) => {
      setIsInstalled(status.isInstalled);
      setCanInstall(status.canInstall && !status.isInstalled && !status.hasPrompted);
    });

    const cleanup = initPwaInstallListener(() => {
      getPwaStatus().then((status) => {
        setCanInstall(status.canInstall && !status.isInstalled && !status.hasPrompted);
      });
    });

    return cleanup;
  }, []);

  const handleClick = useCallback(async () => {
    const result = await promptInstall();
    if (result === "accepted") {
      setCanInstall(false);
      setIsInstalled(true);
      return;
    }

    if (result === "dismissed") {
      setCanInstall(false);
      return;
    }

    const isIos = /iPad|iPhone|iPod/.test(navigator.userAgent);
    toast.message(
      isIos
        ? "请在 Safari 的分享菜单中选择“添加到主屏幕”"
        : "请在浏览器菜单中选择“安装应用”或“添加到主屏幕”"
    );
  }, []);

  if (isInstalled) {
    return (
      <div className={cn(
        "inline-flex items-center gap-2 rounded border border-white/20 bg-white/5 px-4 py-2 text-sm text-white/70 backdrop-blur-sm",
        className
      )}>
        <span>已安装</span>
      </div>
    );
  }

  if (!canInstall) {
    return null;
  }

  return (
    <div className={cn("install-button-root inline-block relative", className)}>
      <button
        type="button"
        onClick={handleClick}
        className="special-button arrow-hover-container color-#000 p-1px primary medium"
      >
          <div className="prefix-container">
            <Image
              src="/logo.png"
              alt="梦枕"
              width={48}
              height={48}
              className="rounded object-contain"
            />
          </div>
          <div className="text-lg ShuHeiTi button-content font-700">
            <span className="button-text">安装梦枕</span>
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
  );
}

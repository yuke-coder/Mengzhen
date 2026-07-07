"use client";

import React, { useEffect, useState } from "react";
import { cn } from "@/lib/utils";
import { useIsMobile } from "@/hooks/use-mobile";
import { Drawer, DrawerContent, DrawerTitle } from "@/components/ui/drawer";
import { X } from "lucide-react";

interface TaskModalProps {
  visible: boolean;
  onClose: () => void;
  children: React.ReactNode;
}

const MOBILE_SNAP_POINTS = [0.72, 1];

export function TaskModal({ visible, onClose, children }: TaskModalProps) {
  const isMobile = useIsMobile();
  const [mounted, setMounted] = useState(false);
  const [animating, setAnimating] = useState(false);
  const [snap, setSnap] = useState<number | string | null>(0.72);

  useEffect(() => {
    if (visible) {
      setMounted(true);
      setSnap(0.72);
      document.body.style.overflow = "hidden";
      requestAnimationFrame(() => {
        requestAnimationFrame(() => {
          setAnimating(true);
        });
      });
    } else {
      setAnimating(false);
      const timer = setTimeout(() => {
        setMounted(false);
        document.body.style.overflow = "";
      }, 600);
      return () => clearTimeout(timer);
    }
  }, [visible]);

  useEffect(() => {
    if (!mounted) return;
    const handleEsc = (e: KeyboardEvent) => {
      if (e.key === "Escape") onClose();
    };
    window.addEventListener("keydown", handleEsc);
    return () => window.removeEventListener("keydown", handleEsc);
  }, [mounted, onClose]);

  if (!mounted) return null;

  if (isMobile) {
    const isFullScreen = snap === 1;

    return (
      <Drawer
        open={visible}
        onOpenChange={(open) => { if (!open) onClose(); }}
        direction="bottom"
        snapPoints={MOBILE_SNAP_POINTS}
        activeSnapPoint={snap}
        setActiveSnapPoint={setSnap}
        snapToSequentialPoint
      >
        <DrawerContent
          data-fullscreen={isFullScreen}
          className={cn(
            "h-[100dvh] max-h-none border-t border-border/60 bg-background/70 backdrop-blur-xl transition-[border-radius,box-shadow,background-color] duration-200 ease-[cubic-bezier(0.22,1,0.36,1)] dark:bg-background/35",
            isFullScreen
              ? "rounded-none bg-background/80 shadow-none dark:bg-background/45"
              : "rounded-t-2xl shadow-[0_-18px_48px_rgba(0,0,0,0.14)] dark:shadow-[0_-18px_54px_rgba(0,0,0,0.35)]"
          )}
        >
          <DrawerTitle className="sr-only">新建任务</DrawerTitle>
          <button
            type="button"
            onClick={onClose}
            className="absolute right-3 top-3 z-[101] flex min-h-[40px] min-w-[40px] items-center justify-center rounded-full bg-white/80 p-2.5 text-foreground backdrop-blur-md dark:bg-black/50 dark:text-white/80"
          >
            <X className="w-5 h-5" />
          </button>
          <div
            className={cn(
              "flex-1 overflow-y-auto overscroll-contain px-4 pb-6 pt-4 -webkit-overflow-scrolling-touch",
              isFullScreen && "mobile-task-sheet-fullscreen"
            )}
          >
            {children}
          </div>
        </DrawerContent>
      </Drawer>
    );
  }

  return (
    <div
      className={cn(
        "fixed inset-0 z-[100] flex items-end sm:items-center justify-center transition-all duration-[600ms] ease-[cubic-bezier(0.22,1,0.36,1)]",
        animating
          ? "bg-black/0 sm:bg-black/20 dark:sm:bg-black/40"
          : "bg-black/0"
      )}
      onClick={(e) => {
        if (e.target === e.currentTarget) onClose();
      }}
    >
      <div
        className={cn(
          "relative w-full sm:max-w-3xl flex flex-col overflow-hidden transition-all duration-[600ms] ease-[cubic-bezier(0.22,1,0.36,1)]",
          "sm:h-auto sm:max-h-[82vh] sm:bg-background/85 dark:sm:bg-background/60 sm:backdrop-blur-xl sm:border sm:border-border/60",
          "sm:shadow-[0_8px_60px_rgba(0,0,0,0.12),0_2px_20px_rgba(0,212,170,0.06)] dark:sm:shadow-[0_8px_60px_rgba(0,0,0,0.3),0_2px_20px_rgba(0,212,170,0.08)] sm:rounded-2xl",
          animating
            ? "opacity-100"
            : "opacity-0 sm:scale-[0.96] sm:translate-y-4"
        )}
        onClick={(e) => e.stopPropagation()}
      >
        <div
          className={cn(
            "absolute top-0 left-0 right-0 h-1 bg-gradient-to-r from-[var(--brand-start)] via-[var(--brand-mid)] to-[var(--brand-end)] transition-opacity duration-[600ms]",
            "hidden sm:block",
            animating ? "opacity-100" : "opacity-0"
          )}
        />

        <button
          type="button"
          onClick={onClose}
          className={cn(
            "fixed z-[101] top-5 right-5 p-3 rounded-full transition-all duration-[600ms] cursor-pointer",
            "backdrop-blur-md bg-white/80 text-foreground dark:bg-black/50",
            "dark:text-white/80 dark:hover:text-white dark:active:text-white",
            "hover:shadow-lg hover:shadow-black/10 dark:hover:shadow-black/30 active:shadow-md",
            "hover:bg-white/90 active:bg-white/95 dark:hover:bg-black/60 dark:active:bg-black/70",
            "flex items-center justify-center",
            animating ? "opacity-100 scale-100" : "opacity-0 scale-75"
          )}
          style={{ transitionDelay: animating ? "200ms" : "0ms" }}
        >
          <X className="w-5 h-5" />
        </button>

        <div className="flex-1 overflow-y-auto overscroll-contain sm:p-6 md:p-8 -webkit-overflow-scrolling-touch">
          <div
            className={cn(
              "transition-all duration-[600ms] ease-[cubic-bezier(0.22,1,0.36,1)]",
              animating
                ? "opacity-100 translate-y-0"
                : "opacity-0 translate-y-6"
            )}
            style={{ transitionDelay: animating ? "150ms" : "0ms" }}
          >
            {children}
          </div>
        </div>

        <div
          className={cn(
            "absolute bottom-0 left-0 right-0 h-px bg-gradient-to-r from-transparent via-[var(--brand-glow)]/30 to-transparent transition-opacity duration-[600ms]",
            "hidden sm:block",
            animating ? "opacity-100" : "opacity-0"
          )}
        />
      </div>
    </div>
  );
}

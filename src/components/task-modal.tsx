"use client";

import React, { useEffect, useState } from "react";
import { cn } from "@/lib/utils";
import { X } from "lucide-react";

interface TaskModalProps {
  visible: boolean;
  onClose: () => void;
  children: React.ReactNode;
}

export function TaskModal({ visible, onClose, children }: TaskModalProps) {
  const [mounted, setMounted] = useState(false);
  const [animating, setAnimating] = useState(false);

  useEffect(() => {
    if (visible) {
      setMounted(true);
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

  return (
    <div
      className={cn(
        "fixed inset-0 z-[100] flex items-end sm:items-center justify-center sm:justify-center transition-all duration-[600ms] ease-[cubic-bezier(0.22,1,0.36,1)]",
        animating
          ? "bg-transparent backdrop-blur-0 sm:bg-black/20 dark:sm:bg-black/40 sm:backdrop-blur-md"
          : "bg-black/0 backdrop-blur-0"
      )}
      onClick={(e) => {
        if (e.target === e.currentTarget) onClose();
      }}
    >
      <div
        className={cn(
          "relative w-full sm:max-w-3xl flex flex-col overflow-hidden transition-all duration-[600ms] ease-[cubic-bezier(0.22,1,0.36,1)]",
          "h-[100dvh] bg-transparent",
          "sm:h-auto sm:max-h-[82vh] sm:bg-background dark:sm:bg-background/95 sm:backdrop-blur-xl sm:border sm:border-border/60",
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

        <div className="flex-1 overflow-y-auto overscroll-contain px-4 pt-14 pb-6 sm:p-6 md:p-8 -webkit-overflow-scrolling-touch">
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

      <button
        onClick={onClose}
        className={cn(
          "fixed z-[101] p-2.5 sm:p-3 rounded-full transition-all duration-[600ms] cursor-pointer",
          "backdrop-blur-md",
          "bg-white/80 border-border/40 hover:border-border/60 active:border-border",
          "text-foreground hover:text-foreground active:text-foreground",
          "dark:bg-black/50 dark:border-white/20 dark:hover:border-white/40 dark:active:border-white/50",
          "dark:text-white/80 dark:hover:text-white dark:active:text-white",
          "hover:shadow-lg hover:shadow-black/10 dark:hover:shadow-black/30 active:shadow-md",
          "hover:bg-white/90 active:bg-white/95 dark:hover:bg-black/60 dark:active:bg-black/70",
          "min-w-[40px] min-h-[40px] sm:min-w-0 sm:min-h-0 flex items-center justify-center",
          "top-3 right-3 sm:top-5 sm:right-5",
          animating
            ? "opacity-100 scale-100"
            : "opacity-0 scale-75"
        )}
        style={{ transitionDelay: animating ? "200ms" : "0ms" }}
      >
        <X className="w-5 h-5" />
      </button>
    </div>
  );
}

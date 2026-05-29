"use client";

import React, { useEffect, useState, useCallback, Component } from "react";
import { cn } from "@/lib/utils";
import { X, AlertCircle } from "lucide-react";

interface TaskModalProps {
  visible: boolean;
  onClose: () => void;
  children: React.ReactNode;
}

interface ErrorBoundaryProps {
  children: React.ReactNode;
  onError: () => void;
}

interface ErrorBoundaryState {
  hasError: boolean;
  error: Error | null;
}

class ModalErrorBoundary extends Component<ErrorBoundaryProps, ErrorBoundaryState> {
  constructor(props: ErrorBoundaryProps) {
    super(props);
    this.state = { hasError: false, error: null };
  }

  static getDerivedStateFromError(error: Error) {
    return { hasError: true, error };
  }

  componentDidCatch(error: Error, errorInfo: React.ErrorInfo) {
    console.error("[TaskModal] Child component error:", error, errorInfo);
    this.props.onError();
  }

  render() {
    if (this.state.hasError) {
      return (
        <div className="flex flex-col items-center justify-center p-8 gap-4 text-center">
          <AlertCircle className="w-10 h-10 text-destructive" />
          <div>
            <p className="text-sm font-medium text-foreground">内容加载出错</p>
            <p className="text-xs text-muted-foreground mt-1">{this.state.error?.message}</p>
          </div>
          <button
            onClick={() => this.setState({ hasError: false, error: null })}
            className="text-xs text-[var(--brand-start)] hover:underline"
          >
            重试
          </button>
        </div>
      );
    }
    return this.props.children;
  }
}

export function TaskModal({ visible, onClose, children }: TaskModalProps) {
  const [mounted, setMounted] = useState(false);
  const [animating, setAnimating] = useState(false);

  const handleError = useCallback(() => {
    // 子组件崩溃时确保清理 body overflow，防止页面被锁死
    document.body.style.overflow = "";
  }, []);

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

  const handleOverlayClick = useCallback(
    (e: React.MouseEvent) => {
      if (e.target === e.currentTarget) onClose();
    },
    [onClose]
  );

  if (!mounted) return null;

  return (
    <div
      className={cn(
        "fixed inset-0 z-[100] flex items-end sm:items-center justify-center sm:justify-center transition-all duration-[600ms] ease-[cubic-bezier(0.22,1,0.36,1)]",
        animating
          ? "bg-black/20 dark:bg-black/40 backdrop-blur-md"
          : "bg-black/0 backdrop-blur-0"
      )}
      onClick={handleOverlayClick}
    >
      <div
        className={cn(
          "relative w-full sm:max-w-3xl flex flex-col overflow-hidden transition-all duration-[600ms] ease-[cubic-bezier(0.22,1,0.36,1)]",
          "bg-background dark:bg-background/95 backdrop-blur-xl border border-border/60",
          "shadow-[0_8px_60px_rgba(0,0,0,0.12),0_2px_20px_rgba(0,212,170,0.06)] dark:shadow-[0_8px_60px_rgba(0,0,0,0.3),0_2px_20px_rgba(0,212,170,0.08)]",
          "sm:rounded-2xl rounded-t-2xl sm:rounded-b-2xl",
          "max-h-[92vh] sm:max-h-[82vh]",
          animating
            ? "opacity-100 scale-100 translate-y-0"
            : "opacity-0 scale-[0.96] translate-y-4"
        )}
        onClick={(e) => e.stopPropagation()}
      >
        <div
          className={cn(
            "absolute top-0 left-0 right-0 h-1 bg-gradient-to-r from-[var(--brand-start)] via-[var(--brand-mid)] to-[var(--brand-end)] transition-opacity duration-[600ms]",
            animating ? "opacity-100" : "opacity-0"
          )}
        />

        <div className="flex-1 overflow-y-auto overscroll-contain p-4 sm:p-6 md:p-8 -webkit-overflow-scrolling-touch">
          <div
            className={cn(
              "transition-all duration-[600ms] ease-[cubic-bezier(0.22,1,0.36,1)]",
              animating
                ? "opacity-100 translate-y-0"
                : "opacity-0 translate-y-6"
            )}
            style={{ transitionDelay: animating ? "150ms" : "0ms" }}
          >
            <ModalErrorBoundary onError={handleError}>
              {children}
            </ModalErrorBoundary>
          </div>
        </div>

        <div
          className={cn(
            "absolute bottom-0 left-0 right-0 h-px bg-gradient-to-r from-transparent via-[var(--brand-glow)]/30 to-transparent transition-opacity duration-[600ms]",
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
